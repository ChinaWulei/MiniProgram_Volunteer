package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.CheckinRequest;
import com.scs.volunteer.dto.ManualCheckinRequest;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.CheckinMapper;
import com.scs.volunteer.service.CheckinService;
import com.scs.volunteer.vo.CheckinStatusVO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class CheckinServiceImpl implements CheckinService {
    private static final double MAX_DISTANCE_METERS = 500D;

    private final CheckinMapper checkinMapper;
    private final ActivityMapper activityMapper;

    public CheckinServiceImpl(CheckinMapper checkinMapper, ActivityMapper activityMapper) {
        this.checkinMapper = checkinMapper;
        this.activityMapper = activityMapper;
    }

    @Override
    public CheckinStatusVO checkin(CheckinRequest request, CurrentUser currentUser) {
        if (currentUser == null || !"VOLUNTEER".equals(currentUser.getRole())) throw new BizException("仅志愿者可签到");
        if (request == null || request.getActivityId() == null) throw new BizException("活动不能为空");
        if (request.getLatitude() == null || request.getLongitude() == null) throw new BizException("定位信息不能为空");
        Activity activity = activityMapper.findById(request.getActivityId()).orElseThrow(() -> new BizException("活动不存在"));
        if (!checkinMapper.approved(activity.getId(), currentUser.getId())) throw new BizException("仅审核通过的志愿者可签到");
        if (checkinMapper.find(activity.getId(), currentUser.getId()).isPresent()) throw new BizException("不能重复签到");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime checkinStart = activity.getCheckinStartTime() == null ? activity.getStartTime() : activity.getCheckinStartTime();
        LocalDateTime checkinEnd = activity.getCheckinEndTime() == null ? activity.getEndTime() : activity.getCheckinEndTime();
        if (now.isBefore(checkinStart)) throw new BizException("签到尚未开始");
        if (now.isAfter(checkinEnd)) throw new BizException("签到已结束，请联系管理员补签");
        if (activity.getLatitude() == null || activity.getLongitude() == null) throw new BizException("活动未配置签到坐标");
        double distance = distanceMeters(activity.getLatitude(), activity.getLongitude(), request.getLatitude(), request.getLongitude());
        if (distance > MAX_DISTANCE_METERS) throw new BizException("未到达活动地点，无法签到");
        String status = now.isAfter(checkinStart.plusMinutes(30)) ? "LATE_CHECKED_IN" : "CHECKED_IN";
        checkinMapper.insertLocation(activity.getId(), currentUser.getId(), status, now, request.getLatitude(), request.getLongitude(), distance);
        return status(activity.getId(), currentUser);
    }

    @Override
    public CheckinStatusVO status(Long activityId, CurrentUser currentUser) {
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        CheckinStatusVO vo = checkinMapper.find(activityId, currentUser.getId()).orElseGet(CheckinStatusVO::new);
        if (vo.getStatus() == null) {
            vo.setStatus(LocalDateTime.now().isAfter(activity.getEndTime()) ? "ABSENT" : "NOT_CHECKED_IN");
        }
        vo.setStatusText(statusText(vo.getStatus()));
        return vo;
    }

    @Override
    public Map<String, Object> activityStatistics(Long activityId, CurrentUser currentUser) {
        requireAdmin(currentUser);
        Map<String, Object> data = checkinMapper.statistics(activityId);
        double approved = number(data.get("approvedCount"));
        double success = number(data.get("checkedCount")) + number(data.get("manualCount"));
        data.put("successRate", approved == 0 ? 0 : Math.round(success * 1000 / approved) / 10.0);
        return data;
    }

    @Override
    public List<Map<String, Object>> activityList(Long activityId, String status, String keyword, CurrentUser currentUser) {
        requireAdmin(currentUser);
        return checkinMapper.list(activityId, status, keyword);
    }

    @Override
    public void manual(Long activityId, ManualCheckinRequest request, CurrentUser currentUser) {
        requireAdmin(currentUser);
        if (request == null || request.getUserId() == null) throw new BizException("请选择志愿者");
        if (request.getReason() == null || request.getReason().isBlank()) throw new BizException("请填写补签原因");
        activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        if (!checkinMapper.approved(activityId, request.getUserId())) throw new BizException("仅能为审核通过的志愿者补签");
        if (checkinMapper.find(activityId, request.getUserId()).isPresent()) throw new BizException("该志愿者已有签到记录");
        checkinMapper.insertManual(activityId, request.getUserId(), currentUser.getId(), request.getReason().trim());
    }

    @Override
    public Map<String, Object> volunteerStatistics(Long userId, CurrentUser currentUser) {
        requireAdmin(currentUser);
        Map<String, Object> data = checkinMapper.volunteerStatistics(userId);
        double expected = number(data.get("expectedTotal"));
        double success = number(data.get("normalCount")) + number(data.get("manualCount"));
        data.put("successRate", expected == 0 ? 0 : Math.round(success * 1000 / expected) / 10.0);
        data.put("recentRecords", checkinMapper.recentByVolunteer(userId));
        return data;
    }

    private double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double earthRadius = 6371000D;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String statusText(String status) {
        return switch (status) {
            case "CHECKED_IN" -> "已签到";
            case "LATE_CHECKED_IN" -> "迟到签到";
            case "MANUAL_CHECKED_IN" -> "管理员补签";
            case "ABSENT" -> "缺勤";
            default -> "未签到";
        };
    }

    private void requireAdmin(CurrentUser currentUser) {
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) throw new BizException("仅管理员可操作");
    }

    private double number(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0D;
    }
}
