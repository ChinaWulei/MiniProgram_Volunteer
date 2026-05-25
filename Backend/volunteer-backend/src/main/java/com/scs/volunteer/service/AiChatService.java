package com.scs.volunteer.service;

import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.AiChatRequest;
import com.scs.volunteer.vo.AiChatResponseVO;

public interface AiChatService {
    AiChatResponseVO chat(AiChatRequest request, CurrentUser currentUser);
}
