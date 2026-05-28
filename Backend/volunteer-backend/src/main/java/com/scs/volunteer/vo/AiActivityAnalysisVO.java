package com.scs.volunteer.vo;

public class AiActivityAnalysisVO {
    private Long activityId;
    private String analysis;

    public AiActivityAnalysisVO() {
    }

    public AiActivityAnalysisVO(Long activityId, String analysis) {
        this.activityId = activityId;
        this.analysis = analysis;
    }

    public Long getActivityId() {
        return activityId;
    }

    public void setActivityId(Long activityId) {
        this.activityId = activityId;
    }

    public String getAnalysis() {
        return analysis;
    }

    public void setAnalysis(String analysis) {
        this.analysis = analysis;
    }
}
