package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.dto.AiChatRequest;
import com.scs.volunteer.service.AiChatService;
import com.scs.volunteer.vo.AiChatResponseVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController extends BaseController {
    private final AiChatService aiChatService;

    public AiController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponseVO> chat(@RequestBody AiChatRequest request, HttpServletRequest httpRequest) {
        return ApiResponse.ok(aiChatService.chat(request, currentUser(httpRequest)));
    }
}
