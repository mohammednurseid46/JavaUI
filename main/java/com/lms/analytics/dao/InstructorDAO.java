package com.lms.analytics.dao;

import com.lms.analytics.models.Instructor;
import com.lms.analytics.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InstructorDAO {

    public List<Instructor> getAllInstructors() {
        List<Instructor> instructors = new ArrayList<>();
        String sql = "SELECT i.*, u.full_name, u.email FROM instructors i " +
                     "JOIN users u ON i.user_id = u.user_id " +
                     "WHERE u.role = 'INSTRUCTOR' " +
                     "ORDER BY u.full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                instructors.add(extractInstructor(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return instructors;
    }

    public List<Instructor> searchInstructors(String keyword) {
        List<Instructor> instructors = new ArrayList<>();
        String sql = "SELECT i.*, u.full_name, u.email FROM instructors i " +
                     "JOIN users u ON i.user_id = u.user_id " +
                     "WHERE u.role = 'INSTRUCTOR' " +
                     "AND (u.full_name LIKE ? OR i.employee_number LIKE ? OR u.email LIKE ?) " +
                     "ORDER BY u.full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String pattern = "%" + keyword + "%";
            pstmt.setString(1, pattern);
            pstmt.setString(2, pattern);
            pstmt.setString(3, pattern);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                instructors.add(extractInstructor(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return instructors;
    }

    public Instructor getInstructorById(int instructorId) {
        String sql = "SELECT i.*, u.full_name, u.email FROM instructors i " +
                     "JOIN users u ON i.user_id = u.user_id " +
                     "WHERE i.instructor_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, instructorId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return extractInstructor(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Instructor extractInstructor(ResultSet rs) throws SQLException {
        Instructor i = new Instructor();
        i.setInstructorId(rs.getInt("instructor_id"));
        i.setUserId(rs.getInt("user_id"));
        i.setEmployeeNumber(rs.getString("employee_number"));
        i.setFullName(rs.getString("full_name"));
        i.setEmail(rs.getString("email"));
        i.setDepartment(rs.getString("department"));
        i.setOfficeLocation(rs.getString("office_location"));
        i.setSpecialization(rs.getString("specialization"));

        String hireDate = rs.getString("hire_date");
        if (hireDate != null && !hireDate.isBlank()) {
            try {
                i.setHireDate(java.time.LocalDate.parse(hireDate.substring(0, 10)));
            } catch (Exception ignored) {}
        }
        return i;
    }
}
