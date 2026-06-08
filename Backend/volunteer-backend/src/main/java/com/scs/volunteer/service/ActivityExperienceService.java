package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;

import java.util.List;
import java.util.Map;

public interface ActivityExperienceService {
    List<Map<String, Object>> list(String category, Boolean enabled, CurrentUser currentUser);
    void adopt(Long evaluationId, String type, CurrentUser currentUser);
    void unadopt(Long evaluationId, String type, CurrentUser currentUser);
    void setEnabled(Long id, boolean enabled, CurrentUser currentUser);
    List<Map<String, Object>> enabledByCategory(String category, int limit);
}
