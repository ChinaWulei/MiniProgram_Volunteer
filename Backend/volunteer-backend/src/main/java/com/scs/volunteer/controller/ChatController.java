package com.scs.volunteer.controller;

import com.scs.volunteer.common.ApiResponse;
import com.scs.volunteer.common.CurrentUser;
import com.scs.volunteer.dto.ActivityInviteRequest;
import com.scs.volunteer.dto.ConversationRequest;
import com.scs.volunteer.dto.InviteReplyRequest;
import com.scs.volunteer.dto.MessageRequest;
import com.scs.volunteer.service.ChatService;
import com.scs.volunteer.service.S3StorageService;
import com.scs.volunteer.vo.ChatConversationVO;
import com.scs.volunteer.vo.ChatMessageVO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController extends BaseController {
    private final ChatService chatService;
    private final S3StorageService s3StorageService;

    public ChatController(ChatService chatService, S3StorageService s3StorageService) {
        this.chatService = chatService;
        this.s3StorageService = s3StorageService;
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

    @PostMapping("/images")
    public ApiResponse<Map<String, String>> image(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        currentUser(request);
        String url = s3StorageService.uploadActivityNewsImage(file);
        return ApiResponse.ok(Map.of("url", url, "imageUrl", url));
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

    @GetMapping("/activity-invites")
    public ApiResponse<List<ChatMessageVO>> activityInvites(HttpServletRequest request) {
        return ApiResponse.ok(chatService.activityInvites(currentUser(request).getId()));
    }

    @GetMapping("/activity-invites/unread-count")
    public ApiResponse<Map<String, Integer>> unreadActivityInviteCount(HttpServletRequest request) {
        return ApiResponse.ok(Map.of("unreadCount", chatService.unreadActivityInviteCount(currentUser(request).getId())));
    }

    @PostMapping("/messages/{id}/read")
    public ApiResponse<Void> markMessageRead(HttpServletRequest request, @PathVariable Long id) {
        chatService.markMessageRead(currentUser(request).getId(), id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/block/{targetUserId}")
    public ApiResponse<Void> block(HttpServletRequest request, @PathVariable Long targetUserId) {
        chatService.block(currentUser(request).getId(), targetUserId);
        return ApiResponse.ok(null);
    }

    @GetMapping("/block/{targetUserId}")
    public ApiResponse<Map<String, Boolean>> blockStatus(HttpServletRequest request, @PathVariable Long targetUserId) {
        return ApiResponse.ok(Map.of("blocked", chatService.blockedByMe(currentUser(request).getId(), targetUserId)));
    }

    @DeleteMapping("/block/{targetUserId}")
    public ApiResponse<Void> unblock(HttpServletRequest request, @PathVariable Long targetUserId) {
        chatService.unblock(currentUser(request).getId(), targetUserId);
        return ApiResponse.ok(null);
    }
}
