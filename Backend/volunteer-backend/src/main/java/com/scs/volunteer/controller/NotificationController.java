package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.mapper.NotificationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class NotificationController extends BaseController {
    private final NotificationMapper notificationMapper;

    public NotificationController(NotificationMapper notificationMapper) {
        this.notificationMapper = notificationMapper;
    }

    @GetMapping("/api/notifications")
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        return ApiResponse.ok(notificationMapper.list(currentUser(request).getId()));
    }

    @GetMapping("/api/notifications/{id}")
    public ApiResponse<Map<String, Object>> detail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = currentUser(request).getId();
        Map<String, Object> notification = notificationMapper.find(userId, id)
                .orElseThrow(() -> new BizException("通知不存在"));
        notificationMapper.markRead(userId, id);
        return ApiResponse.ok(notification);
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
}
