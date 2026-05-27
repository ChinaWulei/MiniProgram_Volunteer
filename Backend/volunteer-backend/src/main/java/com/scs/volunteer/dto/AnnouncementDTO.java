package com.scs.volunteer.dto;

import java.util.List;

public class AnnouncementDTO {
    private String title;
    private String content;
    private List<String> imageUrls;
    private List<Long> ruleFileIds;
    private String status;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public List<Long> getRuleFileIds() { return ruleFileIds; }
    public void setRuleFileIds(List<Long> ruleFileIds) { this.ruleFileIds = ruleFileIds; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
