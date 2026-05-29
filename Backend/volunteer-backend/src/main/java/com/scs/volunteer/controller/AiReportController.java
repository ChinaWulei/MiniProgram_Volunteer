package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.ReportExportRequest;
import com.scs.volunteer.dto.ReportGenerateRequest;
import com.scs.volunteer.service.AiReportService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/report")
public class AiReportController extends BaseController {
    private final AiReportService reportService;

    public AiReportController(AiReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody(required = false) ReportGenerateRequest body,
                                                     HttpServletRequest request) {
        return ApiResponse.ok(reportService.generate(body == null ? new ReportGenerateRequest() : body, currentUser(request)));
    }

    @PostMapping("/export")
    public ApiResponse<Map<String, Object>> export(@RequestBody ReportExportRequest body, HttpServletRequest request) {
        return ApiResponse.ok(reportService.export(body, currentUser(request)));
    }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        return ApiResponse.ok(reportService.list(currentUser(request)));
    }

    @GetMapping("/detail/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(reportService.detail(id, currentUser(request)));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id, HttpServletRequest request) {
        reportService.delete(id, currentUser(request));
        return ApiResponse.ok(null);
    }
}
