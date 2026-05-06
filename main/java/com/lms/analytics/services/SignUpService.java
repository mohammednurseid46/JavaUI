package com.lms.analytics.services;

import com.lms.analytics.dao.StudentDAO;
import com.lms.analytics.dao.UserDAO;
import com.lms.analytics.models.Student;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.DatabaseConnection;
import com.lms.analytics.utils.PasswordEncryptionUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;

public class SignUpService {

    private final UserDAO userDAO = new UserDAO();
    private final StudentDAO studentDAO = new StudentDAO();

    public enum RegisterResult {
        SUCCESS,
        USERNAME_TAKEN,
        EMAIL_TAKEN,
        INVALID_EMAIL,
        WEAK_PASSWORD,
        PASSWORDS_MISMATCH,
        MISSING_FIELDS,
        ERROR
    }

    /**
     * Registers a new student account.
     * Creates a user row (role=STUDENT) and a linked students row.
     */
    public RegisterResult registerStudent(String fullName, String username,
                                          String email, String password,
                                          String confirmPassword, String major) {
        // Validate fields
        if (fullName.isBlank() || username.isBlank() || email.isBlank()
                || password.isBlank() || confirmPassword.isBlank()) {
            return RegisterResult.MISSING_FIELDS;
        }
        // Validate email format
        if (!PasswordEncryptionUtil.isValidEmail(email)) {
            return RegisterResult.INVALID_EMAIL;
        }
        if (!password.equals(confirmPassword)) {
            return RegisterResult.PASSWORDS_MISMATCH;
        }
        if (!PasswordEncryptionUtil.isStrongPassword(password)) {
            return RegisterResult.WEAK_PASSWORD;
        }
        if (userDAO.getUserByUsername(username) != null) {
            return RegisterResult.USERNAME_TAKEN;
        }
        if (isEmailTaken(email)) {
            return RegisterResult.EMAIL_TAKEN;
        }

        // Create user
        User user = new User(username,
                PasswordEncryptionUtil.hashPassword(password),
                email, fullName, "STUDENT");
        user.setPlainPassword(password);   // store plain text for admin visibility
        user.setActive(true);

        if (!userDAO.createUser(user)) {
            return RegisterResult.ERROR;
        }

        // Create student profile
        String studentNumber = generateStudentNumber();
        Student student = new Student(user.getUserId(), studentNumber,
                major.isBlank() ? "Undeclared" : major);
        student.setEnrollmentDate(LocalDate.now());
        student.setCurrentSemester(1);

        if (!studentDAO.createStudent(student)) {
            // Rollback user creation
            userDAO.hardDeleteUser(user.getUserId());
            return RegisterResult.ERROR;
        }

        return RegisterResult.SUCCESS;
    }

    private boolean isEmailTaken(String email) {
        String sql = "SELECT COUNT(*) FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private String generateStudentNumber() {
        // Use MAX student_number to avoid collisions from concurrent registrations
        String sql = "SELECT COUNT(*), MAX(CAST(SUBSTR(student_number, 4) AS INTEGER)) FROM students";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int maxNum = rs.getInt(2); // MAX of numeric part
                int count  = rs.getInt(1);
                int next   = Math.max(maxNum, count) + 1;
                return String.format("STU%05d", next);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "STU" + String.format("%05d", System.currentTimeMillis() % 100000);
    }

    public String getPasswordRequirements() {
        return "Min 8 chars · Uppercase · Lowercase · Number · Special char";
    }
}
