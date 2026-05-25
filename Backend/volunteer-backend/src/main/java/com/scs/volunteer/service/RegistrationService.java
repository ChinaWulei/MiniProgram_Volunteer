package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.RegistrationDTO;
import com.scs.volunteer.dto.ReviewDTO;

import java.util.List;
import java.util.Map;

public interface RegistrationService {
    void register(RegistrationDTO dto, CurrentUser currentUser);
    List<Map<String, Object>> my(CurrentUser currentUser);
    void review(Long id, ReviewDTO dto, CurrentUser currentUser);
}
