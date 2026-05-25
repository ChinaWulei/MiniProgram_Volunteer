package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityInviteRequest;
import com.scs.volunteer.dto.ConversationRequest;
import com.scs.volunteer.dto.InviteReplyRequest;
import com.scs.volunteer.dto.MessageRequest;
import com.scs.volunteer.service.ChatService;
import com.scs.volunteer.vo.ChatConversationVO;
import com.scs.volunteer.vo.ChatMessageVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController extends BaseController {
    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ApiResponse<List<ChatConversationVO>> conversations(HttpServletRequest request) {
        return ApiResponse.ok(chatService.conversations(currentUser(request).getId()));
    }

    @PostMapping("/conversations")
    public ApiResponse<Map<String, Long>> conversation(HttpServletRequest request, @RequestBody ConversationRequest body) {
        return ApiResponse.ok(Map.of("conversationId", chatService.getOrCreateConversation(currentUser(request).getId(), body)));
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public ApiResponse<List<ChatMessageVO>> messages(HttpServletRequest request, @PathVariable Long conversationId) {
        return ApiResponse.ok(chatService.messages(currentUser(request).getId(), conversationId));
    }

    @PostMapping("/messages")
    public ApiResponse<ChatMessageVO> send(HttpServletRequest request, @RequestBody MessageRequest body) {
        return ApiResponse.ok(chatService.sendMessage(currentUser(request).getId(), body));
    }

    @PostMapping("/activity-invite")
    public ApiResponse<ChatMessageVO> invite(HttpServletRequest request, @RequestBody ActivityInviteRequest body) {
        CurrentUser user = currentUser(request);
        return ApiResponse.ok(chatService.sendActivityInvite(user.getId(), user.getRole(), body));
    }

    @PostMapping("/activity-invite/{id}/reply")
    public ApiResponse<ChatMessageVO> reply(HttpServletRequest request, @PathVariable Long id, @RequestBody InviteReplyRequest body) {
        return ApiResponse.ok(chatService.replyInvite(currentUser(request).getId(), id, body));
    }
}
