package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.ActivityDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.service.ActivityService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController extends BaseController {
    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    @GetMapping
    public ApiResponse<List<Activity>> list(String category, String status, String keyword) {
        return ApiResponse.ok(activityService.list(category, status, keyword));
    }

    @GetMapping("/{id}")
    public ApiResponse<Activity> detail(@PathVariable Long id) {
        return ApiResponse.ok(activityService.detail(id));
    }

    @PostMapping
    public ApiResponse<Map<String, Long>> create(@RequestBody ActivityDTO dto, HttpServletRequest request) {
        return ApiResponse.ok(Map.of("id", activityService.create(dto, currentUser(request))));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @RequestBody ActivityDTO dto, HttpServletRequest request) {
        activityService.update(id, dto, currentUser(request));
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        activityService.delete(id, currentUser(request));
        return ApiResponse.ok(null);
    }
}
