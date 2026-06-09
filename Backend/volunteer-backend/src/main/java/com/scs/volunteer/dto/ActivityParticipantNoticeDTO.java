package com.scs.volunteer.dto;

public class ActivityParticipantNoticeDTO {
    private String title;
    private String content;
    private String scope;
    private java.util.List<Long> ruleFileIds;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public java.util.List<Long> getRuleFileIds() {
        return ruleFileIds;
    }

    public void setRuleFileIds(java.util.List<Long> ruleFileIds) {
        this.ruleFileIds = ruleFileIds;
    }
}
