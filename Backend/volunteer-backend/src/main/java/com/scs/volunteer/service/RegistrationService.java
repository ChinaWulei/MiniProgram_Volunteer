package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.RegistrationDTO;
import com.scs.volunteer.dto.ReviewDTO;

import java.util.List;
import java.util.Map;

public interface RegistrationService {
    void register(RegistrationDTO dto, CurrentUser currentUser);
    List<Map<String, Object>> my(CurrentUser currentUser);
    List<Map<String, Object>> adminList(String keyword, String status, Long activityId, String department,
                                        String priorityDepartment, CurrentUser currentUser);
    List<String> adminDepartments(CurrentUser currentUser);
    byte[] exportApproved(Long activityId, CurrentUser currentUser);
    void review(Long id, ReviewDTO dto, CurrentUser currentUser);
    void cancel(Long id, ReviewDTO dto, CurrentUser currentUser);
    void withdraw(Long id, CurrentUser currentUser);
}
