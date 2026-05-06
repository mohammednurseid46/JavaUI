package com.lms.analytics.dao;

import com.lms.analytics.models.Student;
import com.lms.analytics.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StudentDAO {

    public boolean createStudent(Student student) {
        String sql = "INSERT INTO students " +
                "(user_id, student_number, date_of_birth, phone, address, " +
                "enrollment_date, current_semester, major) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, student.getUserId());
            pstmt.setString(2, student.getStudentNumber());
            pstmt.setObject(3, student.getDateOfBirth());
            pstmt.setString(4, student.getPhone());
            pstmt.setString(5, student.getAddress());
            pstmt.setObject(6, student.getEnrollmentDate());
            pstmt.setInt(7, student.getCurrentSemester());
            pstmt.setString(8, student.getMajor());

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) student.setStudentId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            // If UNIQUE constraint on user_id or student_number — student already exists
            // Check if a row already exists for this user_id and return true
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                Student existing = getStudentByUserId(student.getUserId());
                if (existing != null) {
                    student.setStudentId(existing.getStudentId());
                    return true;
                }
            }
            e.printStackTrace();
        }
        return false;
    }

    public Student getStudentById(int studentId) {
        String sql = "SELECT s.*, u.full_name, u.email FROM students s " +
                "JOIN users u ON s.user_id = u.user_id WHERE s.student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return extractStudent(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Student getStudentByUserId(int userId) {
        String sql = "SELECT s.*, u.full_name, u.email, u.is_active FROM students s " +
                "JOIN users u ON s.user_id = u.user_id WHERE s.user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return extractStudent(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        // Use LEFT JOIN and include all users with role=STUDENT regardless of is_active
        String sql = "SELECT s.*, u.full_name, u.email, u.is_active FROM students s " +
                "JOIN users u ON s.user_id = u.user_id " +
                "WHERE u.role = 'STUDENT' " +
                "ORDER BY u.full_name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) students.add(extractStudent(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public List<Student> searchStudents(String keyword) {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT s.*, u.full_name, u.email FROM students s " +
                "JOIN users u ON s.user_id = u.user_id " +
                "WHERE u.full_name LIKE ? OR s.student_number LIKE ? OR s.major LIKE ? " +
                "ORDER BY u.full_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String p = "%" + keyword + "%";
            pstmt.setString(1, p);
            pstmt.setString(2, p);
            pstmt.setString(3, p);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) students.add(extractStudent(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return students;
    }

    public boolean updateStudent(Student student) {
        String sql = "UPDATE students SET phone = ?, address = ?, current_semester = ?, major = ? " +
                "WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, student.getPhone());
            pstmt.setString(2, student.getAddress());
            pstmt.setInt(3, student.getCurrentSemester());
            pstmt.setString(4, student.getMajor());
            pstmt.setInt(5, student.getStudentId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteStudent(int studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getTotalStudents() {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Student extractStudent(ResultSet rs) throws SQLException {
        Student s = new Student();
        s.setStudentId(rs.getInt("student_id"));
        s.setUserId(rs.getInt("user_id"));
        s.setStudentNumber(rs.getString("student_number"));
        s.setFullName(rs.getString("full_name"));
        s.setEmail(rs.getString("email"));
        s.setPhone(rs.getString("phone"));
        s.setAddress(rs.getString("address"));
        s.setCurrentSemester(rs.getInt("current_semester"));
        s.setMajor(rs.getString("major"));

        // Use getString to avoid SQLite date parsing issues
        String dob = rs.getString("date_of_birth");
        if (dob != null && !dob.isBlank()) {
            try { s.setDateOfBirth(java.time.LocalDate.parse(dob.substring(0, 10))); }
            catch (Exception ignored) {}
        }
        String enDate = rs.getString("enrollment_date");
        if (enDate != null && !enDate.isBlank()) {
            try { s.setEnrollmentDate(java.time.LocalDate.parse(enDate.substring(0, 10))); }
            catch (Exception ignored) {}
        }
        return s;
    }
}
