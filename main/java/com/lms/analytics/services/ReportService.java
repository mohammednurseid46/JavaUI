package com.lms.analytics.services;



import com.lms.analytics.utils.DatabaseConnection;
import javafx.collections.ObservableList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.properties.UnitValue;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ==================== PDF REPORT GENERATION ====================

    public boolean generateAccreditationReport(String filePath) {
        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Title
            document.add(new Paragraph("ACCREDITATION REPORT")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FORMATTER))
                    .setFontSize(10)
                    .setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph(" "));

            // Course Statistics
            document.add(new Paragraph("Course Statistics").setFontSize(14).setBold());
            String courseStats = getCourseStatistics();
            document.add(new Paragraph(courseStats));

            document.add(new Paragraph(" "));

            // Enrollment Data Table
            document.add(new Paragraph("Enrollment Summary").setFontSize(14).setBold());
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2, 2}));
            table.useAllAvailableWidth();

            // Headers
            table.addCell(new Cell().add(new Paragraph("Course Code")));
            table.addCell(new Cell().add(new Paragraph("Course Name")));
            table.addCell(new Cell().add(new Paragraph("Enrolled")));
            table.addCell(new Cell().add(new Paragraph("Completion Rate")));

            // Data
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT c.course_code, c.course_name, c.enrolled_count, " +
                                 "ROUND(CAST(COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) AS FLOAT) / " +
                                 "NULLIF(COUNT(e.enrollment_id), 0) * 100, 2) as completion_rate " +
                                 "FROM courses c LEFT JOIN enrollments e ON c.course_id = e.course_id " +
                                 "GROUP BY c.course_id")) {

                while (rs.next()) {
                    table.addCell(new Cell().add(new Paragraph(rs.getString("course_code"))));
                    table.addCell(new Cell().add(new Paragraph(rs.getString("course_name"))));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(rs.getInt("enrolled_count")))));
                    String rate = rs.getString("completion_rate");
                    table.addCell(new Cell().add(new Paragraph((rate == null ? "0" : rate) + "%")));}
            }

            document.add(table);
            document.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean generateAcademicProgressReport(String filePath, int studentId) {
        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // Get student info
            String studentName = getStudentName(studentId);

            document.add(new Paragraph("ACADEMIC PROGRESS REPORT")
                    .setFontSize(18).setBold().setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("Student: " + studentName)
                    .setFontSize(12));
            document.add(new Paragraph("Student ID: " + studentId)
                    .setFontSize(12));
            document.add(new Paragraph("Report Date: " + LocalDateTime.now().format(DATE_FORMATTER))
                    .setFontSize(10));

            document.add(new Paragraph(" "));

            // Course Grades Table
            document.add(new Paragraph("Course Grades").setFontSize(14).setBold());
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 4, 2, 2}));
            table.useAllAvailableWidth();

            table.addCell(new Cell().add(new Paragraph("Course Code")));
            table.addCell(new Cell().add(new Paragraph("Course Name")));
            table.addCell(new Cell().add(new Paragraph("Grade")));
            table.addCell(new Cell().add(new Paragraph("Status")));

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT c.course_code, c.course_name, e.grade, e.status " +
                                 "FROM enrollments e JOIN courses c ON e.course_id = c.course_id " +
                                 "WHERE e.student_id = ?")) {

                pstmt.setInt(1, studentId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    table.addCell(new Cell().add(new Paragraph(rs.getString("course_code"))));
                    table.addCell(new Cell().add(new Paragraph(rs.getString("course_name"))));
                    double grade = rs.getDouble("grade");
                    String gradeText = rs.wasNull() ? "N/A" : String.format("%.1f", grade);
                    table.addCell(new Cell().add(new Paragraph(gradeText)));
                    table.addCell(new Cell().add(new Paragraph(rs.getString("status"))));
                }
            }

            document.add(table);

            // GPA Calculation
            double gpa = calculateGPA(studentId);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Overall GPA: " + String.format("%.2f", gpa))
                    .setFontSize(12).setBold());

            document.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean generateInstructorEvaluationReport(String filePath, int instructorId) {
        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            String instructorName = getInstructorName(instructorId);

            document.add(new Paragraph("INSTRUCTOR EVALUATION REPORT").setFontSize(18).setBold().setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("Instructor: " + instructorName)
                    .setFontSize(12));
            document.add(new Paragraph("Report Date: " + LocalDateTime.now().format(DATE_FORMATTER))
                    .setFontSize(10));

            document.add(new Paragraph(" "));

            // Course Performance
            document.add(new Paragraph("Course Performance").setFontSize(14).setBold());
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2, 2, 2}));
            table.useAllAvailableWidth();

            table.addCell(new Cell().add(new Paragraph("Course Code")));
            table.addCell(new Cell().add(new Paragraph("Course Name")));
            table.addCell(new Cell().add(new Paragraph("Enrolled")));
            table.addCell(new Cell().add(new Paragraph("Avg Grade")));
            table.addCell(new Cell().add(new Paragraph("Completion")));

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT c.course_code, c.course_name, c.enrolled_count, " +
                                 "ROUND(AVG(e.grade), 2) as avg_grade, " +
                                 "ROUND(CAST(COUNT(CASE WHEN e.status = 'COMPLETED' THEN 1 END) AS FLOAT) / " +
                                 "NULLIF(COUNT(e.enrollment_id), 0) * 100, 2) as completion_rate " +
                                 "FROM courses c LEFT JOIN enrollments e ON c.course_id = e.course_id " +
                                 "WHERE c.instructor_id = ? GROUP BY c.course_id")) {

                pstmt.setInt(1, instructorId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    table.addCell(new Cell().add(new Paragraph(rs.getString("course_code"))));
                    table.addCell(new Cell().add(new Paragraph(rs.getString("course_name"))));
                    table.addCell(new Cell().add(new Paragraph(String.valueOf(rs.getInt("enrolled_count")))));
                    String avgGrade = rs.getString("avg_grade");
                    table.addCell(new Cell().add(new Paragraph(avgGrade == null ? "N/A" : avgGrade)));
                    String rate = rs.getString("completion_rate");
                    table.addCell(new Cell().add(new Paragraph((rate == null ? "0" : rate) + "%")));
                }
            }

            document.add(table);
            document.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean generateFinancialSummaryReport(String filePath) {
        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            document.add(new Paragraph("FINANCIAL SUMMARY REPORT")
                    .setFontSize(18).setBold().setTextAlignment(com.itextpdf.layout.properties.TextAlignment.CENTER));

            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DATE_FORMATTER))
                    .setFontSize(10));

            document.add(new Paragraph(" "));

            // Revenue by Course
            document.add(new Paragraph("Revenue by Course").setFontSize(14).setBold());
            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2}));
            table.useAllAvailableWidth();

            table.addCell(new Cell().add(new Paragraph("Course Code")));
            table.addCell(new Cell().add(new Paragraph("Course Name")));table.addCell(new Cell().add(new Paragraph("Revenue")));

            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT c.course_code, c.course_name, " +
                                 "COALESCE(SUM(p.amount), 0) as revenue " +
                                 "FROM courses c LEFT JOIN enrollments e ON c.course_id = e.course_id " +
                                 "LEFT JOIN payments p ON e.enrollment_id = p.enrollment_id " +
                                 "GROUP BY c.course_id ORDER BY revenue DESC")) {

                while (rs.next()) {
                    table.addCell(new Cell().add(new Paragraph(rs.getString("course_code"))));
                    table.addCell(new Cell().add(new Paragraph(rs.getString("course_name"))));
                    table.addCell(new Cell().add(new Paragraph(String.format("$%.2f", rs.getDouble("revenue")))));
                }
            }

            document.add(table);

            // Total Revenue
            double totalRevenue = getTotalRevenue();
            document.add(new Paragraph(" "));
            document.add(new Paragraph("TOTAL REVENUE: $" + String.format("%.2f", totalRevenue))
                    .setFontSize(14).setBold());

            document.close();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== EXCEL REPORT GENERATION ====================

    public boolean exportGradeBookToExcel(String filePath, int courseId) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Grade Book");

            // Create header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Headers
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Student ID", "Student Name", "Course", "Grade", "Letter Grade", "Status"};
            for (int i = 0; i < headers.length; i++) {
                // ከዚህ በፊት የነበረው: Cell cell = headerRow.createCell(i);
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data
            int rowNum = 1;
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT s.student_number, u.full_name, c.course_name, e.grade, e.status " +
                                 "FROM enrollments e " +
                                 "JOIN students s ON e.student_id = s.student_id " +
                                 "JOIN users u ON s.user_id = u.user_id " +
                                 "JOIN courses c ON e.course_id = c.course_id " +
                                 "WHERE e.course_id = ?")) {

                pstmt.setInt(1, courseId);
                ResultSet rs = pstmt.executeQuery();

                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(rs.getString("student_number"));
                    row.createCell(1).setCellValue(rs.getString("full_name"));
                    row.createCell(2).setCellValue(rs.getString("course_name"));

                    double grade = rs.getDouble("grade");
                    if (rs.wasNull()) {row.createCell(3).setCellValue("N/A");
                        row.createCell(4).setCellValue("N/A");
                    } else {
                        row.createCell(3).setCellValue(grade);
                        row.createCell(4).setCellValue(getLetterGrade(grade));
                    }
                    row.createCell(5).setCellValue(rs.getString("status"));
                }
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportStudentReportToExcel(String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Student Report");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Student ID", "Student Number", "Name", "Email", "Major", "Enrollment Date", "Courses Enrolled"};
            for (int i = 0; i < headers.length; i++) {
                // ከዚህ በፊት የነበረው: Cell cell = headerRow.createCell(i);
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT s.student_id, s.student_number, u.full_name, u.email, " +
                                 "s.major, s.enrollment_date, " +
                                 "(SELECT COUNT(*) FROM enrollments e WHERE e.student_id = s.student_id) as course_count " +
                                 "FROM students s JOIN users u ON s.user_id = u.user_id")) {

                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(rs.getInt("student_id"));
                    row.createCell(1).setCellValue(rs.getString("student_number"));
                    row.createCell(2).setCellValue(rs.getString("full_name"));
                    row.createCell(3).setCellValue(rs.getString("email"));
                    row.createCell(4).setCellValue(rs.getString("major"));
                    row.createCell(5).setCellValue(rs.getString("enrollment_date"));
                    row.createCell(6).setCellValue(rs.getInt("course_count"));
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportCourseReportToExcel(String filePath) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Course Report");

            CellStyle headerStyle = workbook.createCellStyle();Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Course Code", "Course Name", "Credits", "Instructor", "Capacity", "Enrolled", "Status", "Revenue"};
            for (int i = 0; i < headers.length; i++) {
                // ከዚህ በፊት የነበረው: Cell cell = headerRow.createCell(i);
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT c.course_code, c.course_name, c.credits, u.full_name as instructor, " +
                                 "c.capacity, c.enrolled_count, c.status, " +
                                 "COALESCE(SUM(p.amount), 0) as revenue " +
                                 "FROM courses c " +
                                 "LEFT JOIN users u ON c.instructor_id = u.user_id " +
                                 "LEFT JOIN enrollments e ON c.course_id = e.course_id " +
                                 "LEFT JOIN payments p ON e.enrollment_id = p.enrollment_id " +
                                 "GROUP BY c.course_id")) {

                while (rs.next()) {
                    Row row = sheet.createRow(rowNum++);
                    row.createCell(0).setCellValue(rs.getString("course_code"));
                    row.createCell(1).setCellValue(rs.getString("course_name"));
                    row.createCell(2).setCellValue(rs.getInt("credits"));
                    row.createCell(3).setCellValue(rs.getString("instructor"));
                    row.createCell(4).setCellValue(rs.getInt("capacity"));
                    row.createCell(5).setCellValue(rs.getInt("enrolled_count"));
                    row.createCell(6).setCellValue(rs.getString("status"));
                    row.createCell(7).setCellValue(rs.getDouble("revenue"));
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
                workbook.write(fileOut);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== WORD CERTIFICATE GENERATION ====================

    public boolean generateCertificateToWord(String filePath, int studentId, int courseId) {
        try (XWPFDocument document = new XWPFDocument()) {

            // Get student and course info
            String studentName = getStudentName(studentId);
            String courseName = getCourseName(courseId);
            String completionDate = getCompletionDate(studentId, courseId);

            // Certificate Title
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("CERTIFICATE OF COMPLETION");
            titleRun.setFontSize(24);
            titleRun.setBold(true);
            titleRun.setFontFamily("Times New Roman");

            // Subtitle
            XWPFParagraph subtitlePara = document.createParagraph();
            subtitlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun subtitleRun = subtitlePara.createRun();subtitleRun.setText("This certificate is proudly presented to");
            subtitleRun.setFontSize(16);
            subtitleRun.setFontFamily("Times New Roman");

            // Student Name
            XWPFParagraph namePara = document.createParagraph();
            namePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun nameRun = namePara.createRun();
            nameRun.setText(studentName);
            nameRun.setFontSize(28);
            nameRun.setBold(true);
            nameRun.setFontFamily("Times New Roman");
            nameRun.addBreak();

            // Description
            XWPFParagraph descPara = document.createParagraph();
            descPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun descRun = descPara.createRun();
            descRun.setText("for successfully completing the course");
            descRun.setFontSize(14);
            descRun.setFontFamily("Times New Roman");
            descRun.addBreak();

            // Course Name
            XWPFParagraph coursePara = document.createParagraph();
            coursePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun courseRun = coursePara.createRun();
            courseRun.setText(courseName);
            courseRun.setFontSize(20);
            courseRun.setBold(true);
            courseRun.setFontFamily("Times New Roman");
            courseRun.addBreak();

            // Completion Date
            XWPFParagraph datePara = document.createParagraph();
            datePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun dateRun = datePara.createRun();
            dateRun.setText("Completion Date: " + completionDate);
            dateRun.setFontSize(12);
            dateRun.setFontFamily("Times New Roman");

            // Save document
            try (FileOutputStream out = new FileOutputStream(filePath)) {
                document.write(out);
            }

            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== HELPER METHODS ====================

    private String getCourseStatistics() {
        StringBuilder stats = new StringBuilder();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as total FROM courses");
            if (rs.next()) {
                stats.append("Total Courses: ").append(rs.getInt("total")).append("\n");
            }

            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM students");
            if (rs.next()) {
                stats.append("Total Students: ").append(rs.getInt("total")).append("\n");
            }

            rs = stmt.executeQuery("SELECT COUNT(*) as total FROM enrollments");
            if (rs.next()) {
                stats.append("Total Enrollments: ").append(rs.getInt("total")).append("\n");
            }

            rs = stmt.executeQuery("SELECT ROUND(AVG(grade), 2) as avg FROM enrollments WHERE grade IS NOT NULL");
            if (rs.next()) {
                stats.append("Average Grade: ").append(rs.getString("avg")).append("%\n");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stats.toString();
    }

    private String getStudentName(int studentId) {
        // Try students table first
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT u.full_name FROM students s JOIN users u ON s.user_id = u.user_id WHERE s.student_id = ?")) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (SQLException e) { e.printStackTrace(); }
        // Fallback: try user_id directly
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT full_name FROM users WHERE user_id = ?")) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (SQLException e) { e.printStackTrace(); }
        return "Student #" + studentId;
    }

    private String getInstructorName(int instructorId) {
        // instructors are stored in users table — query by user_id directly
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT full_name FROM users WHERE user_id = ? AND role = 'INSTRUCTOR'")) {
            pstmt.setInt(1, instructorId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        // Fallback: try instructors table
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT u.full_name FROM users u WHERE u.user_id = ?")) {
            pstmt.setInt(1, instructorId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) return rs.getString("full_name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Instructor #" + instructorId;
    }

    private String getCourseName(int courseId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT course_name FROM courses WHERE course_id = ?")) {
            pstmt.setInt(1, courseId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("course_name");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Unknown Course";
    }

    private String getCompletionDate(int studentId, int courseId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT completion_date FROM enrollments WHERE student_id = ? AND course_id = ?")) {
            pstmt.setInt(1, studentId);
            pstmt.setInt(2, courseId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next() && rs.getDate("completion_date") != null) {
                return rs.getDate("completion_date").toString();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    private double calculateGPA(int studentId) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(
                     "SELECT AVG(grade) as avg FROM enrollments WHERE student_id = ? AND grade IS NOT NULL")) {
            pstmt.setInt(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getDouble("avg");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private double getTotalRevenue() {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COALESCE(SUM(amount), 0) FROM payments WHERE status = 'COMPLETED'")) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    private String getLetterGrade(double grade) {
        if (grade >= 90) return "A";
        if (grade >= 80) return "B";
        if (grade >= 70) return "C";
        if (grade >= 60) return "D";
        return "F";
    }
}