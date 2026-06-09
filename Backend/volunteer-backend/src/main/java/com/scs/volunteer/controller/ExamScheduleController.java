package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.mapper.ExamScheduleMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exam-schedules")
public class ExamScheduleController extends BaseController {
    private final ExamScheduleMapper examScheduleMapper;

    public ExamScheduleController(ExamScheduleMapper examScheduleMapper) {
        this.examScheduleMapper = examScheduleMapper;
    }

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        return ApiResponse.ok(examScheduleMapper.list(currentUser(request).getId()));
    }

    @PutMapping
    public ApiResponse<Void> replace(@RequestBody Map<String, List<Map<String, Object>>> body, HttpServletRequest request) {
        examScheduleMapper.replace(currentUser(request).getId(), body == null ? List.of() : body.get("exams"));
        return ApiResponse.ok(null);
    }
}
