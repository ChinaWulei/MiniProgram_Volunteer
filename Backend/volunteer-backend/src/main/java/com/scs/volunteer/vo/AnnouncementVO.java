package com.scs.volunteer.vo;

import java.time.LocalDateTime;
import java.util.List;

public class AnnouncementVO {
    private Long id;
    private String title;
    private String content;
    private String status;
    private Long createdBy;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private List<String> imageUrls;
    private List<Attachment> attachments;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }

    public static class Attachment {
        private Long ruleFileId;
        private String fileName;
        private String fileType;
        private String url;

        public Long getRuleFileId() { return ruleFileId; }
        public void setRuleFileId(Long ruleFileId) { this.ruleFileId = ruleFileId; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public String getFileType() { return fileType; }
        public void setFileType(String fileType) { this.fileType = fileType; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
    }
}
