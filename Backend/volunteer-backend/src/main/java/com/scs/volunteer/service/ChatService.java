package com.scs.volunteer.service;

import com.scs.volunteer.dto.ActivityInviteRequest;
import com.scs.volunteer.dto.ConversationRequest;
import com.scs.volunteer.dto.InviteReplyRequest;
import com.scs.volunteer.dto.MessageRequest;
import com.scs.volunteer.vo.ChatConversationVO;
import com.scs.volunteer.vo.ChatMessageVO;

import java.util.List;

public interface ChatService {
    List<ChatConversationVO> conversations(Long userId);
    Long getOrCreateConversation(Long userId, ConversationRequest request);
    List<ChatMessageVO> messages(Long userId, Long conversationId);
    ChatMessageVO sendMessage(Long userId, MessageRequest request);
    ChatMessageVO sendActivityInvite(Long userId, String role, ActivityInviteRequest request);
    ChatMessageVO replyInvite(Long userId, Long inviteId, InviteReplyRequest request);
}
