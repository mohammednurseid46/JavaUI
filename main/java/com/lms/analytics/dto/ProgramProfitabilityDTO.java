package com.lms.analytics.dto;

public class ProgramProfitabilityDTO {
    private String courseCode;
    private String courseName;
    private int enrolledStudents;
    private double pricePerStudent;
    private double totalRevenue;
    private double operatingCost;
    private double profit;

    public ProgramProfitabilityDTO() {}

    public ProgramProfitabilityDTO(String courseCode, String courseName, int enrolledStudents,
                                    double pricePerStudent, double totalRevenue, double operatingCost) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.enrolledStudents = enrolledStudents;
        this.pricePerStudent = pricePerStudent;
        this.totalRevenue = totalRevenue;
        this.operatingCost = operatingCost;
        this.profit = totalRevenue - operatingCost;
    }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public int getEnrolledStudents() { return enrolledStudents; }
    public void setEnrolledStudents(int enrolledStudents) { this.enrolledStudents = enrolledStudents; }

    public double getPricePerStudent() { return pricePerStudent; }
    public void setPricePerStudent(double pricePerStudent) { this.pricePerStudent = pricePerStudent; }

    public double getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(double totalRevenue) {
        this.totalRevenue = totalRevenue;
        this.profit = totalRevenue - operatingCost;
    }

    public double getOperatingCost() { return operatingCost; }
    public void setOperatingCost(double operatingCost) {
        this.operatingCost = operatingCost;
        this.profit = totalRevenue - operatingCost;
    }

    public double getProfit() { return profit; }

    public double getProfitMargin() {
        if (totalRevenue == 0) return 0;
        return (profit / totalRevenue) * 100;
    }

    public boolean isProfitable() { return profit > 0; }
}
