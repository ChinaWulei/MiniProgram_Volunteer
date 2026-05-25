package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.ActivityNewsDTO;
import com.scs.volunteer.service.ActivityNewsService;
import com.scs.volunteer.vo.ActivityNewsVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class ActivityNewsController extends BaseController {
    private final ActivityNewsService activityNewsService;

    public ActivityNewsController(ActivityNewsService activityNewsService) {
        this.activityNewsService = activityNewsService;
    }

    @PostMapping("/api/admin/activity-news/images")
    public ApiResponse<Map<String, List<String>>> uploadImages(HttpServletRequest request, @RequestParam("files") MultipartFile[] files) {
        return ApiResponse.ok(Map.of("urls", activityNewsService.uploadImages(files, currentUser(request))));
    }

    @PostMapping("/api/admin/activities/{activityId}/news/generate")
    public ApiResponse<Map<String, String>> generate(HttpServletRequest request, @PathVariable Long activityId) {
        return ApiResponse.ok(activityNewsService.generate(activityId, currentUser(request)));
    }

    @PostMapping("/api/admin/activity-news")
    public ApiResponse<Map<String, Long>> save(HttpServletRequest request, @RequestBody ActivityNewsDTO dto) {
        return ApiResponse.ok(Map.of("id", activityNewsService.save(dto, currentUser(request))));
    }

    @PostMapping("/api/admin/activity-news/{newsId}/publish")
    public ApiResponse<Void> publish(HttpServletRequest request, @PathVariable Long newsId) {
        activityNewsService.publish(newsId, currentUser(request));
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/activity-news")
    public ApiResponse<List<ActivityNewsVO>> adminList(HttpServletRequest request) {
        return ApiResponse.ok(activityNewsService.adminList(currentUser(request)));
    }

    @GetMapping("/api/activity-news")
    public ApiResponse<List<ActivityNewsVO>> published() {
        return ApiResponse.ok(activityNewsService.published());
    }

    @GetMapping("/api/activity-news/{newsId}")
    public ApiResponse<ActivityNewsVO> detail(HttpServletRequest request, @PathVariable Long newsId) {
        return ApiResponse.ok(activityNewsService.detail(newsId, currentUser(request)));
    }
}
