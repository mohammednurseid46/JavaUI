package com.lms.analytics.models;

import java.time.LocalDate;

public class Grade {
    private int gradeId;
    private int enrollmentId;
    private String assignmentName;
    private String assignmentType; // QUIZ, ASSIGNMENT, MIDTERM, FINAL, PROJECT
    private double score;
    private double maxScore;
    private double weight;
    private LocalDate gradedDate;
    private String comments;

    public Grade() {}

    public Grade(int enrollmentId, String assignmentName, String assignmentType,
                 double score, double maxScore, double weight) {
        this.enrollmentId = enrollmentId;
        this.assignmentName = assignmentName;
        this.assignmentType = assignmentType;
        this.score = score;
        this.maxScore = maxScore;
        this.weight = weight;
        this.gradedDate = LocalDate.now();
    }

    public int getGradeId() { return gradeId; }
    public void setGradeId(int gradeId) { this.gradeId = gradeId; }

    public int getEnrollmentId() { return enrollmentId; }
    public void setEnrollmentId(int enrollmentId) { this.enrollmentId = enrollmentId; }

    public String getAssignmentName() { return assignmentName; }
    public void setAssignmentName(String assignmentName) { this.assignmentName = assignmentName; }

    public String getAssignmentType() { return assignmentType; }
    public void setAssignmentType(String assignmentType) { this.assignmentType = assignmentType; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public double getMaxScore() { return maxScore; }
    public void setMaxScore(double maxScore) { this.maxScore = maxScore; }

    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    public LocalDate getGradedDate() { return gradedDate; }
    public void setGradedDate(LocalDate gradedDate) { this.gradedDate = gradedDate; }

    public String getComments() { return comments; }
    public void setComments(String comments) { this.comments = comments; }

    public double getPercentage() {
        if (maxScore == 0) return 0;
        return (score / maxScore) * 100;
    }

    public double getWeightedScore() {
        return getPercentage() * (weight / 100);
    }
}
