package com.scs.volunteer.vo;

public class RuleChunkVO {
    private Long ruleFileId;
    private String fileName;
    private Integer chunkIndex;
    private String content;
    private Double distance;

    public Long getRuleFileId() { return ruleFileId; }
    public void setRuleFileId(Long ruleFileId) { this.ruleFileId = ruleFileId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
}
