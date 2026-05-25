package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.UserProfileDTO;
import com.scs.volunteer.service.UserProfileService;
import com.scs.volunteer.vo.UserProfileVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController extends BaseController {
    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/profile")
    public ApiResponse<UserProfileVO> profile(HttpServletRequest request) {
        return ApiResponse.ok(userProfileService.profile(currentUser(request).getId()));
    }

    @PutMapping("/profile")
    public ApiResponse<UserProfileVO> update(@RequestBody UserProfileDTO dto, HttpServletRequest request) {
        return ApiResponse.ok(userProfileService.update(currentUser(request).getId(), dto));
    }

    @PostMapping("/avatar")
    public ApiResponse<Map<String, String>> avatar(@RequestParam("file") MultipartFile file, HttpServletRequest request) {
        CurrentUser user = currentUser(request);
        return ApiResponse.ok(Map.of("avatarUrl", userProfileService.updateAvatar(user.getId(), file)));
    }
}
