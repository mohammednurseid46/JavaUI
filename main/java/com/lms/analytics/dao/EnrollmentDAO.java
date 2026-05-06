package com.lms.analytics.dao;

import com.lms.analytics.models.Enrollment;
import com.lms.analytics.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentDAO {

    private CourseDAO courseDAO = new CourseDAO();

    // ── Shared SQL fragment for resolving student name ─────────────────
    // Path 1: enrollment.student_id → students.student_id → users.full_name
    // Path 2: enrollment.student_id used directly as users.user_id (fallback)
    // Path 3: show student_id number as last resort
    private static final String NAME_SELECT =
        "COALESCE(s.student_number, s2.student_number, '') AS student_number, " +
        "COALESCE(u1.full_name, u2.full_name, u3.full_name, " +
        "  'Student ID:' || e.student_id) AS student_name, " +
        "COALESCE(c.course_code, '') AS course_code, " +
        "COALESCE(c.course_name, 'Unknown Course') AS course_name ";

    private static final String NAME_JOINS =
        // Path 1: via students table
        "LEFT JOIN students s  ON e.student_id = s.student_id " +
        "LEFT JOIN users    u1 ON s.user_id    = u1.user_id " +
        // Path 2: enrollment.student_id IS the user_id directly
        "LEFT JOIN users    u2 ON e.student_id = u2.user_id " +
        // Path 3: enrollment.student_id matches students.user_id directly
        "LEFT JOIN students s2 ON e.student_id = s2.user_id " +
        "LEFT JOIN users    u3 ON s2.user_id   = u3.user_id " +
        "LEFT JOIN courses  c  ON e.course_id  = c.course_id ";

    // CREATE
    public boolean enrollStudent(int studentId, int courseId) {
        if (isEnrolled(studentId, courseId)) return false;

        String sql = "INSERT INTO enrollments (student_id, course_id, enrollment_date, status) " +
                "VALUES (?, ?, ?, 'ENROLLED')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, courseId);
            pstmt.setDate(3, Date.valueOf(LocalDate.now()));
            if (pstmt.executeUpdate() > 0) {
                courseDAO.incrementEnrolledCount(courseId);
                return true;
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // READ - by ID
    public Enrollment getEnrollmentById(int enrollmentId) {
        String sql = "SELECT e.enrollment_id, e.student_id, e.course_id, " +
                "e.enrollment_date, e.status, e.grade, e.completion_date, " +
                NAME_SELECT +
                "FROM enrollments e " + NAME_JOINS +
                "WHERE e.enrollment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enrollmentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return extractEnrollmentFromResultSet(rs);
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // READ - by student
    public List<Enrollment> getEnrollmentsByStudent(int studentId) {
        List<Enrollment> list = new ArrayList<>();
        String sql = "SELECT e.enrollment_id, e.student_id, e.course_id, " +
                "e.enrollment_date, e.status, e.grade, e.completion_date, " +
                NAME_SELECT +
                "FROM enrollments e " + NAME_JOINS +
                "WHERE e.student_id = ? ORDER BY e.enrollment_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(extractEnrollmentFromResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // READ - by course
    public List<Enrollment> getEnrollmentsByCourse(int courseId) {
        List<Enrollment> list = new ArrayList<>();
        String sql = "SELECT e.enrollment_id, e.student_id, e.course_id, " +
                "e.enrollment_date, e.status, e.grade, e.completion_date, " +
                NAME_SELECT +
                "FROM enrollments e " + NAME_JOINS +
                "WHERE e.course_id = ? ORDER BY COALESCE(u1.full_name, u2.full_name)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) list.add(extractEnrollmentFromResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // READ - all
    public List<Enrollment> getAllEnrollments() {
        List<Enrollment> list = new ArrayList<>();
        String sql = "SELECT e.enrollment_id, e.student_id, e.course_id, " +
                "e.enrollment_date, e.status, e.grade, e.completion_date, " +
                NAME_SELECT +
                "FROM enrollments e " + NAME_JOINS +
                "ORDER BY e.enrollment_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(extractEnrollmentFromResultSet(rs));
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // UPDATE - status
    public boolean updateEnrollmentStatus(int enrollmentId, String status) {
        String sql = "UPDATE enrollments SET status = ? WHERE enrollment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, enrollmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // UPDATE - grade
    public boolean updateGrade(int enrollmentId, double grade) {
        String sql = "UPDATE enrollments SET grade = ?, completion_date = ? WHERE enrollment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, grade);
            if (grade >= 60) pstmt.setDate(2, Date.valueOf(LocalDate.now()));
            else             pstmt.setNull(2, Types.DATE);
            pstmt.setInt(3, enrollmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // DELETE - drop
    public boolean dropEnrollment(int enrollmentId) {
        String sql = "DELETE FROM enrollments WHERE enrollment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            Enrollment enrollment = getEnrollmentById(enrollmentId);
            if (enrollment != null)
                courseDAO.decrementEnrolledCount(enrollment.getCourseId());
            pstmt.setInt(1, enrollmentId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    // CHECK - is enrolled
    public boolean isEnrolled(int studentId, int courseId) {
        String sql = "SELECT COUNT(*) FROM enrollments WHERE student_id = ? AND course_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, courseId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) { e.printStackTrace(); }
        return false;
    }

    private Enrollment extractEnrollmentFromResultSet(ResultSet rs) throws SQLException {
        Enrollment e = new Enrollment();
        e.setEnrollmentId(rs.getInt("enrollment_id"));
        e.setStudentId(rs.getInt("student_id"));
        e.setStudentName(rs.getString("student_name") != null
            ? rs.getString("student_name") : "Student #" + rs.getInt("student_id"));
        e.setCourseId(rs.getInt("course_id"));
        e.setCourseName(rs.getString("course_name") != null ? rs.getString("course_name") : "Unknown Course");
        e.setCourseCode(rs.getString("course_code") != null ? rs.getString("course_code") : "");

        // Use getString to avoid SQLite date parsing issues
        String enrollDate = rs.getString("enrollment_date");
        if (enrollDate != null && !enrollDate.isBlank()) {
            try { e.setEnrollmentDate(java.time.LocalDate.parse(enrollDate.substring(0, 10))); }
            catch (Exception ignored) {}
        }

        e.setStatus(rs.getString("status"));

        double grade = rs.getDouble("grade");
        if (!rs.wasNull()) e.setGrade(grade);

        String compDate = rs.getString("completion_date");
        if (compDate != null && !compDate.isBlank()) {
            try { e.setCompletionDate(java.time.LocalDate.parse(compDate.substring(0, 10))); }
            catch (Exception ignored) {}
        }
        return e;
    }

    // Analytics
    public int getTotalEnrollments() {
        return countQuery("SELECT COUNT(*) FROM enrollments");
    }

    public int getActiveEnrollments() {
        return countQuery("SELECT COUNT(*) FROM enrollments WHERE status = 'ENROLLED'");
    }

    public int getCompletedEnrollments() {
        return countQuery("SELECT COUNT(*) FROM enrollments WHERE status = 'COMPLETED'");
    }

    public double getAverageGrade() {
        String sql = "SELECT AVG(grade) FROM enrollments WHERE grade IS NOT NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }

    private int countQuery(String sql) {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { e.printStackTrace(); }
        return 0;
    }
}
