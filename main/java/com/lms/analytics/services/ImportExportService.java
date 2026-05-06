package com.lms.analytics.services;

import com.lms.analytics.utils.CSVImporter;
import com.lms.analytics.utils.ExcelExporter;
import com.lms.analytics.utils.PDFExporter;

import java.util.List;
import java.util.Map;

public class ImportExportService {

    private final CSVImporter csvImporter;
    private final ExcelExporter excelExporter;
    private final PDFExporter pdfExporter;

    public ImportExportService() {
        this.csvImporter = new CSVImporter();
        this.excelExporter = new ExcelExporter();
        this.pdfExporter = new PDFExporter();
    }

    // ==================== IMPORT ====================

    public ImportResult importStudentsFromCSV(String filePath) {
        return csvImporter.importStudents(filePath);
    }

    public ImportResult importCoursesFromCSV(String filePath) {
        return csvImporter.importCourses(filePath);
    }

    public ImportResult importEnrollmentsFromCSV(String filePath) {
        return csvImporter.importEnrollments(filePath);
    }

    // ==================== EXPORT ====================

    public boolean exportStudentsToExcel(String filePath) {
        return excelExporter.exportStudents(filePath);
    }

    public boolean exportCoursesToExcel(String filePath) {
        return excelExporter.exportCourses(filePath);
    }

    public boolean exportEnrollmentsToExcel(String filePath) {
        return excelExporter.exportEnrollments(filePath);
    }

    public boolean exportGradeBookToExcel(String filePath, int courseId) {
        return excelExporter.exportGradeBook(filePath, courseId);
    }

    public boolean exportReportToPDF(String filePath, String reportType, Map<String, Object> params) {
        return pdfExporter.exportReport(filePath, reportType, params);
    }

    public boolean exportStudentsToCSV(String filePath) {
        return csvImporter.exportStudentsToCSV(filePath);
    }

    // ==================== RESULT CLASS ====================

    public static class ImportResult {
        private int successCount;
        private int failureCount;
        private List<String> errors;

        public ImportResult(int successCount, int failureCount, List<String> errors) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.errors = errors;
        }

        public int getSuccessCount() { return successCount; }
        public int getFailureCount() { return failureCount; }
        public List<String> getErrors() { return errors; }
        public boolean hasErrors() { return failureCount > 0; }

        @Override
        public String toString() {
            return String.format("Imported: %d success, %d failed", successCount, failureCount);
        }
    }
}
