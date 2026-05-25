package com.scs.volunteer.service.impl;

import com.scs.volunteer.common.BizException;
import com.scs.volunteer.dto.ActivityInviteRequest;
import com.scs.volunteer.dto.ConversationRequest;
import com.scs.volunteer.dto.InviteReplyRequest;
import com.scs.volunteer.dto.MessageRequest;
import com.scs.volunteer.mapper.ActivityMapper;
import com.scs.volunteer.mapper.ChatMapper;
import com.scs.volunteer.service.ChatService;
import com.scs.volunteer.websocket.ChatWebSocketHandler;
import com.scs.volunteer.vo.ChatConversationVO;
import com.scs.volunteer.vo.ChatMessageVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {
    private final ChatMapper chatMapper;
    private final ActivityMapper activityMapper;
    private final ChatWebSocketHandler webSocketHandler;

    public ChatServiceImpl(ChatMapper chatMapper, ActivityMapper activityMapper, ChatWebSocketHandler webSocketHandler) {
        this.chatMapper = chatMapper;
        this.activityMapper = activityMapper;
        this.webSocketHandler = webSocketHandler;
    }

    @Override
    public List<ChatConversationVO> conversations(Long userId) {
        return chatMapper.conversations(userId);
    }

    @Override
    public Long getOrCreateConversation(Long userId, ConversationRequest request) {
        if (request.getTargetUserId() == null || request.getTargetUserId().equals(userId)) {
            throw new BizException("请选择有效的联系人");
        }
        return chatMapper.findPrivateConversation(userId, request.getTargetUserId())
                .orElseGet(() -> chatMapper.createPrivateConversation(userId, request.getTargetUserId()));
    }

    @Override
    public List<ChatMessageVO> messages(Long userId, Long conversationId) {
        ensureParticipant(userId, conversationId);
        chatMapper.markRead(conversationId, userId);
        return chatMapper.messages(conversationId);
    }

    @Override
    public ChatMessageVO sendMessage(Long userId, MessageRequest request) {
        Long conversationId = request.getConversationId();
        Long receiverId = request.getReceiverId();
        if (conversationId == null) {
            if (receiverId == null) throw new BizException("请选择联系人");
            conversationId = getOrCreateConversation(userId, target(receiverId));
        } else {
            ensureParticipant(userId, conversationId);
            receiverId = chatMapper.peerId(conversationId, userId);
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank() && request.getActivityId() == null) throw new BizException("消息内容不能为空");
        ChatMessageVO message = chatMapper.insertMessage(conversationId, userId, receiverId,
                request.getActivityId() == null ? "TEXT" : "ACTIVITY_CARD", content, request.getActivityId(), null);
        webSocketHandler.pushToUser(receiverId, "message", message);
        return message;
    }

    @Override
    public ChatMessageVO sendActivityInvite(Long userId, String role, ActivityInviteRequest request) {
        if (!"ADMIN".equals(role)) throw new BizException("仅管理员可发送活动邀请");
        if (request.getReceiverId() == null || request.getActivityId() == null) throw new BizException("邀请信息不完整");
        activityMapper.findById(request.getActivityId()).orElseThrow(() -> new BizException("活动不存在"));
        Long conversationId = getOrCreateConversation(userId, target(request.getReceiverId()));
        String reason = request.getReason() == null || request.getReason().isBlank() ? "邀请你参加这项学院志愿活动" : request.getReason();
        ChatMessageVO message = chatMapper.insertMessage(conversationId, userId, request.getReceiverId(), "ACTIVITY_INVITE", reason, request.getActivityId(), "PENDING");
        webSocketHandler.pushToUser(request.getReceiverId(), "activityInvite", message);
        return message;
    }

    @Override
    public ChatMessageVO replyInvite(Long userId, Long inviteId, InviteReplyRequest request) {
        ChatMessageVO invite = chatMapper.findMessage(inviteId).orElseThrow(() -> new BizException("邀请不存在"));
        if (!userId.equals(invite.getReceiverId())) throw new BizException("只能回复发给自己的邀请");
        String status = "ACCEPTED".equalsIgnoreCase(request.getStatus()) ? "ACCEPTED" : "DECLINED";
        chatMapper.updateInviteStatus(inviteId, status);
        String text = request.getContent();
        if (text == null || text.isBlank()) text = "ACCEPTED".equals(status) ? "我接受这个活动邀请" : "我暂时无法参加这个活动";
        ChatMessageVO reply = chatMapper.insertMessage(invite.getConversationId(), userId, invite.getSenderId(), "TEXT", text, null, null);
        webSocketHandler.pushToUser(invite.getSenderId(), "message", reply);
        return chatMapper.findMessage(inviteId).orElse(invite);
    }

    private void ensureParticipant(Long userId, Long conversationId) {
        if (!chatMapper.isParticipant(conversationId, userId)) throw new BizException("无权查看该聊天记录");
    }

    private ConversationRequest target(Long targetUserId) {
        ConversationRequest request = new ConversationRequest();
        request.setTargetUserId(targetUserId);
        return request;
    }
}
