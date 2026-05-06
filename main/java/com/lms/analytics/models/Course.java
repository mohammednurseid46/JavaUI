package com.lms.analytics.models;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Course {
    private int courseId;
    private String courseCode;
    private String courseName;
    private String description;
    private int credits;
    private int instructorId;
    private String instructorName; // For display
    private int capacity;
    private int enrolledCount;
    private BigDecimal price;
    private String status; // ACTIVE, COMPLETED, CANCELLED
    private LocalDateTime createdAt;

    public Course() {}

    public Course(String courseCode, String courseName, String description, int credits,
                  int instructorId, int capacity, BigDecimal price) {
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.description = description;
        this.credits = credits;
        this.instructorId = instructorId;
        this.capacity = capacity;
        this.price = price;
        this.status = "ACTIVE";
        this.enrolledCount = 0;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public int getCourseId() { return courseId; }
    public void setCourseId(int courseId) { this.courseId = courseId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }

    public int getInstructorId() { return instructorId; }
    public void setInstructorId(int instructorId) { this.instructorId = instructorId; }

    public String getInstructorName() { return instructorName; }
    public void setInstructorName(String instructorName) { this.instructorName = instructorName; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public int getEnrolledCount() { return enrolledCount; }
    public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public int getAvailableSeats() {
        return capacity - enrolledCount;
    }

    public double getOccupancyRate() {
        if (capacity == 0) return 0;
        return (double) enrolledCount / capacity * 100;
    }
}