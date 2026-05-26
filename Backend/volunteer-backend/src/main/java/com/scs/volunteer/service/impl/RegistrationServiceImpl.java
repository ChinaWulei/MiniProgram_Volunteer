package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.RegistrationDTO;
import com.scs.volunteer.dto.ReviewDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.CreditMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import com.scs.volunteer.mapper.ServiceRecordMapper;
import com.scs.volunteer.mapper.VolunteerMapper;
import com.scs.volunteer.service.RegistrationService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;
    private final VolunteerMapper volunteerMapper;
    private final ServiceRecordMapper serviceRecordMapper;
    private final NotificationMapper notificationMapper;
    private final CreditMapper creditMapper;

    public RegistrationServiceImpl(RegistrationMapper registrationMapper, ActivityMapper activityMapper, VolunteerMapper volunteerMapper, ServiceRecordMapper serviceRecordMapper, NotificationMapper notificationMapper, CreditMapper creditMapper) {
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
        this.volunteerMapper = volunteerMapper;
        this.serviceRecordMapper = serviceRecordMapper;
        this.notificationMapper = notificationMapper;
        this.creditMapper = creditMapper;
    }

    @Override
    public void register(RegistrationDTO dto, CurrentUser currentUser) {
        if (currentUser == null || !"VOLUNTEER".equals(currentUser.getRole())) {
            throw new BizException("仅志愿者可报名");
        }
        Activity activity = activityMapper.findById(dto.getActivityId()).orElseThrow(() -> new BizException("活动不存在"));
        if (!"报名中".equals(activity.getStatus()) && !"已发布".equals(activity.getStatus())) {
            throw new BizException("当前活动不可报名");
        }
        if (activity.getRegisteredNumber() >= activity.getRecruitNumber()) {
            throw new BizException("活动已满员");
        }
        if (registrationMapper.exists(dto.getActivityId(), currentUser.getId())) {
            throw new BizException("不能重复报名");
        }
        if (creditMapper.score(currentUser.getId()) < 70) {
            throw new BizException("信用分低于70分，暂不能报名，请联系管理员处理");
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (activity.getSignupStartTime() != null && now.isBefore(activity.getSignupStartTime())) {
            throw new BizException("报名尚未开始");
        }
        if (activity.getSignupDeadline() != null && now.isAfter(activity.getSignupDeadline())) {
            throw new BizException("报名已截止");
        }
        if (registrationMapper.hasTimeConflict(currentUser.getId(), activity.getId(), activity.getStartTime(), activity.getEndTime())) {
            throw new BizException("该时间段已报名其他活动，请确认时间安排");
        }
        String signupStatus = "自动通过".equals(activity.getReviewMethod()) ? "已通过" : "待审核";
        registrationMapper.insert(dto.getActivityId(), currentUser.getId(), signupStatus);
        activityMapper.increaseRegistered(dto.getActivityId());
    }

    @Override
    public List<Map<String, Object>> my(CurrentUser currentUser) {
        return registrationMapper.my(currentUser.getId());
    }

    @Override
    public List<Map<String, Object>> adminList(String keyword, String status, Long activityId, CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可查看报名列表");
        }
        return registrationMapper.adminList(keyword, status, activityId);
    }

    @Override
    public void review(Long id, ReviewDTO dto, CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可审核");
        }
        Map<String, Object> reg = registrationMapper.findMap(id);
        registrationMapper.review(id, dto.getStatus(), dto.getReviewRemark());
        if ("已完成".equals(dto.getStatus())) {
            Long userId = ((Number) reg.get("user_id")).longValue();
            Long activityId = ((Number) reg.get("activity_id")).longValue();
            Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
            double hours = activity.getServiceHours() == null
                    ? Math.max(1.0, Duration.between(activity.getStartTime(), activity.getEndTime()).toMinutes() / 60.0)
                    : activity.getServiceHours();
            volunteerMapper.addService(userId, hours);
            serviceRecordMapper.insert(userId, activityId, hours, "管理员确认完成");
        }
    }

    @Override
    public void cancel(Long id, ReviewDTO dto, CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new BizException("仅管理员可取消报名");
        }
        String reason = dto == null ? "" : (dto.getReason() == null ? dto.getReviewRemark() : dto.getReason());
        if (reason == null || reason.isBlank()) throw new BizException("请填写取消原因");
        Map<String, Object> reg = registrationMapper.findMap(id);
        Long userId = ((Number) reg.get("user_id")).longValue();
        Long activityId = ((Number) reg.get("activity_id")).longValue();
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        registrationMapper.delete(id);
        activityMapper.decreaseRegistered(activityId);
        notificationMapper.insert(userId, "REGISTRATION_CANCELLED", "报名已取消",
                "你报名的《" + activity.getName() + "》已由管理员取消。原因：" + reason,
                "ACTIVITY", activityId);
    }
}
