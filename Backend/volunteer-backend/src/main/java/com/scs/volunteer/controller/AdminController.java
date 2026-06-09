package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.BizException;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityAiGenerateRequest;
import com.scs.volunteer.dto.ActivityParticipantNoticeDTO;
import com.scs.volunteer.dto.ActivityDTO;
import com.scs.volunteer.dto.CreditRuleDTO;
import com.scs.volunteer.dto.ManualCheckinRequest;
import com.scs.volunteer.entity.Activity;
import com.scs.volunteer.entity.RuleFile;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.service.ActivityAiGenerateService;
import com.scs.volunteer.service.ActivityService;
import com.scs.volunteer.service.CheckinService;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.service.StatisticsService;
import com.scs.volunteer.service.RuleFileService;
import com.scs.volunteer.mapper.CreditMapper;
import com.scs.volunteer.mapper.NotificationMapper;
import com.scs.volunteer.mapper.RegistrationMapper;
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
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import com.scs.volunteer.vo.ActivityAiGenerateVO;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {
    private final StatisticsService statisticsService;
    private final ActivityService activityService;
    private final S3StorageService s3StorageService;
    private final CheckinService checkinService;
    private final CreditMapper creditMapper;
    private final ActivityAiGenerateService activityAiGenerateService;
    private final ActivityMapper activityMapper;
    private final RegistrationMapper registrationMapper;
    private final NotificationMapper notificationMapper;
    private final RuleFileService ruleFileService;

    public AdminController(StatisticsService statisticsService, ActivityService activityService, S3StorageService s3StorageService,
                           CheckinService checkinService, CreditMapper creditMapper, ActivityAiGenerateService activityAiGenerateService,
                           ActivityMapper activityMapper, RegistrationMapper registrationMapper,
                           NotificationMapper notificationMapper, RuleFileService ruleFileService) {
        this.statisticsService = statisticsService;
        this.activityService = activityService;
        this.s3StorageService = s3StorageService;
        this.checkinService = checkinService;
        this.creditMapper = creditMapper;
        this.activityAiGenerateService = activityAiGenerateService;
        this.activityMapper = activityMapper;
        this.registrationMapper = registrationMapper;
        this.notificationMapper = notificationMapper;
        this.ruleFileService = ruleFileService;
    }

    @GetMapping("/statistics")
    public ApiResponse<Map<String, Object>> statistics(HttpServletRequest request,
                                                       @RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate) {
        return ApiResponse.ok(statisticsService.overview(currentUser(request), startDate, endDate));
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

    @PostMapping("/activities/ai-generate")
    public ApiResponse<ActivityAiGenerateVO> aiGenerateActivity(HttpServletRequest request, @RequestBody ActivityAiGenerateRequest body) {
        return ApiResponse.ok(activityAiGenerateService.generate(body, currentUser(request)));
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

    @PostMapping("/activities/{id}/notifications")
    @Transactional
    public ApiResponse<Map<String, Integer>> sendActivityNotice(HttpServletRequest request,
                                                                @PathVariable Long id,
                                                                @RequestBody ActivityParticipantNoticeDTO dto) {
        requireAdmin(currentUser(request));
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
            throw new BizException("请填写通知标题");
        }
        if (dto.getContent() == null || dto.getContent().isBlank()) {
            throw new BizException("请填写通知内容");
        }
        java.util.List<Long> ruleFileIds = dto.getRuleFileIds() == null
                ? java.util.List.of()
                : dto.getRuleFileIds().stream().filter(java.util.Objects::nonNull).distinct().toList();
        if (ruleFileIds.size() > 5) {
            throw new BizException("单次通知最多上传5个附件");
        }
        for (Long ruleFileId : ruleFileIds) {
            ruleFileService.detail(ruleFileId);
        }
        Activity activity = activityMapper.findById(id).orElseThrow(() -> new BizException("活动不存在"));
        java.util.List<Long> userIds = registrationMapper.participantUserIds(id, dto.getScope());
        String title = limit(dto.getTitle().trim(), 120);
        String content = limit("《" + activity.getName() + "》：" + dto.getContent().trim(), 500);
        for (Long userId : userIds) {
            Long notificationId = notificationMapper.insert(userId, "ACTIVITY_NOTICE", title, content, "ACTIVITY", id);
            notificationMapper.addAttachments(notificationId, ruleFileIds);
        }
        return ApiResponse.ok(Map.of("sentCount", userIds.size()));
    }

    @PostMapping("/activity-notifications/attachments")
    public ApiResponse<Map<String, Object>> uploadActivityNoticeAttachment(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "originalName", required = false) String originalName) {
        RuleFile uploaded = ruleFileService.upload(file, currentUser(request), originalName);
        return ApiResponse.ok(Map.of(
                "id", uploaded.getId(),
                "fileName", uploaded.getOriginalName(),
                "fileType", uploaded.getFileType(),
                "url", uploaded.getS3Url(),
                "status", uploaded.getStatus()
        ));
    }

    @PostMapping("/activity-notifications/images")
    public ApiResponse<Map<String, Object>> uploadActivityNoticeImage(
            HttpServletRequest request,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "originalName", required = false) String originalName) {
        RuleFile uploaded = ruleFileService.uploadImage(file, currentUser(request), originalName);
        return ApiResponse.ok(Map.of(
                "id", uploaded.getId(),
                "fileName", uploaded.getOriginalName(),
                "fileType", uploaded.getFileType(),
                "url", uploaded.getS3Url(),
                "status", uploaded.getStatus(),
                "isImage", true
        ));
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

    private String limit(String value, int max) {
        if (value == null) return "";
        return value.length() > max ? value.substring(0, max) : value;
    }
}
