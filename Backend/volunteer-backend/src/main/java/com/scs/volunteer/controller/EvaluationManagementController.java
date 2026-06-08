package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.mapper.EvaluationMapper;
import com.scs.volunteer.service.ActivityExperienceService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class EvaluationManagementController extends BaseController {
    private final EvaluationMapper evaluationMapper;
    private final ActivityExperienceService activityExperienceService;

    public EvaluationManagementController(EvaluationMapper evaluationMapper, ActivityExperienceService activityExperienceService) {
        this.evaluationMapper = evaluationMapper;
        this.activityExperienceService = activityExperienceService;
    }

    @GetMapping("/api/evaluations/my")
    public ApiResponse<List<Map<String, Object>>> my(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user == null) throw new BizException("请先登录");
        return ApiResponse.ok(evaluationMapper.my(user.getId()));
    }

    @GetMapping("/api/admin/experiences")
    public ApiResponse<List<Map<String, Object>>> experiences(HttpServletRequest request,
                                                              @RequestParam(required = false) String category,
                                                              @RequestParam(required = false) Boolean enabled) {
        return ApiResponse.ok(activityExperienceService.list(category, enabled, currentUser(request)));
    }

    @PostMapping("/api/admin/experiences/{id}/enabled")
    public ApiResponse<Void> enableExperience(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> body) {
        activityExperienceService.setEnabled(id, Boolean.TRUE.equals(body.get("enabled")), currentUser(request));
        return ApiResponse.ok(null);
    }
}
