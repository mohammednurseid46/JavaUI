package com.lms.analytics.dao;

import com.lms.analytics.models.Course;
import com.lms.analytics.utils.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDAO {

    // CREATE
    public boolean createCourse(Course course) {
        String sql = "INSERT INTO courses (course_code, course_name, description, credits, " +
                "instructor_id, capacity, price, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, course.getCourseCode());
            pstmt.setString(2, course.getCourseName());
            pstmt.setString(3, course.getDescription());
            pstmt.setInt(4, course.getCredits());
            pstmt.setInt(5, course.getInstructorId());
            pstmt.setInt(6, course.getCapacity());
            pstmt.setBigDecimal(7, course.getPrice());
            pstmt.setString(8, course.getStatus());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        course.setCourseId(rs.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // READ - Get course by ID
    public Course getCourseById(int courseId) {
        String sql = "SELECT c.*, u.full_name as instructor_name FROM courses c " +
                "LEFT JOIN users u ON c.instructor_id = u.user_id " +
                "WHERE c.course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, courseId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractCourseFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // READ - Get course by code
    public Course getCourseByCode(String courseCode) {
        String sql = "SELECT c.*, u.full_name as instructor_name FROM courses c " +
                "LEFT JOIN users u ON c.instructor_id = u.user_id " +
                "WHERE c.course_code = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, courseCode);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractCourseFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // READ - Get all courses
    public List<Course> getAllCourses() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name as instructor_name FROM courses c " +
                "LEFT JOIN users u ON c.instructor_id = u.user_id " +
                "ORDER BY c.course_code";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                courses.add(extractCourseFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }return courses;
    }

    // READ - Get active courses
    public List<Course> getActiveCourses() {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name as instructor_name FROM courses c " +
                "LEFT JOIN users u ON c.instructor_id = u.user_id " +
                "WHERE c.status = 'ACTIVE' ORDER BY c.course_code";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                courses.add(extractCourseFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    // READ - Get courses by instructor
    public List<Course> getCoursesByInstructor(int instructorId) {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name as instructor_name FROM courses c " +
                "LEFT JOIN users u ON c.instructor_id = u.user_id " +
                "WHERE c.instructor_id = ? ORDER BY c.course_code";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, instructorId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                courses.add(extractCourseFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    // Check if course is taught by specific instructor
    public boolean isCourseTaughtByInstructor(int courseId, int instructorId) {
        String sql = "SELECT COUNT(*) FROM courses WHERE course_id = ? AND instructor_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, courseId);
            pstmt.setInt(2, instructorId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // UPDATE
    public boolean updateCourse(Course course) {
        String sql = "UPDATE courses SET course_code = ?, course_name = ?, description = ?, " +
                "credits = ?, instructor_id = ?, capacity = ?, price = ?, status = ? " +
                "WHERE course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, course.getCourseCode());
            pstmt.setString(2, course.getCourseName());
            pstmt.setString(3, course.getDescription());
            pstmt.setInt(4, course.getCredits());
            pstmt.setInt(5, course.getInstructorId());
            pstmt.setInt(6, course.getCapacity());
            pstmt.setBigDecimal(7, course.getPrice());
            pstmt.setString(8, course.getStatus());
            pstmt.setInt(9, course.getCourseId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // UPDATE - Increment enrolled count
    public boolean incrementEnrolledCount(int courseId) {
        String sql = "UPDATE courses SET enrolled_count = enrolled_count + 1 WHERE course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, courseId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // UPDATE - Decrement enrolled count
    public boolean decrementEnrolledCount(int courseId) {
        String sql = "UPDATE courses SET enrolled_count = enrolled_count - 1 WHERE course_id = ? AND enrolled_count > 0";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, courseId);
            return pstmt.executeUpdate() > 0;} catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // DELETE
    public boolean deleteCourse(int courseId) {
        String sql = "DELETE FROM courses WHERE course_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, courseId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Search courses
    public List<Course> searchCourses(String keyword) {
        List<Course> courses = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name as instructor_name FROM courses c " +
                "LEFT JOIN users u ON c.instructor_id = u.user_id " +
                "WHERE c.course_code LIKE ? OR c.course_name LIKE ? OR c.description LIKE ? " +
                "ORDER BY c.course_code";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String searchPattern = "%" + keyword + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                courses.add(extractCourseFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courses;
    }

    private Course extractCourseFromResultSet(ResultSet rs) throws SQLException {
        Course course = new Course();
        course.setCourseId(rs.getInt("course_id"));
        course.setCourseCode(rs.getString("course_code"));
        course.setCourseName(rs.getString("course_name"));
        course.setDescription(rs.getString("description"));
        course.setCredits(rs.getInt("credits"));
        course.setInstructorId(rs.getInt("instructor_id"));
        course.setInstructorName(rs.getString("instructor_name"));
        course.setCapacity(rs.getInt("capacity"));
        course.setEnrolledCount(rs.getInt("enrolled_count"));
        course.setPrice(rs.getBigDecimal("price"));
        course.setStatus(rs.getString("status"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            course.setCreatedAt(createdAt.toLocalDateTime());
        }

        return course;
    }

    // Get course statistics
    public int getTotalCourses() {
        String sql = "SELECT COUNT(*) FROM courses WHERE status = 'ACTIVE'";

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

    public double getAverageOccupancyRate() {
        String sql = "SELECT AVG(CAST(enrolled_count AS FLOAT) / capacity * 100) FROM courses WHERE capacity > 0";

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
}