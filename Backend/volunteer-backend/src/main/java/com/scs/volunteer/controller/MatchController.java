package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.service.MatchService;
import com.scs.volunteer.vo.MatchVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/match")
public class MatchController extends BaseController {
    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/activity/{activityId}")
    public ApiResponse<List<MatchVO>> top5(@PathVariable Long activityId, HttpServletRequest request) {
        return ApiResponse.ok(matchService.top5(activityId, currentUser(request)));
    }
}
