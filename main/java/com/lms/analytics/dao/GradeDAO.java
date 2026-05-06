package com.lms.analytics.dao;

import com.lms.analytics.models.Grade;
import com.lms.analytics.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GradeDAO {

    public boolean createGrade(Grade grade) {
        String sql = "INSERT INTO grades (enrollment_id, assignment_name, assignment_type, " +
                "score, max_score, weight, graded_date, comments) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, grade.getEnrollmentId());
            pstmt.setString(2, grade.getAssignmentName());
            pstmt.setString(3, grade.getAssignmentType());
            pstmt.setDouble(4, grade.getScore());
            pstmt.setDouble(5, grade.getMaxScore());
            pstmt.setDouble(6, grade.getWeight());
            pstmt.setObject(7, grade.getGradedDate());
            pstmt.setString(8, grade.getComments());
            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) grade.setGradeId(rs.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Grade> getGradesByEnrollment(int enrollmentId) {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT * FROM grades WHERE enrollment_id = ? ORDER BY graded_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enrollmentId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) grades.add(extractGrade(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return grades;
    }

    public List<Grade> getGradesByCourse(int courseId) {
        List<Grade> grades = new ArrayList<>();
        String sql = "SELECT g.* FROM grades g " +
                "JOIN enrollments e ON g.enrollment_id = e.enrollment_id " +
                "WHERE e.course_id = ? ORDER BY g.graded_date DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) grades.add(extractGrade(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return grades;
    }

    public boolean updateGrade(Grade grade) {
        String sql = "UPDATE grades SET score = ?, max_score = ?, weight = ?, comments = ? WHERE grade_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setDouble(1, grade.getScore());
            pstmt.setDouble(2, grade.getMaxScore());
            pstmt.setDouble(3, grade.getWeight());
            pstmt.setString(4, grade.getComments());
            pstmt.setInt(5, grade.getGradeId());
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteGrade(int gradeId) {
        String sql = "DELETE FROM grades WHERE grade_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, gradeId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public double calculateWeightedAverage(int enrollmentId) {
        String sql = "SELECT SUM(score / max_score * weight) / SUM(weight) * 100 as avg " +
                "FROM grades WHERE enrollment_id = ? AND max_score > 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, enrollmentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getDouble("avg");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private Grade extractGrade(ResultSet rs) throws SQLException {
        Grade g = new Grade();
        g.setGradeId(rs.getInt("grade_id"));
        g.setEnrollmentId(rs.getInt("enrollment_id"));
        g.setAssignmentName(rs.getString("assignment_name"));
        g.setAssignmentType(rs.getString("assignment_type"));
        g.setScore(rs.getDouble("score"));
        g.setMaxScore(rs.getDouble("max_score"));
        g.setWeight(rs.getDouble("weight"));
        g.setComments(rs.getString("comments"));
        Date d = rs.getDate("graded_date");
        if (d != null) g.setGradedDate(d.toLocalDate());
        return g;
    }
}
