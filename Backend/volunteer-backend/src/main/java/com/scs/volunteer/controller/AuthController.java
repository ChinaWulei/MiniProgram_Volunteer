package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.LoginDTO;
import com.scs.volunteer.dto.RegisterDTO;
import com.scs.volunteer.service.AuthService;
import com.scs.volunteer.vo.LoginVO;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginVO> login(@RequestBody LoginDTO dto) {
        return ApiResponse.ok(authService.login(dto));
    }

    @PostMapping("/register")
    public ApiResponse<LoginVO> register(@RequestBody RegisterDTO dto) {
        return ApiResponse.ok(authService.register(dto));
    }
}
