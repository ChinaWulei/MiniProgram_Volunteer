package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.ActivityAiAnalysisRequest;
import com.scs.volunteer.dto.ActivityDTO;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.service.ActivityAiAnalysisService;
import com.scs.volunteer.service.ActivityService;
import com.scs.volunteer.vo.AiActivityAnalysisVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/activities")
public class ActivityController extends BaseController {
    private final ActivityService activityService;
    private final ActivityAiAnalysisService activityAiAnalysisService;

    public ActivityController(ActivityService activityService, ActivityAiAnalysisService activityAiAnalysisService) {
        this.activityService = activityService;
        this.activityAiAnalysisService = activityAiAnalysisService;
    }

    @GetMapping
    public ApiResponse<List<Activity>> list(String category, String status, String keyword) {
        return ApiResponse.ok(activityService.list(category, status, keyword));
    }

    @GetMapping("/recommend")
    public ApiResponse<List<Activity>> recommend(HttpServletRequest request) {
        return ApiResponse.ok(activityService.recommend(currentUser(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<Activity> detail(@PathVariable Long id) {
        return ApiResponse.ok(activityService.detail(id));
    }

    @PostMapping("/{id}/ai-analysis")
    public ApiResponse<AiActivityAnalysisVO> aiAnalysis(@PathVariable Long id,
                                                        @RequestBody(required = false) ActivityAiAnalysisRequest body,
                                                        HttpServletRequest request) {
        String question = body == null ? null : body.getQuestion();
        return ApiResponse.ok(activityAiAnalysisService.analyze(id, currentUser(request), question));
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
