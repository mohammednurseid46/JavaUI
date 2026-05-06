package com.lms.analytics.dto;

public class EnrollmentTrendDTO {
    private String month;
    private int enrollmentCount;
    private int dropCount;
    private int completionCount;

    public EnrollmentTrendDTO() {}

    public EnrollmentTrendDTO(String month, int enrollmentCount, int dropCount, int completionCount) {
        this.month = month;
        this.enrollmentCount = enrollmentCount;
        this.dropCount = dropCount;
        this.completionCount = completionCount;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public int getEnrollmentCount() { return enrollmentCount; }
    public void setEnrollmentCount(int enrollmentCount) { this.enrollmentCount = enrollmentCount; }

    public int getDropCount() { return dropCount; }
    public void setDropCount(int dropCount) { this.dropCount = dropCount; }

    public int getCompletionCount() { return completionCount; }
    public void setCompletionCount(int completionCount) { this.completionCount = completionCount; }

    public double getRetentionRate() {
        if (enrollmentCount == 0) return 0;
        return (double)(enrollmentCount - dropCount) / enrollmentCount * 100;
    }
}
