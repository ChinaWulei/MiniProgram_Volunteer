package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.CheckinAdjustmentApplyDTO;
import com.scs.volunteer.dto.CheckinAdjustmentAuditDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.CheckinAdjustmentMapper;
import com.scs.volunteer.mapper.CheckinMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.mapper.ServiceRecordMapper;
import com.scs.volunteer.mapper.VolunteerMapper;
import com.scs.volunteer.service.CheckinAdjustmentService;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.vo.CheckinStatusVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class CheckinAdjustmentServiceImpl implements CheckinAdjustmentService {
    private static final DateTimeFormatter FORM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CheckinAdjustmentMapper adjustmentMapper;
    private final CheckinMapper checkinMapper;
    private final ActivityMapper activityMapper;
    private final S3StorageService s3StorageService;
    private final ServiceRecordMapper serviceRecordMapper;
    private final VolunteerMapper volunteerMapper;
    private final RegistrationMapper registrationMapper;
    private final int applyLimitHours;

    public CheckinAdjustmentServiceImpl(CheckinAdjustmentMapper adjustmentMapper, CheckinMapper checkinMapper,
                                        ActivityMapper activityMapper, S3StorageService s3StorageService,
                                        ServiceRecordMapper serviceRecordMapper, VolunteerMapper volunteerMapper,
                                        RegistrationMapper registrationMapper,
                                        @Value("${app.checkin.adjustment-apply-limit-hours:24}") int applyLimitHours) {
        this.adjustmentMapper = adjustmentMapper;
        this.checkinMapper = checkinMapper;
        this.activityMapper = activityMapper;
        this.s3StorageService = s3StorageService;
        this.serviceRecordMapper = serviceRecordMapper;
        this.volunteerMapper = volunteerMapper;
        this.registrationMapper = registrationMapper;
        this.applyLimitHours = applyLimitHours;
    }

    @Override
    public Long apply(CheckinAdjustmentApplyDTO dto, CurrentUser currentUser) {
        if (currentUser == null || !"VOLUNTEER".equals(currentUser.getRole())) throw new BizException("仅志愿者可申请补签");
        if (dto == null || dto.getActivityId() == null) throw new BizException("活动不能为空");
        if (blank(dto.getReason())) throw new BizException("请填写补签原因");
        Activity activity = activityMapper.findById(dto.getActivityId()).orElseThrow(() -> new BizException("活动不存在"));
        if (!checkinMapper.approved(activity.getId(), currentUser.getId())) throw new BizException("仅审核通过的活动可申请补签");
        if (activity.getEndTime() == null || LocalDateTime.now().isAfter(activity.getEndTime().plusHours(applyLimitHours))) {
            throw new BizException("活动结束超过" + applyLimitHours + "小时，不能申请补签");
        }
        if (adjustmentMapper.hasOpenApplication(activity.getId(), currentUser.getId())) throw new BizException("已有待审核补签申请");
        CheckinStatusVO original = checkinMapper.find(activity.getId(), currentUser.getId()).orElse(null);
        String originalStatus = original == null ? "ABSENT" : original.getStatus();
        LocalDateTime originalTime = original == null ? null : original.getCheckinTime();
        return adjustmentMapper.insertApplication(activity.getId(), currentUser.getId(), originalStatus, originalTime,
                dto.getReason().trim(), dto.getDescription(), dto.getProofImageUrl(), activity.getServiceHours());
    }

    @Override
    public String uploadProof(MultipartFile file, CurrentUser currentUser) {
        if (currentUser == null || !"VOLUNTEER".equals(currentUser.getRole())) throw new BizException("仅志愿者可上传证明");
        return s3StorageService.uploadActivityNewsImage(file);
    }

    @Override
    public List<Map<String, Object>> my(CurrentUser currentUser) {
        if (currentUser == null || !"VOLUNTEER".equals(currentUser.getRole())) throw new BizException("仅志愿者可查看补签记录");
        return adjustmentMapper.my(currentUser.getId());
    }

    @Override
    public List<Map<String, Object>> adminList(String auditStatus, Long activityId, String keyword, CurrentUser currentUser) {
        requireAdmin(currentUser);
        adjustmentMapper.createMissingAnomalyRows();
        return adjustmentMapper.adminList(auditStatus, activityId, keyword);
    }

    @Override
    public void audit(Long id, CheckinAdjustmentAuditDTO dto, CurrentUser currentUser) {
        requireAdmin(currentUser);
        Map<String, Object> row = adjustmentMapper.find(id);
        CheckinAdjustmentAuditDTO body = dto == null ? new CheckinAdjustmentAuditDTO() : dto;
        String status = normalizeAuditStatus(body.getAuditStatus());
        String newStatus = "APPROVED".equals(status) ? normalizeCheckinStatus(first(body.getNewStatus(), "MANUAL_CHECKED_IN")) : null;
        LocalDateTime newTime = "APPROVED".equals(status) ? parseTime(body.getNewCheckinTime()) : null;
        adjustmentMapper.audit(id, status, newStatus, newTime, body.getNewServiceHours(), body.getHoursReason(),
                body.getAdminRemark(), currentUser.getId());
        if ("APPROVED".equals(status)) {
            applyServiceHours(row, body.getNewServiceHours(), body.getHoursReason(), newStatus);
        }
    }

    @Override
    public void updateStatus(Long id, CheckinAdjustmentAuditDTO dto, CurrentUser currentUser) {
        requireAdmin(currentUser);
        Map<String, Object> row = adjustmentMapper.find(id);
        CheckinAdjustmentAuditDTO body = dto == null ? new CheckinAdjustmentAuditDTO() : dto;
        String newStatus = normalizeCheckinStatus(body.getNewStatus());
        LocalDateTime newTime = parseTime(body.getNewCheckinTime());
        adjustmentMapper.audit(id, "APPROVED", newStatus, newTime, body.getNewServiceHours(), body.getHoursReason(),
                body.getAdminRemark(), currentUser.getId());
        applyServiceHours(row, body.getNewServiceHours(), body.getHoursReason(), newStatus);
    }

    private void applyServiceHours(Map<String, Object> row, Double newServiceHours, String hoursReason, String newStatus) {
        if (newServiceHours == null || "ABSENT".equals(newStatus)) return;
        if (newServiceHours < 0) throw new BizException("服务时长不能小于0");
        Long activityId = ((Number) row.get("activity_id")).longValue();
        Long userId = ((Number) row.get("user_id")).longValue();
        double oldHours = serviceRecordMapper.totalHours(userId, activityId);
        double delta = newServiceHours - oldHours;
        int countDelta = oldHours <= 0 && newServiceHours > 0 ? 1 : 0;
        serviceRecordMapper.replace(userId, activityId, newServiceHours,
                blank(hoursReason) ? "补签审核调整服务时长" : hoursReason);
        volunteerMapper.adjustService(userId, delta, countDelta);
        registrationMapper.markCompleted(activityId, userId, blank(hoursReason) ? "补签审核确认完成" : hoursReason);
    }

    private String normalizeAuditStatus(String value) {
        if ("REJECTED".equalsIgnoreCase(value) || "已驳回".equals(value)) return "REJECTED";
        return "APPROVED";
    }

    private String normalizeCheckinStatus(String value) {
        if ("CHECKED_IN".equals(value) || "正常签到".equals(value)) return "CHECKED_IN";
        if ("LATE_CHECKED_IN".equals(value) || "迟到".equals(value)) return "LATE_CHECKED_IN";
        if ("ABSENT".equals(value) || "缺勤".equals(value)) return "ABSENT";
        if ("MANUAL_CHECKED_IN".equals(value) || "补签".equals(value)) return "MANUAL_CHECKED_IN";
        throw new BizException("签到状态不正确");
    }

    private LocalDateTime parseTime(String value) {
        if (blank(value)) return LocalDateTime.now();
        String text = value.trim();
        if (text.contains("T")) {
            if (text.length() == 16) text += ":00";
            return LocalDateTime.parse(text);
        }
        return LocalDateTime.parse(text, FORM_FORMAT);
    }

    private String first(String value, String fallback) {
        return blank(value) ? fallback : value;
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private void requireAdmin(CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) throw new BizException("仅管理员可操作");
    }
}
