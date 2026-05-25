package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.RegistrationDTO;
import com.scs.volunteer.dto.ReviewDTO;
import com.scs.volunteer.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

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
}
