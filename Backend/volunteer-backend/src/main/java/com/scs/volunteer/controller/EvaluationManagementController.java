package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.mapper.EvaluationMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class EvaluationManagementController extends BaseController {
    private final EvaluationMapper evaluationMapper;

    public EvaluationManagementController(EvaluationMapper evaluationMapper) {
        this.evaluationMapper = evaluationMapper;
    }

    @GetMapping("/api/evaluations/my")
    public ApiResponse<List<Map<String, Object>>> my(HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        if (user == null) throw new BizException("请先登录");
        return ApiResponse.ok(evaluationMapper.my(user.getId()));
    }

}
