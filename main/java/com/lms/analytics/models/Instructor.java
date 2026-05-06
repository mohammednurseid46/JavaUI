package com.lms.analytics.models;

import java.time.LocalDate;

public class Instructor {
    private int instructorId;
    private int userId;
    private String employeeNumber;
    private String fullName;
    private String email;
    private String department;
    private String officeLocation;
    private LocalDate hireDate;
    private String specialization;

    public Instructor() {}

    public Instructor(int userId, String employeeNumber, String department, String specialization) {
        this.userId = userId;
        this.employeeNumber = employeeNumber;
        this.department = department;
        this.specialization = specialization;
        this.hireDate = LocalDate.now();
    }

    public int getInstructorId() { return instructorId; }
    public void setInstructorId(int instructorId) { this.instructorId = instructorId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getEmployeeNumber() { return employeeNumber; }
    public void setEmployeeNumber(String employeeNumber) { this.employeeNumber = employeeNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getOfficeLocation() { return officeLocation; }
    public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }

    public LocalDate getHireDate() { return hireDate; }
    public void setHireDate(LocalDate hireDate) { this.hireDate = hireDate; }

    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    @Override
    public String toString() { return fullName + " (" + employeeNumber + ")"; }
}
