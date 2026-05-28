package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.CheckinAdjustmentApplyDTO;
import com.scs.volunteer.dto.CheckinAdjustmentAuditDTO;
import com.scs.volunteer.service.CheckinAdjustmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
public class CheckinAdjustmentController extends BaseController {
    private final CheckinAdjustmentService checkinAdjustmentService;

    public CheckinAdjustmentController(CheckinAdjustmentService checkinAdjustmentService) {
        this.checkinAdjustmentService = checkinAdjustmentService;
    }

    @PostMapping("/api/checkin-adjustments/apply")
    public ApiResponse<Map<String, Long>> apply(HttpServletRequest request, @RequestBody CheckinAdjustmentApplyDTO body) {
        return ApiResponse.ok(Map.of("id", checkinAdjustmentService.apply(body, currentUser(request))));
    }

    @PostMapping("/api/checkin-adjustments/proof")
    public ApiResponse<Map<String, String>> uploadProof(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        String url = checkinAdjustmentService.uploadProof(file, currentUser(request));
        return ApiResponse.ok(Map.of("url", url, "proofImageUrl", url));
    }

    @GetMapping("/api/checkin-adjustments/my")
    public ApiResponse<List<Map<String, Object>>> my(HttpServletRequest request) {
        return ApiResponse.ok(checkinAdjustmentService.my(currentUser(request)));
    }

    @GetMapping("/api/admin/checkin-adjustments/pending")
    public ApiResponse<List<Map<String, Object>>> adminList(HttpServletRequest request, String auditStatus, Long activityId, String keyword) {
        return ApiResponse.ok(checkinAdjustmentService.adminList(auditStatus, activityId, keyword, currentUser(request)));
    }

    @PostMapping("/api/admin/checkin-adjustments/{id}/audit")
    public ApiResponse<Void> audit(HttpServletRequest request, @PathVariable Long id, @RequestBody CheckinAdjustmentAuditDTO body) {
        checkinAdjustmentService.audit(id, body, currentUser(request));
        return ApiResponse.ok(null);
    }

    @PostMapping("/api/admin/checkin-adjustments/{id}/update-status")
    public ApiResponse<Void> updateStatus(HttpServletRequest request, @PathVariable Long id, @RequestBody CheckinAdjustmentAuditDTO body) {
        checkinAdjustmentService.updateStatus(id, body, currentUser(request));
        return ApiResponse.ok(null);
    }
}
