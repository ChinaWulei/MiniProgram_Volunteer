package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.service.RegistrationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/signup")
public class UserSignupController extends BaseController {
    private final RegistrationService registrationService;

    public UserSignupController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping("/list")
    public ApiResponse<List<Map<String, Object>>> list(HttpServletRequest request) {
        return ApiResponse.ok(registrationService.my(currentUser(request)));
    }
}
