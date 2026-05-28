package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.vo.AiActivityAnalysisVO;

public interface ActivityAiAnalysisService {
    AiActivityAnalysisVO analyze(Long activityId, CurrentUser currentUser, String question);
}
