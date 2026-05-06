package com.lms.analytics.dto;

public class CompletionRateDTO {
    private String courseCode;
    private String courseName;
    private int totalEnrolled;
    private int completed;
    private int inProgress;
    private int dropped;
    private double averageGrade;

    public CompletionRateDTO() {}

    public CompletionRateDTO(String courseCode, String courseName, int totalEnrolled,
                              int completed, int inProgress, int dropped, double averageGrade) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.totalEnrolled = totalEnrolled;
        this.completed = completed;
        this.inProgress = inProgress;
        this.dropped = dropped;
        this.averageGrade = averageGrade;
    }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getTotalEnrolled() { return totalEnrolled; }
    public void setTotalEnrolled(int totalEnrolled) { this.totalEnrolled = totalEnrolled; }

    public int getCompleted() { return completed; }
    public void setCompleted(int completed) { this.completed = completed; }

    public int getInProgress() { return inProgress; }
    public void setInProgress(int inProgress) { this.inProgress = inProgress; }

    public int getDropped() { return dropped; }
    public void setDropped(int dropped) { this.dropped = dropped; }

    public double getAverageGrade() { return averageGrade; }
    public void setAverageGrade(double averageGrade) { this.averageGrade = averageGrade; }

    public double getCompletionRate() {
        if (totalEnrolled == 0) return 0;
        return (double) completed / totalEnrolled * 100;
    }

    public double getDropRate() {
        if (totalEnrolled == 0) return 0;
        return (double) dropped / totalEnrolled * 100;
    }
}
