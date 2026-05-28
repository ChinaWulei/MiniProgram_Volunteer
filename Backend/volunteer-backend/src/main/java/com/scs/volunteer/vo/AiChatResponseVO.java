package com.scs.volunteer.vo;

import java.util.List;

public class AiChatResponseVO {
    private String sessionId;
    private String intent;
    private String answer;
    private String reply;
    private List<String> sources;
    private List<AiRecommendedActivityVO> recommendations;
    private List<AiRecommendedActivityVO> recommendedActivities;

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) {
        this.answer = answer;
        this.reply = answer;
    }
    public String getReply() { return reply; }
    public void setReply(String reply) {
        this.reply = reply;
        this.answer = reply;
    }
    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }
    public List<AiRecommendedActivityVO> getRecommendations() { return recommendations; }
    public void setRecommendations(List<AiRecommendedActivityVO> recommendations) {
        this.recommendations = recommendations;
        this.recommendedActivities = recommendations;
    }
    public List<AiRecommendedActivityVO> getRecommendedActivities() { return recommendedActivities; }
    public void setRecommendedActivities(List<AiRecommendedActivityVO> recommendedActivities) {
        this.recommendedActivities = recommendedActivities;
        this.recommendations = recommendedActivities;
    }
}
