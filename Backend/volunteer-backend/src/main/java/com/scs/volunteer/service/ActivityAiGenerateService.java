package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityAiGenerateRequest;
import com.scs.volunteer.vo.ActivityAiGenerateVO;

public interface ActivityAiGenerateService {
    ActivityAiGenerateVO generate(ActivityAiGenerateRequest request, CurrentUser currentUser);
}
