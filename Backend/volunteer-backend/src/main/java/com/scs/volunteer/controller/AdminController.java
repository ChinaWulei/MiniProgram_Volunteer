package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityDTO;
import com.scs.volunteer.dto.CreditRuleDTO;
import com.scs.volunteer.dto.ManualCheckinRequest;
import com.scs.volunteer.service.ActivityService;
import com.scs.volunteer.service.CheckinService;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.service.StatisticsService;
import com.scs.volunteer.mapper.CreditMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {
    private final StatisticsService statisticsService;
    private final ActivityService activityService;
    private final S3StorageService s3StorageService;
    private final CheckinService checkinService;
    private final CreditMapper creditMapper;

    public AdminController(StatisticsService statisticsService, ActivityService activityService, S3StorageService s3StorageService, CheckinService checkinService, CreditMapper creditMapper) {
        this.statisticsService = statisticsService;
        this.activityService = activityService;
        this.s3StorageService = s3StorageService;
        this.checkinService = checkinService;
        this.creditMapper = creditMapper;
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics(HttpServletRequest request) {
        return ApiResponse.ok(statisticsService.overview(currentUser(request)));
    }

    @GetMapping("/credit-rules")
    public ApiResponse<java.util.List<Map<String, Object>>> creditRules(HttpServletRequest request) {
        requireAdmin(currentUser(request));
        return ApiResponse.ok(creditMapper.rules());
    }

    @PostMapping("/credit-rules")
    public ApiResponse<Void> saveCreditRule(HttpServletRequest request, @RequestBody CreditRuleDTO dto) {
        requireAdmin(currentUser(request));
        if (dto == null || dto.getCode() == null || dto.getName() == null || dto.getChangeValue() == null) {
            throw new BizException("信用规则信息不完整");
        }
        creditMapper.saveRule(dto.getCode(), dto.getName(), dto.getChangeValue(), dto.getEnabled() == null || dto.getEnabled());
        return ApiResponse.ok(null);
    }

    @PostMapping("/activity/image")
    public ApiResponse<Map<String, String>> uploadActivityImage(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        requireAdmin(currentUser(request));
        String url = s3StorageService.uploadActivityCover(file);
        return ApiResponse.ok(Map.of("url", url, "coverImageUrl", url));
    }

    @PostMapping("/activities")
    public ApiResponse<Map<String, Long>> publishActivity(HttpServletRequest request, @RequestBody ActivityDTO dto) {
        return ApiResponse.ok(Map.of("id", activityService.create(dto, currentUser(request))));
    }

    @DeleteMapping("/activities/{id}")
    public ApiResponse<Void> deleteActivity(HttpServletRequest request, @PathVariable Long id) {
        activityService.delete(id, currentUser(request));
        return ApiResponse.ok(null);
    }

    @PostMapping("/activities/{id}/finish")
    public ApiResponse<Void> finishActivity(HttpServletRequest request, @PathVariable Long id) {
        activityService.finish(id, currentUser(request));
        return ApiResponse.ok(null);
    }

    @GetMapping("/activities/{id}/summary")
    public ApiResponse<Map<String, String>> activitySummary(HttpServletRequest request, @PathVariable Long id) {
        return ApiResponse.ok(Map.of("summary", activityService.summary(id, currentUser(request))));
    }

    @GetMapping("/activities/{id}/checkin/statistics")
    public ApiResponse<Map<String, Object>> checkinStatistics(HttpServletRequest request, @PathVariable Long id) {
        return ApiResponse.ok(checkinService.activityStatistics(id, currentUser(request)));
    }

    @GetMapping("/activities/{id}/checkin/list")
    public ApiResponse<java.util.List<Map<String, Object>>> checkinList(HttpServletRequest request, @PathVariable Long id, String status, String keyword) {
        return ApiResponse.ok(checkinService.activityList(id, status, keyword, currentUser(request)));
    }

    @PostMapping("/activities/{id}/checkin/manual")
    public ApiResponse<Void> manualCheckin(HttpServletRequest request, @PathVariable Long id, @RequestBody ManualCheckinRequest body) {
        checkinService.manual(id, body, currentUser(request));
        return ApiResponse.ok(null);
    }

    @GetMapping("/volunteers/{userId}/checkin/statistics")
    public ApiResponse<Map<String, Object>> volunteerCheckinStatistics(HttpServletRequest request, @PathVariable Long userId) {
        return ApiResponse.ok(checkinService.volunteerStatistics(userId, currentUser(request)));
    }

    private void requireAdmin(CurrentUser user) {
        if (user == null || !"ADMIN".equals(user.getRole())) {
            throw new BizException("仅管理员可操作");
        }
    }
}
