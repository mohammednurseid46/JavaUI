package com.lms.analytics.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import com.lms.analytics.utils.DatabaseConnection;

import java.io.FileOutputStream;
import java.sql.*;

public class ExcelExporter {

    public boolean exportStudents(String filePath) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Students");
            CellStyle headerStyle = createHeaderStyle(wb);

            String[] headers = {"ID", "Student Number", "Full Name", "Email", "Major", "Semester", "Enrollment Date"};
            createHeaderRow(sheet, headers, headerStyle);

            int rowNum = 1;
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT s.student_id, s.student_number, u.full_name, u.email, " +
                         "s.major, s.current_semester, s.enrollment_date " +
                         "FROM students s JOIN users u ON s.user_id=u.user_id ORDER BY u.full_name")) {
                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(rs.getInt("student_id"));
                    row.createCell(1).setCellValue(rs.getString("student_number"));
                    row.createCell(2).setCellValue(rs.getString("full_name"));
                    row.createCell(3).setCellValue(rs.getString("email"));
                    row.createCell(4).setCellValue(rs.getString("major"));
                    row.createCell(5).setCellValue(rs.getInt("current_semester"));
                    row.createCell(6).setCellValue(rs.getString("enrollment_date"));
                }
            }
            autoSize(sheet, headers.length);
            writeFile(wb, filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportCourses(String filePath) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Courses");
            CellStyle headerStyle = createHeaderStyle(wb);

            String[] headers = {"Code", "Name", "Credits", "Instructor", "Capacity", "Enrolled", "Price", "Status"};
            createHeaderRow(sheet, headers, headerStyle);

            int rowNum = 1;
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT c.course_code, c.course_name, c.credits, u.full_name as instructor, " +
                         "c.capacity, c.enrolled_count, c.price, c.status " +
                         "FROM courses c LEFT JOIN users u ON c.instructor_id=u.user_id ORDER BY c.course_code")) {
                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(rs.getString("course_code"));
                    row.createCell(1).setCellValue(rs.getString("course_name"));
                    row.createCell(2).setCellValue(rs.getInt("credits"));
                    row.createCell(3).setCellValue(rs.getString("instructor"));
                    row.createCell(4).setCellValue(rs.getInt("capacity"));
                    row.createCell(5).setCellValue(rs.getInt("enrolled_count"));
                    row.createCell(6).setCellValue(rs.getDouble("price"));
                    row.createCell(7).setCellValue(rs.getString("status"));
                }
            }
            autoSize(sheet, headers.length);
            writeFile(wb, filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportEnrollments(String filePath) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Enrollments");
            CellStyle headerStyle = createHeaderStyle(wb);

            String[] headers = {"Student", "Course", "Enrollment Date", "Status", "Grade"};
            createHeaderRow(sheet, headers, headerStyle);

            int rowNum = 1;
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT u.full_name, c.course_name, e.enrollment_date, e.status, e.grade " +
                         "FROM enrollments e " +
                         "JOIN students s ON e.student_id=s.student_id " +
                         "JOIN users u ON s.user_id=u.user_id " +
                         "JOIN courses c ON e.course_id=c.course_id " +
                         "ORDER BY e.enrollment_date DESC")) {
                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(rs.getString("full_name"));
                    row.createCell(1).setCellValue(rs.getString("course_name"));
                    row.createCell(2).setCellValue(rs.getString("enrollment_date"));
                    row.createCell(3).setCellValue(rs.getString("status"));
                    double grade = rs.getDouble("grade");
                    row.createCell(4).setCellValue(rs.wasNull() ? "N/A" : String.format("%.1f", grade));
                }
            }
            autoSize(sheet, headers.length);
            writeFile(wb, filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportGradeBook(String filePath, int courseId) {
        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Grade Book");
            CellStyle headerStyle = createHeaderStyle(wb);

            String[] headers = {"Student Number", "Student Name", "Course", "Grade", "Letter Grade", "Status"};
            createHeaderRow(sheet, headers, headerStyle);

            int rowNum = 1;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT s.student_number, u.full_name, c.course_name, e.grade, e.status " +
                         "FROM enrollments e " +
                         "JOIN students s ON e.student_id=s.student_id " +
                         "JOIN users u ON s.user_id=u.user_id " +
                         "JOIN courses c ON e.course_id=c.course_id " +
                         "WHERE e.course_id=?")) {
                pstmt.setInt(1, courseId);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(rs.getString("student_number"));
                    row.createCell(1).setCellValue(rs.getString("full_name"));
                    row.createCell(2).setCellValue(rs.getString("course_name"));
                    double grade = rs.getDouble("grade");
                    if (rs.wasNull()) {
                        row.createCell(3).setCellValue("N/A");
                        row.createCell(4).setCellValue("N/A");
                    } else {
                        row.createCell(3).setCellValue(grade);
                        row.createCell(4).setCellValue(getLetterGrade(grade));
                    }
                    row.createCell(5).setCellValue(rs.getString("status"));
                }
            }
            autoSize(sheet, headers.length);
            writeFile(wb, filePath);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void createHeaderRow(Sheet sheet, String[] headers, CellStyle style) {
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            org.apache.poi.ss.usermodel.Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) sheet.autoSizeColumn(i);
    }

    private void writeFile(Workbook wb, String filePath) throws Exception {
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            wb.write(out);
        }
    }

    private String getLetterGrade(double grade) {
        if (grade >= 90) return "A";
        if (grade >= 80) return "B";
        if (grade >= 70) return "C";
        if (grade >= 60) return "D";
        return "F";
    }
}
