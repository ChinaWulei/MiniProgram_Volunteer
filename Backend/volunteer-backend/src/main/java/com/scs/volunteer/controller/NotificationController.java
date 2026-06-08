package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityParticipantNoticeDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class NotificationController extends BaseController {
    private final NotificationMapper notificationMapper;
    private final RegistrationMapper registrationMapper;
    private final ActivityMapper activityMapper;

    public NotificationController(NotificationMapper notificationMapper, RegistrationMapper registrationMapper, ActivityMapper activityMapper) {
        this.notificationMapper = notificationMapper;
        this.registrationMapper = registrationMapper;
        this.activityMapper = activityMapper;
    }

    @GetMapping("/api/notifications")
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        return ApiResponse.ok(notificationMapper.list(currentUser(request).getId()));
    }

    @GetMapping("/api/notifications/unread-count")
    public ApiResponse<Map<String, Integer>> unreadCount(HttpServletRequest request) {
        return ApiResponse.ok(Map.of("unreadCount", notificationMapper.unreadCount(currentUser(request).getId())));
    }

    @PostMapping("/api/notifications/{id}/read")
    public ApiResponse<Void> markRead(HttpServletRequest request, @PathVariable Long id) {
        notificationMapper.markRead(currentUser(request).getId(), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/activities/{activityId}/notifications")
    public ApiResponse<Map<String, Integer>> sendActivityNotice(HttpServletRequest request,
                                                                @PathVariable Long activityId,
                                                                @RequestBody ActivityParticipantNoticeDTO dto) {
        requireAdmin(currentUser(request));
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BizException("请填写通知标题");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BizException("请填写通知内容");
        }
        Activity activity = activityMapper.findById(activityId).orElseThrow(() -> new BizException("活动不存在"));
        List<Long> userIds = registrationMapper.participantUserIds(activityId, dto.getScope());
        String title = limit(dto.getTitle().trim(), 120);
        String content = dto.getContent().trim();
        String noticeContent = limit("《" + activity.getName() + "》：" + content, 500);
        for (Long userId : userIds) {
            notificationMapper.insert(userId, "ACTIVITY_NOTICE", title, noticeContent, "ACTIVITY", activityId);
        }
        return ApiResponse.ok(Map.of("sentCount", userIds.size()));
    }

    private void requireAdmin(CurrentUser user) {
        if (user == null || !"ADMIN".equals(user.getRole())) throw new BizException("仅管理员可发送活动通知");
    }

    private String limit(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }
}
