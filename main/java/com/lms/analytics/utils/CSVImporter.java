package com.lms.analytics.utils;

import com.lms.analytics.services.ImportExportService.ImportResult;
import com.lms.analytics.utils.DatabaseConnection;

import java.io.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CSVImporter {

    // ==================== IMPORT ====================

    public ImportResult importStudents(String filePath) {
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                if (firstLine) { firstLine = false; continue; } // skip header
                String[] cols = parseCsvLine(line);
                if (cols.length < 5) {
                    errors.add("Line " + lineNum + ": insufficient columns");
                    fail++;
                    continue;
                }
                try {
                    // Expected: username, full_name, email, student_number, major
                    String username = cols[0].trim();
                    String fullName = cols[1].trim();
                    String email    = cols[2].trim();
                    String stuNum   = cols[3].trim();
                    String major    = cols[4].trim();

                    String hashedPw = PasswordEncryptionUtil.hashPassword("Student@123");

                    try (Connection conn = DatabaseConnection.getConnection()) {
                        conn.setAutoCommit(false);
                        try {
                            PreparedStatement userStmt = conn.prepareStatement(
                                    "INSERT INTO users (username, password_hash, email, full_name, role) VALUES (?,?,?,?,'STUDENT')",
                                    Statement.RETURN_GENERATED_KEYS);
                            userStmt.setString(1, username);
                            userStmt.setString(2, hashedPw);
                            userStmt.setString(3, email);
                            userStmt.setString(4, fullName);
                            userStmt.executeUpdate();

                            ResultSet keys = userStmt.getGeneratedKeys();
                            int userId = keys.next() ? keys.getInt(1) : -1;

                            PreparedStatement stuStmt = conn.prepareStatement(
                                    "INSERT INTO students (user_id, student_number, major, enrollment_date, current_semester) VALUES (?,?,?,date('now'),1)");
                            stuStmt.setInt(1, userId);
                            stuStmt.setString(2, stuNum);
                            stuStmt.setString(3, major);
                            stuStmt.executeUpdate();

                            conn.commit();
                            success++;
                        } catch (SQLException ex) {
                            conn.rollback();
                            errors.add("Line " + lineNum + ": " + ex.getMessage());
                            fail++;
                        } finally {
                            conn.setAutoCommit(true);
                        }
                    }
                } catch (Exception ex) {
                    errors.add("Line " + lineNum + ": " + ex.getMessage());
                    fail++;
                }
            }
        } catch (IOException e) {
            errors.add("Cannot read file: " + e.getMessage());
        }
        return new ImportResult(success, fail, errors);
    }

    public ImportResult importCourses(String filePath) {
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                if (firstLine) { firstLine = false; continue; }
                String[] cols = parseCsvLine(line);
                if (cols.length < 5) {
                    errors.add("Line " + lineNum + ": insufficient columns");
                    fail++;
                    continue;
                }
                try {
                    // Expected: course_code, course_name, credits, capacity, price
                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(
                                 "INSERT INTO courses (course_code, course_name, credits, capacity, price, status) VALUES (?,?,?,?,?,'ACTIVE')")) {
                        pstmt.setString(1, cols[0].trim());
                        pstmt.setString(2, cols[1].trim());
                        pstmt.setInt(3, Integer.parseInt(cols[2].trim()));
                        pstmt.setInt(4, Integer.parseInt(cols[3].trim()));
                        pstmt.setDouble(5, Double.parseDouble(cols[4].trim()));
                        pstmt.executeUpdate();
                        success++;
                    }
                } catch (Exception ex) {
                    errors.add("Line " + lineNum + ": " + ex.getMessage());
                    fail++;
                }
            }
        } catch (IOException e) {
            errors.add("Cannot read file: " + e.getMessage());
        }
        return new ImportResult(success, fail, errors);
    }

    public ImportResult importEnrollments(String filePath) {
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean firstLine = true;
            int lineNum = 0;

            while ((line = br.readLine()) != null) {
                lineNum++;
                if (firstLine) { firstLine = false; continue; }
                String[] cols = parseCsvLine(line);
                if (cols.length < 2) {
                    errors.add("Line " + lineNum + ": insufficient columns");
                    fail++;
                    continue;
                }
                try {
                    // Expected: student_id, course_id
                    int studentId = Integer.parseInt(cols[0].trim());
                    int courseId  = Integer.parseInt(cols[1].trim());

                    try (Connection conn = DatabaseConnection.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(
                                 "INSERT OR IGNORE INTO enrollments (student_id, course_id, enrollment_date, status) VALUES (?,?,date('now'),'ENROLLED')")) {
                        pstmt.setInt(1, studentId);
                        pstmt.setInt(2, courseId);
                        pstmt.executeUpdate();
                        success++;
                    }
                } catch (Exception ex) {
                    errors.add("Line " + lineNum + ": " + ex.getMessage());
                    fail++;
                }
            }
        } catch (IOException e) {
            errors.add("Cannot read file: " + e.getMessage());
        }
        return new ImportResult(success, fail, errors);
    }

    // ==================== EXPORT ====================

    public boolean exportStudentsToCSV(String filePath) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filePath))) {
            pw.println("student_id,student_number,full_name,email,major,semester,enrollment_date");

            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT s.student_id, s.student_number, u.full_name, u.email, " +
                         "s.major, s.current_semester, s.enrollment_date " +
                         "FROM students s JOIN users u ON s.user_id=u.user_id ORDER BY u.full_name")) {
                while (rs.next()) {
                    pw.printf("%d,%s,%s,%s,%s,%d,%s%n",
                            rs.getInt("student_id"),
                            escapeCsv(rs.getString("student_number")),
                            escapeCsv(rs.getString("full_name")),
                            escapeCsv(rs.getString("email")),
                            escapeCsv(rs.getString("major")),
                            rs.getInt("current_semester"),
                            rs.getString("enrollment_date"));
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ==================== HELPERS ====================

    private String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString());
        return tokens.toArray(new String[0]);
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
