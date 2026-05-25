package com.scs.volunteer.vo;

import java.util.List;

public class AiChatResponseVO {
    private String sessionId;
    private String reply;
    private List<AiRecommendedActivityVO> recommendedActivities;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getReply() { return reply; }
    public void setReply(String reply) { this.reply = reply; }
    public List<AiRecommendedActivityVO> getRecommendedActivities() { return recommendedActivities; }
    public void setRecommendedActivities(List<AiRecommendedActivityVO> recommendedActivities) { this.recommendedActivities = recommendedActivities; }
}
