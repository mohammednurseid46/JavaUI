package com.lms.analytics.models;

import java.time.LocalDate;

public class Student {
    private int studentId;
    private int userId;
    private String studentNumber;
    private String fullName;
    private String email;
    private LocalDate dateOfBirth;
    private String phone;
    private String address;
    private LocalDate enrollmentDate;
    private int currentSemester;
    private String major;

    public Student() {}

    public Student(int userId, String studentNumber, String major) {
        this.userId = userId;
        this.studentNumber = studentNumber;
        this.major = major;
        this.enrollmentDate = LocalDate.now();
        this.currentSemester = 1;
    }

    public int getStudentId() { return studentId; }
    public void setStudentId(int studentId) { this.studentId = studentId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getStudentNumber() { return studentNumber; }
    public void setStudentNumber(String studentNumber) { this.studentNumber = studentNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }

    public int getCurrentSemester() { return currentSemester; }
    public void setCurrentSemester(int currentSemester) { this.currentSemester = currentSemester; }

    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }

    @Override
    public String toString() { return fullName + " (" + studentNumber + ")"; }
}
