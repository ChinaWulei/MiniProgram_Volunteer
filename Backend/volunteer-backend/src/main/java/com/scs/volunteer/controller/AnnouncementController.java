package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.AnnouncementDTO;
import com.scs.volunteer.entity.RuleFile;
import com.scs.volunteer.service.AnnouncementService;
import com.scs.volunteer.vo.AnnouncementVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class AnnouncementController extends BaseController {
    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @PostMapping("/api/admin/announcements/images")
    public ApiResponse<Map<String, List<String>>> uploadImages(HttpServletRequest request, @RequestParam("files") MultipartFile[] files) {
        return ApiResponse.ok(Map.of("urls", announcementService.uploadImages(files, currentUser(request))));
    }

    @PostMapping("/api/admin/announcements/attachments")
    public ApiResponse<Map<String, Object>> uploadAttachment(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        RuleFile ruleFile = announcementService.uploadAttachment(file, currentUser(request));
        return ApiResponse.ok(announcementService.attachmentResult(ruleFile));
    }

    @PostMapping("/api/admin/announcements")
    public ApiResponse<Map<String, Long>> save(HttpServletRequest request, @RequestBody AnnouncementDTO dto) {
        return ApiResponse.ok(Map.of("id", announcementService.save(dto, currentUser(request))));
    }

    @PostMapping("/api/admin/announcements/{id}/publish")
    public ApiResponse<Void> publish(HttpServletRequest request, @PathVariable Long id) {
        announcementService.publish(id, currentUser(request));
        return ApiResponse.ok(null);
    }

    @GetMapping("/api/admin/announcements")
    public ApiResponse<List<AnnouncementVO>> adminList(HttpServletRequest request) {
        return ApiResponse.ok(announcementService.adminList(currentUser(request)));
    }

    @GetMapping("/api/announcements")
    public ApiResponse<List<AnnouncementVO>> published() {
        return ApiResponse.ok(announcementService.published());
    }

    @GetMapping("/api/announcements/{id}")
    public ApiResponse<AnnouncementVO> detail(HttpServletRequest request, @PathVariable Long id) {
        return ApiResponse.ok(announcementService.detail(id, currentUser(request)));
    }
}
