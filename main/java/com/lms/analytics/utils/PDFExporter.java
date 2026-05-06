package com.lms.analytics.utils;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.lms.analytics.utils.DatabaseConnection;

import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class PDFExporter {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public boolean exportReport(String filePath, String reportType, Map<String, Object> params) {
        switch (reportType) {
            case "ACCREDITATION": return exportAccreditationReport(filePath);
            case "FINANCIAL":     return exportFinancialReport(filePath);
            case "ENROLLMENT":    return exportEnrollmentReport(filePath);
            default:              return exportGenericReport(filePath, reportType);
        }
    }

    public boolean exportAccreditationReport(String filePath) {
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            addTitle(doc, "ACCREDITATION REPORT");
            addTimestamp(doc);

            doc.add(new Paragraph("Course Statistics").setFontSize(14).setBold());
            doc.add(new Paragraph(getCourseStats()));

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Enrollment Summary").setFontSize(14).setBold());

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 4, 2, 2}));
            table.useAllAvailableWidth();
            addHeaderCell(table, "Course Code");
            addHeaderCell(table, "Course Name");
            addHeaderCell(table, "Enrolled");
            addHeaderCell(table, "Completion %");

            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT c.course_code, c.course_name, c.enrolled_count, " +
                         "ROUND(CAST(COUNT(CASE WHEN e.status='COMPLETED' THEN 1 END) AS FLOAT) / " +
                         "NULLIF(COUNT(e.enrollment_id),0)*100,2) as cr " +
                         "FROM courses c LEFT JOIN enrollments e ON c.course_id=e.course_id GROUP BY c.course_id")) {
                while (rs.next()) {
                    table.addCell(rs.getString("course_code"));
                    table.addCell(rs.getString("course_name"));
                    table.addCell(String.valueOf(rs.getInt("enrolled_count")));
                    String cr = rs.getString("cr");
                    table.addCell((cr == null ? "0" : cr) + "%");
                }
            }
            doc.add(table);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportFinancialReport(String filePath) {
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            addTitle(doc, "FINANCIAL SUMMARY REPORT");
            addTimestamp(doc);

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 4, 2}));
            table.useAllAvailableWidth();
            addHeaderCell(table, "Course Code");
            addHeaderCell(table, "Course Name");
            addHeaderCell(table, "Revenue");

            double total = 0;
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT c.course_code, c.course_name, COALESCE(SUM(p.amount),0) as rev " +
                         "FROM courses c LEFT JOIN enrollments e ON c.course_id=e.course_id " +
                         "LEFT JOIN payments p ON e.enrollment_id=p.enrollment_id " +
                         "GROUP BY c.course_id ORDER BY rev DESC")) {
                while (rs.next()) {
                    double rev = rs.getDouble("rev");
                    total += rev;
                    table.addCell(rs.getString("course_code"));
                    table.addCell(rs.getString("course_name"));
                    table.addCell(String.format("$%.2f", rev));
                }
            }
            doc.add(table);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("TOTAL REVENUE: $" + String.format("%.2f", total)).setFontSize(14).setBold());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exportEnrollmentReport(String filePath) {
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {

            addTitle(doc, "ENROLLMENT REPORT");
            addTimestamp(doc);

            Table table = new Table(UnitValue.createPercentArray(new float[]{3, 3, 2, 2}));
            table.useAllAvailableWidth();
            addHeaderCell(table, "Student");
            addHeaderCell(table, "Course");
            addHeaderCell(table, "Date");
            addHeaderCell(table, "Status");

            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT u.full_name, c.course_name, e.enrollment_date, e.status " +
                         "FROM enrollments e " +
                         "JOIN students s ON e.student_id=s.student_id " +
                         "JOIN users u ON s.user_id=u.user_id " +
                         "JOIN courses c ON e.course_id=c.course_id " +
                         "ORDER BY e.enrollment_date DESC")) {
                while (rs.next()) {
                    table.addCell(rs.getString("full_name"));
                    table.addCell(rs.getString("course_name"));
                    table.addCell(rs.getString("enrollment_date"));
                    table.addCell(rs.getString("status"));
                }
            }
            doc.add(table);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean exportGenericReport(String filePath, String title) {
        try (PdfWriter writer = new PdfWriter(filePath);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf)) {
            addTitle(doc, title);
            addTimestamp(doc);
            doc.add(new Paragraph("No data available for this report type."));
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void addTitle(Document doc, String title) {
        doc.add(new Paragraph(title).setFontSize(18).setBold().setTextAlignment(TextAlignment.CENTER));
    }

    private void addTimestamp(Document doc) {
        doc.add(new Paragraph("Generated: " + LocalDateTime.now().format(FMT))
                .setFontSize(10).setTextAlignment(TextAlignment.CENTER));
        doc.add(new Paragraph(" "));
    }

    private void addHeaderCell(Table table, String text) {
        table.addCell(new Cell().add(new Paragraph(text).setBold()));
    }

    private String getCourseStats() {
        StringBuilder sb = new StringBuilder();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM courses");
            if (rs.next()) sb.append("Total Courses: ").append(rs.getInt(1)).append("\n");
            rs = stmt.executeQuery("SELECT COUNT(*) FROM students");
            if (rs.next()) sb.append("Total Students: ").append(rs.getInt(1)).append("\n");
            rs = stmt.executeQuery("SELECT COUNT(*) FROM enrollments");
            if (rs.next()) sb.append("Total Enrollments: ").append(rs.getInt(1)).append("\n");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}
