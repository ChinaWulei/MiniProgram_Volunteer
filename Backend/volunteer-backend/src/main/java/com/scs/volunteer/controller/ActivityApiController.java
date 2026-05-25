package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.RegistrationDTO;
import com.scs.volunteer.service.ActivityService;
import com.scs.volunteer.service.RegistrationService;
import com.scs.volunteer.vo.ActivityDetailVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/activity")
public class ActivityApiController extends BaseController {
    private final ActivityService activityService;
    private final RegistrationService registrationService;

    public ActivityApiController(ActivityService activityService, RegistrationService registrationService) {
        this.activityService = activityService;
        this.registrationService = registrationService;
    }

    @GetMapping("/{id}")
    public ApiResponse<ActivityDetailVO> detail(@PathVariable Long id, HttpServletRequest request) {
        return ApiResponse.ok(activityService.detail(id, currentUser(request)));
    }

    @PostMapping("/{id}/signup")
    public ApiResponse<Void> signup(@PathVariable Long id, HttpServletRequest request) {
        RegistrationDTO dto = new RegistrationDTO();
        dto.setActivityId(id);
        registrationService.register(dto, currentUser(request));
        return ApiResponse.ok(null);
    }
}
