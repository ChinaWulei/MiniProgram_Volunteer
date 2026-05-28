package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.CheckinAdjustmentApplyDTO;
import com.scs.volunteer.dto.CheckinAdjustmentAuditDTO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface CheckinAdjustmentService {
    Long apply(CheckinAdjustmentApplyDTO dto, CurrentUser currentUser);
    String uploadProof(MultipartFile file, CurrentUser currentUser);
    List<Map<String, Object>> my(CurrentUser currentUser);
    List<Map<String, Object>> adminList(String auditStatus, Long activityId, String keyword, CurrentUser currentUser);
    void audit(Long id, CheckinAdjustmentAuditDTO dto, CurrentUser currentUser);
    void updateStatus(Long id, CheckinAdjustmentAuditDTO dto, CurrentUser currentUser);
}
