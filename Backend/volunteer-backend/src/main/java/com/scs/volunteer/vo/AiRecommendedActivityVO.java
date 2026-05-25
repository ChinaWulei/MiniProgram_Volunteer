package com.scs.volunteer.vo;

public class AiRecommendedActivityVO {
    private Long id;
    private String title;
    private String time;
    private String location;
    private Integer remainingSlots;
    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public Integer getRemainingSlots() { return remainingSlots; }
    public void setRemainingSlots(Integer remainingSlots) { this.remainingSlots = remainingSlots; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
