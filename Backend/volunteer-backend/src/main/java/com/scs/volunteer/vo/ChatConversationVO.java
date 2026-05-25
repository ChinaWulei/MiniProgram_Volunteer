package com.scs.volunteer.vo;

import java.time.LocalDateTime;

public class ChatConversationVO {
    private Long id;
    private Long peerUserId;
    private String peerName;
    private String peerAvatarUrl;
    private String peerCollege;
    private String peerMajorClass;
    private String lastMessage;
    private LocalDateTime lastMessageAt;
    private Integer unreadCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPeerUserId() { return peerUserId; }
    public void setPeerUserId(Long peerUserId) { this.peerUserId = peerUserId; }
    public String getPeerName() { return peerName; }
    public void setPeerName(String peerName) { this.peerName = peerName; }
    public String getPeerAvatarUrl() { return peerAvatarUrl; }
    public void setPeerAvatarUrl(String peerAvatarUrl) { this.peerAvatarUrl = peerAvatarUrl; }
    public String getPeerCollege() { return peerCollege; }
    public void setPeerCollege(String peerCollege) { this.peerCollege = peerCollege; }
    public String getPeerMajorClass() { return peerMajorClass; }
    public void setPeerMajorClass(String peerMajorClass) { this.peerMajorClass = peerMajorClass; }
    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }
    public LocalDateTime getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(LocalDateTime lastMessageAt) { this.lastMessageAt = lastMessageAt; }
    public Integer getUnreadCount() { return unreadCount; }
    public void setUnreadCount(Integer unreadCount) { this.unreadCount = unreadCount; }
}
