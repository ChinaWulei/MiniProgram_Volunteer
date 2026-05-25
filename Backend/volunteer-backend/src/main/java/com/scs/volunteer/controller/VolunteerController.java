package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.service.VolunteerService;
import com.scs.volunteer.vo.VolunteerVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/volunteers")
public class VolunteerController {
    private final VolunteerService volunteerService;

    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    @GetMapping
    public ApiResponse<List<VolunteerVO>> list(String college, String majorClass, String skillTag, String keyword, String sortBy) {
        return ApiResponse.ok(volunteerService.list(college, majorClass, skillTag, keyword, sortBy));
    }

    @GetMapping("/{id}")
    public ApiResponse<VolunteerVO> detail(@PathVariable Long id) {
        return ApiResponse.ok(volunteerService.detail(id));
    }
}
