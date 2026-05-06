package com.lms.analytics.services;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.lms.analytics.dao.*;
import com.lms.analytics.utils.DatabaseConnection;
import java.sql.*;
import java.util.*;

public class AnalyticsService {

    private CourseDAO courseDAO;
    private EnrollmentDAO enrollmentDAO;
    private UserDAO userDAO;

    public AnalyticsService() {
        this.courseDAO = new CourseDAO();
        this.enrollmentDAO = new EnrollmentDAO();
        this.userDAO = new UserDAO();
    }

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalStudents", getTotalStudents());
        stats.put("activeCourses", courseDAO.getTotalCourses());
        stats.put("totalEnrollments", enrollmentDAO.getTotalEnrollments());
        stats.put("completionRate", getCompletionRatePercentage());
        stats.put("averageGrade", enrollmentDAO.getAverageGrade());
        stats.put("totalRevenue", getTotalRevenue());

        return stats;
    }

    private int getTotalStudents() {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private double getCompletionRatePercentage() {
        int total = enrollmentDAO.getTotalEnrollments();
        int completed = enrollmentDAO.getCompletedEnrollments();
        if (total == 0) return 0;
        return (double) completed / total * 100;
    }

    private double getTotalRevenue() {
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'COMPLETED'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public Map<String, Integer> getMonthlyEnrollmentTrends() {
        Map<String, Integer> trends = new LinkedHashMap<>();
        // Show all months that have enrollments (up to last 12 months)
        String sql = """
            SELECT strftime('%Y-%m', enrollment_date) as month, COUNT(*) as count
            FROM enrollments
            WHERE enrollment_date IS NOT NULL
              AND enrollment_date >= date('now', '-12 months')
            GROUP BY month
            ORDER BY month ASC
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String month = rs.getString("month");
                int count = rs.getInt("count");
                if (month != null) trends.put(month, count);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // If no data in last 12 months, get all available data
        if (trends.isEmpty()) {
            String allSql = """
                SELECT strftime('%Y-%m', enrollment_date) as month, COUNT(*) as count
                FROM enrollments
                WHERE enrollment_date IS NOT NULL
                GROUP BY month
                ORDER BY month ASC
                LIMIT 12
            """;
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(allSql)) {
                while (rs.next()) {
                    String month = rs.getString("month");
                    int count = rs.getInt("count");
                    if (month != null) trends.put(month, count);
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return trends;
    }

    public Map<String, Double> getCompletionRates() {
        Map<String, Double> rates = new HashMap<>();
        int total = enrollmentDAO.getTotalEnrollments();
        int completed = enrollmentDAO.getCompletedEnrollments();
        int enrolled = enrollmentDAO.getActiveEnrollments();

        if (total > 0) {
            rates.put("Completed", (double) completed / total * 100);
            rates.put("In Progress", (double) enrolled / total * 100);
            rates.put("Not Started", 100 - ((completed + enrolled) * 100.0 / total));
        }

        return rates;}

    public ObservableList<Map<String, Object>> getRecentEnrollments() {
        ObservableList<Map<String, Object>> recentEnrollments = FXCollections.observableArrayList();

        String sql = """
            SELECT u.full_name as studentName, c.course_name as courseName, 
                   e.enrollment_date as date, e.status
            FROM enrollments e
            JOIN students s ON e.student_id = s.student_id
            JOIN users u ON s.user_id = u.user_id
            JOIN courses c ON e.course_id = c.course_id
            ORDER BY e.enrollment_date DESC
            LIMIT 10
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                row.put("studentName", rs.getString("studentName"));
                row.put("courseName", rs.getString("courseName"));
                row.put("date", rs.getString("date"));
                row.put("status", rs.getString("status"));
                recentEnrollments.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return recentEnrollments;
    }
}