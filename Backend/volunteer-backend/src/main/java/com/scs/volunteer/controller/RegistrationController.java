package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.RegistrationDTO;
import com.scs.volunteer.dto.ReviewDTO;
import com.scs.volunteer.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/registrations")
public class RegistrationController extends BaseController {
    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping
    public ApiResponse<Void> register(@RequestBody RegistrationDTO dto, HttpServletRequest request) {
        registrationService.register(dto, currentUser(request));
        return ApiResponse.ok(null);
    }

    @GetMapping("/my")
    public ApiResponse<List<Map<String, Object>>> my(HttpServletRequest request) {
        return ApiResponse.ok(registrationService.my(currentUser(request)));
    }

    @GetMapping("/admin")
    public ApiResponse<List<Map<String, Object>>> adminList(String keyword, String status, Long activityId,
                                                            String department, HttpServletRequest request) {
        return ApiResponse.ok(registrationService.adminList(
                keyword, status, activityId, department, currentUser(request)));
    }

    @GetMapping("/admin/departments")
    public ApiResponse<List<String>> adminDepartments(HttpServletRequest request) {
        return ApiResponse.ok(registrationService.adminDepartments(currentUser(request)));
    }

    @GetMapping("/admin/activities/{activityId}/approved-export")
    public void exportApproved(@PathVariable Long activityId, HttpServletRequest request,
                               HttpServletResponse response) throws java.io.IOException {
        byte[] content = registrationService.exportApproved(activityId, currentUser(request));
        String filename = URLEncoder.encode("活动录取志愿者名单-" + activityId + ".xlsx", StandardCharsets.UTF_8)
                .replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + filename);
        response.setContentLength(content.length);
        response.getOutputStream().write(content);
    }

    @PutMapping("/{id}/review")
    public ApiResponse<Void> review(@PathVariable Long id, @RequestBody ReviewDTO dto, HttpServletRequest request) {
        registrationService.review(id, dto, currentUser(request));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(@PathVariable Long id, @RequestBody ReviewDTO dto, HttpServletRequest request) {
        registrationService.cancel(id, dto, currentUser(request));
        return ApiResponse.ok(null);
    }

    @PostMapping("/{id}/withdraw")
    public ApiResponse<Void> withdraw(@PathVariable Long id, HttpServletRequest request) {
        registrationService.withdraw(id, currentUser(request));
        return ApiResponse.ok(null);
    }
}
