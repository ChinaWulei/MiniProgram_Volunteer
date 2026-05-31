package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.SubscribeSettingsDTO;
import com.scs.volunteer.service.ActivitySubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/activity-subscriptions")
public class ActivitySubscriptionController extends BaseController {
    private final ActivitySubscriptionService activitySubscriptionService;

    public ActivitySubscriptionController(ActivitySubscriptionService activitySubscriptionService) {
        this.activitySubscriptionService = activitySubscriptionService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> settings(HttpServletRequest request) {
        return ApiResponse.ok(activitySubscriptionService.settings(currentUser(request).getId()));
    }

    @PutMapping
    public ApiResponse<Void> save(@RequestBody SubscribeSettingsDTO dto, HttpServletRequest request) {
        activitySubscriptionService.save(currentUser(request).getId(), dto);
        return ApiResponse.ok(null);
    }
}
