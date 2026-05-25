package com.scs.volunteer.vo;

public class MatchVO {
    private VolunteerVO volunteer;
    private double score;
    private String reason;

    public MatchVO(VolunteerVO volunteer, double score, String reason) {
        this.volunteer = volunteer;
        this.score = score;
        this.reason = reason;
    }

    public VolunteerVO getVolunteer() { return volunteer; }
    public void setVolunteer(VolunteerVO volunteer) { this.volunteer = volunteer; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
