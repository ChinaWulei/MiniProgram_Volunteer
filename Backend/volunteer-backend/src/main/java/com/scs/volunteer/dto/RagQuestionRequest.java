package com.scs.volunteer.dto;

public class RagQuestionRequest {
    private String question;
    private Integer topK;

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public Integer getTopK() { return topK; }
    public void setTopK(Integer topK) { this.topK = topK; }
}
