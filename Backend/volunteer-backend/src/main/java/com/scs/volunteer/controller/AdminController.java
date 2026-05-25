package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityDTO;
import com.scs.volunteer.service.ActivityService;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.service.StatisticsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {
    private final StatisticsService statisticsService;
    private final ActivityService activityService;
    private final S3StorageService s3StorageService;

    public AdminController(StatisticsService statisticsService, ActivityService activityService, S3StorageService s3StorageService) {
        this.statisticsService = statisticsService;
        this.activityService = activityService;
        this.s3StorageService = s3StorageService;
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics(HttpServletRequest request) {
        return ApiResponse.ok(statisticsService.overview(currentUser(request)));
    }

    @PostMapping("/activity/image")
    public ApiResponse<Map<String, String>> uploadActivityImage(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        requireAdmin(currentUser(request));
        String url = s3StorageService.uploadActivityCover(file);
        return ApiResponse.ok(Map.of("url", url, "coverImageUrl", url));
    }

    @PostMapping("/activities")
    public ApiResponse<Map<String, Long>> publishActivity(HttpServletRequest request, @RequestBody ActivityDTO dto) {
        return ApiResponse.ok(Map.of("id", activityService.create(dto, currentUser(request))));
    }

    @DeleteMapping("/activities/{id}")
    public ApiResponse<Void> deleteActivity(HttpServletRequest request, @PathVariable Long id) {
        activityService.delete(id, currentUser(request));
        return ApiResponse.ok(null);
    }

    @PostMapping("/activities/{id}/finish")
    public ApiResponse<Void> finishActivity(HttpServletRequest request, @PathVariable Long id) {
        activityService.finish(id, currentUser(request));
        return ApiResponse.ok(null);
    }

    @GetMapping("/activities/{id}/summary")
    public ApiResponse<Map<String, String>> activitySummary(HttpServletRequest request, @PathVariable Long id) {
        return ApiResponse.ok(Map.of("summary", activityService.summary(id, currentUser(request))));
    }

    private void requireAdmin(CurrentUser user) {
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BizException("仅管理员可操作");
        }
    }
}
