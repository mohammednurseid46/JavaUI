package com.lms.analytics.utils;

import org.apache.poi.xwpf.usermodel.*;
import com.lms.analytics.utils.DatabaseConnection;

import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WordExporter {

    public boolean generateCertificate(String filePath, int studentId, int courseId, int instructorId) {
        try (XWPFDocument doc = new XWPFDocument()) {

            String studentName = getStudentName(studentId);
            String courseName  = getCourseName(courseId);
            String instructorName = getInstructorName(instructorId);
            String date        = getCompletionDate(studentId, courseId);

            // Header
            addCenteredBoldParagraph(doc, "CERTIFICATE OF COMPLETION", 24);
            addCenteredParagraph(doc, " ", 12);

            // Main content
            addCenteredParagraph(doc, "This certificate is proudly presented to", 14);
            addCenteredParagraph(doc, " ", 8);
            addCenteredBoldParagraph(doc, studentName, 28);
            addCenteredParagraph(doc, " ", 8);

            // Details section with IDs
            addCenteredParagraph(doc, "Student ID: " + (studentId > 0 ? studentId : "N/A"), 12);
            addCenteredParagraph(doc, " ", 6);

            addCenteredParagraph(doc, "for successfully completing the course", 14);
            addCenteredParagraph(doc, " ", 8);
            addCenteredBoldParagraph(doc, courseName, 22);
            addCenteredParagraph(doc, " ", 6);

            // Course and Instructor details
            addCenteredParagraph(doc, "Course ID: " + (courseId > 0 ? courseId : "N/A"), 12);
            addCenteredParagraph(doc, " ", 6);

            if (instructorId > 0 && !instructorName.equals("Unknown Instructor")) {
                addCenteredParagraph(doc, "Instructor: " + instructorName, 12);
                addCenteredParagraph(doc, "Instructor ID: " + instructorId, 12);
                addCenteredParagraph(doc, " ", 6);
            }

            // Completion date
            addCenteredParagraph(doc, "Completion Date: " + date, 12);

            // Footer with signature line
            addCenteredParagraph(doc, " ", 24);
            addCenteredParagraph(doc, "_______________________________", 12);
            addCenteredParagraph(doc, "Authorized Signature", 10);

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                doc.write(out);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean generateStudentReport(String filePath, int studentId) {
        try (XWPFDocument doc = new XWPFDocument()) {

            String studentName = getStudentName(studentId);
            addCenteredBoldParagraph(doc, "STUDENT ACADEMIC REPORT", 20);
            addParagraph(doc, "Student: " + studentName, 12);
            addParagraph(doc, "Generated: " + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 10);
            addParagraph(doc, " ", 10);

            XWPFTable table = doc.createTable();
            XWPFTableRow header = table.getRow(0);
            header.getCell(0).setText("Course");
            header.addNewTableCell().setText("Grade");
            header.addNewTableCell().setText("Status");

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT c.course_name, e.grade, e.status FROM enrollments e " +
                         "JOIN courses c ON e.course_id=c.course_id WHERE e.student_id=?")) {
                pstmt.setInt(1, studentId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    XWPFTableRow row = table.createRow();
                    row.getCell(0).setText(rs.getString("course_name"));
                    double g = rs.getDouble("grade");
                    row.getCell(1).setText(rs.wasNull() ? "N/A" : String.format("%.1f", g));
                    row.getCell(2).setText(rs.getString("status"));
                }
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                doc.write(out);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addCenteredBoldParagraph(XWPFDocument doc, String text, int fontSize) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setBold(true);
        run.setFontFamily("Times New Roman");
    }

    private void addCenteredParagraph(XWPFDocument doc, String text, int fontSize) {
        XWPFParagraph p = doc.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setFontFamily("Times New Roman");
    }

    private void addParagraph(XWPFDocument doc, String text, int fontSize) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
    }

    private String getStudentName(int studentId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT u.full_name FROM students s JOIN users u ON s.user_id=u.user_id WHERE s.student_id=?")) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Unknown Student";
    }

    private String getCourseName(int courseId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT course_name FROM courses WHERE course_id=?")) {
            pstmt.setInt(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("course_name");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Unknown Course";
    }

    private String getInstructorName(int instructorId) {
        if (instructorId <= 0) return "Unknown Instructor";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT u.full_name FROM instructors i JOIN users u ON i.user_id=u.user_id WHERE i.instructor_id=?")) {
            pstmt.setInt(1, instructorId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Unknown Instructor";
    }

    private String getCompletionDate(int studentId, int courseId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT completion_date FROM enrollments WHERE student_id=? AND course_id=?")) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, courseId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getDate("completion_date") != null)
                return rs.getDate("completion_date").toString();
        } catch (SQLException e) { e.printStackTrace(); }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }
}
