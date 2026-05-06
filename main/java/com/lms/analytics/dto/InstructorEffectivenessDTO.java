package com.lms.analytics.dto;

public class InstructorEffectivenessDTO {
    private int instructorId;
    private String instructorName;
    private String department;
    private int totalCourses;
    private int totalStudents;
    private double averageGrade;
    private double completionRate;
    private double averageRating;

    public InstructorEffectivenessDTO() {}

    public InstructorEffectivenessDTO(int instructorId, String instructorName, String department,
                                       int totalCourses, int totalStudents, double averageGrade,
                                       double completionRate, double averageRating) {
        this.instructorId = instructorId;
        this.instructorName = instructorName;
        this.department = department;
        this.totalCourses = totalCourses;
        this.totalStudents = totalStudents;
        this.averageGrade = averageGrade;
        this.completionRate = completionRate;
        this.averageRating = averageRating;
    }

    public int getInstructorId() { return instructorId; }
    public void setInstructorId(int instructorId) { this.instructorId = instructorId; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public int getTotalCourses() { return totalCourses; }
    public void setTotalCourses(int totalCourses) { this.totalCourses = totalCourses; }

    public int getTotalStudents() { return totalStudents; }
    public void setTotalStudents(int totalStudents) { this.totalStudents = totalStudents; }

    public double getAverageGrade() { return averageGrade; }
    public void setAverageGrade(double averageGrade) { this.averageGrade = averageGrade; }

    public double getCompletionRate() { return completionRate; }
    public void setCompletionRate(double completionRate) { this.completionRate = completionRate; }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { this.averageRating = averageRating; }

    public String getEffectivenessLevel() {
        double score = (completionRate * 0.4) + (averageGrade * 0.4) + (averageRating * 20 * 0.2);
        if (score >= 80) return "Excellent";
        if (score >= 65) return "Good";
        if (score >= 50) return "Average";
        return "Needs Improvement";
    }
}
