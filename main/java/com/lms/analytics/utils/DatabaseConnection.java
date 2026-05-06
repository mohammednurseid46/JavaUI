package com.lms.analytics.utils;

import java.sql.*;
import java.io.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DatabaseConnection {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static Connection connection = null;
    private static String databaseUrl;

    static {
        loadConfiguration();
    }

    private static void loadConfiguration() {
        Properties props = new Properties();
        try (InputStream input = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("config/database.properties")) {

            if (input == null) {
                LOGGER.warning("database.properties not found, using default SQLite configuration");
                databaseUrl = "jdbc:sqlite:lms_database.db";
            } else {
                props.load(input);
                databaseUrl = props.getProperty("db.url", "jdbc:sqlite:lms_database.db");
                String driver = props.getProperty("db.driver", "org.sqlite.JDBC");
                Class.forName(driver);
            }
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load database configuration", e);
            databaseUrl = "jdbc:sqlite:lms_database.db";
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(databaseUrl);
                LOGGER.info("Database connection established successfully");
            }
            return connection;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to connect to database", e);
            throw new SQLException("Cannot connect to database: " + e.getMessage());
        }
    }

    public static void initializeDatabase() {
        String schema = """
            CREATE TABLE IF NOT EXISTS users (
                user_id INTEGER PRIMARY KEY AUTOINCREMENT,
                username VARCHAR(50) UNIQUE NOT NULL,
                password_hash VARCHAR(255) NOT NULL,
                email VARCHAR(100) NOT NULL,
                full_name VARCHAR(100) NOT NULL,
                role VARCHAR(20) DEFAULT 'STUDENT',
                is_active BOOLEAN DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_login TIMESTAMP
            );
            
            CREATE TABLE IF NOT EXISTS courses (
                course_id INTEGER PRIMARY KEY AUTOINCREMENT,
                course_code VARCHAR(20) UNIQUE NOT NULL,
                course_name VARCHAR(200) NOT NULL,
                description TEXT,
                credits INTEGER DEFAULT 3,
                instructor_id INTEGER,
                capacity INTEGER DEFAULT 30,
                enrolled_count INTEGER DEFAULT 0,
                price DECIMAL(10,2),
                status VARCHAR(20) DEFAULT 'ACTIVE',
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (instructor_id) REFERENCES users(user_id)
            );
            
            CREATE TABLE IF NOT EXISTS students (
                student_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER UNIQUE NOT NULL,
                student_number VARCHAR(50) UNIQUE NOT NULL,
                date_of_birth DATE,
                phone VARCHAR(20),
                address TEXT,
                enrollment_date DATE,
                current_semester INTEGER DEFAULT 1,
                major VARCHAR(100),
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            );
            
            CREATE TABLE IF NOT EXISTS instructors (instructor_id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER UNIQUE NOT NULL,
                employee_number VARCHAR(50) UNIQUE NOT NULL,
                department VARCHAR(100),
                office_location VARCHAR(100),
                hire_date DATE,
                specialization VARCHAR(200),
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            );
            
            CREATE TABLE IF NOT EXISTS enrollments (
                enrollment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                course_id INTEGER NOT NULL,
                enrollment_date DATE DEFAULT CURRENT_DATE,
                status VARCHAR(20) DEFAULT 'ENROLLED',
                grade DECIMAL(5,2),
                completion_date DATE,
                FOREIGN KEY (student_id) REFERENCES students(student_id),
                FOREIGN KEY (course_id) REFERENCES courses(course_id),
                UNIQUE(student_id, course_id)
            );
            
            CREATE TABLE IF NOT EXISTS grades (
                grade_id INTEGER PRIMARY KEY AUTOINCREMENT,
                enrollment_id INTEGER NOT NULL,
                assignment_name VARCHAR(100),
                assignment_type VARCHAR(30),
                score DECIMAL(5,2),
                max_score DECIMAL(5,2),
                weight DECIMAL(5,2),
                graded_date DATE,
                comments TEXT,
                FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
            );
            
            CREATE TABLE IF NOT EXISTS payments (
                payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
                student_id INTEGER NOT NULL,
                enrollment_id INTEGER,
                amount DECIMAL(10,2) NOT NULL,
                payment_date DATE DEFAULT CURRENT_DATE,
                payment_method VARCHAR(50),
                transaction_id VARCHAR(100),
                status VARCHAR(20) DEFAULT 'COMPLETED',
                FOREIGN KEY (student_id) REFERENCES students(student_id),
                FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
            );
            
            CREATE TABLE IF NOT EXISTS course_evaluations (
                evaluation_id INTEGER PRIMARY KEY AUTOINCREMENT,
                course_id INTEGER NOT NULL,
                student_id INTEGER NOT NULL,
                rating INTEGER CHECK(rating >= 1 AND rating <= 5),
                feedback TEXT,
                submitted_date DATE DEFAULT CURRENT_DATE,
                FOREIGN KEY (course_id) REFERENCES courses(course_id),
                FOREIGN KEY (student_id) REFERENCES students(student_id)
            );

            CREATE TABLE IF NOT EXISTS password_history (
                history_id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id            INTEGER NOT NULL,
                password_hash      VARCHAR(255) NOT NULL,
                changed_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                changed_by_user_id INTEGER DEFAULT 0,
                change_reason      VARCHAR(50) DEFAULT 'SELF_CHANGE',
                is_current         BOOLEAN DEFAULT 1,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
            );
        """;

        try (Statement stmt = getConnection().createStatement()) {
            // Execute each statement separately
            for (String sql : schema.split(";")) {
                if (!sql.trim().isEmpty()) {
                    stmt.execute(sql.trim());
                }
            }
            LOGGER.info("Database initialized successfully");

            // Add plain_password column if it doesn't exist (for admin visibility)
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN plain_password VARCHAR(255)");
                LOGGER.info("Added plain_password column to users table");
            } catch (SQLException ignored) {
                // Column already exists — ignore
            }

            // Backfill plain_password for existing users using known default passwords
            backfillPlainPasswords();

            // Check if admin user exists, if not create default
            createDefaultAdmin();

            // Seed password_history for ALL existing users who have no entry yet
            // (covers admin, all instructors, all students already in the DB)
            backfillPasswordHistory();

            // Repair enrollments where student_id doesn't match students table
            repairEnrollmentStudentIds();

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
        }
    }

    /**
     * Backfills plain_password for existing users who have NULL plain_password.
     * Tries default passwords: Student@123 for STUDENT, Instructor@123 for INSTRUCTOR.
     * If the hash matches, stores the plain text.
     */
    private static void backfillPlainPasswords() {
        String selectSql =
            "SELECT user_id, role, password_hash FROM users " +
            "WHERE (plain_password IS NULL OR plain_password = '') AND role != 'ADMIN'";
        String updateSql = "UPDATE users SET plain_password = ? WHERE user_id = ?";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String role = rs.getString("role");
                String hash = rs.getString("password_hash");

                // Try the default password for this role
                String defaultPass = "INSTRUCTOR".equals(role) ? "Instructor@123" : "Student@123";

                if (hash != null && PasswordEncryptionUtil.verifyPassword(defaultPass, hash)) {
                    try (PreparedStatement upd = getConnection().prepareStatement(updateSql)) {
                        upd.setString(1, defaultPass);
                        upd.setInt(2, userId);
                        upd.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(java.util.logging.Level.WARNING, "Could not backfill plain passwords", e);
        }
    }

    private static void createDefaultAdmin() {
        String checkAdmin = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(checkAdmin)) {

            if (rs.next() && rs.getInt(1) == 0) {
                String hashedPassword = PasswordEncryptionUtil.hashPassword("Admin@123");
                String insertAdmin =
                    "INSERT INTO users (username, password_hash, plain_password, email, full_name, role) " +
                    "VALUES ('admin', '" + hashedPassword + "', 'Admin@123', " +
                    "'admin@lms.com', 'System Administrator', 'ADMIN')";
                stmt.execute(insertAdmin);
                LOGGER.info("Default admin user created (username: admin, password: Admin@123)");

                // Record admin's initial password in history
                try (PreparedStatement ps = getConnection().prepareStatement(
                        "SELECT user_id FROM users WHERE username = 'admin'")) {
                    ResultSet adminRs = ps.executeQuery();
                    if (adminRs.next()) {
                        int adminId = adminRs.getInt("user_id");
                        recordInitialPasswordHistory(adminId, hashedPassword, "SIGNUP");
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not verify/create admin user", e);
        }
    }

    /**
     * Seeds password_history for ALL existing users who have no history entry yet.
     * Safe to call on every startup — only inserts for users with no existing rows.
     * Covers: admin, all instructors, all students already in the DB.
     */
    private static void backfillPasswordHistory() {
        String findMissing =
            "SELECT user_id, password_hash FROM users " +
            "WHERE user_id NOT IN (SELECT DISTINCT user_id FROM password_history)";

        try (Statement stmt = getConnection().createStatement();
             ResultSet rs = stmt.executeQuery(findMissing)) {

            int count = 0;
            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String hash = rs.getString("password_hash");
                if (hash != null && !hash.isBlank()) {
                    recordInitialPasswordHistory(userId, hash, "SIGNUP");
                    count++;
                }
            }
            if (count > 0) {
                LOGGER.info("Backfilled password_history for " + count + " existing user(s)");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not backfill password history", e);
        }
    }

    /**
     * Inserts a single SIGNUP entry into password_history for a user.
     * Marks all previous entries as not-current first (safety), then inserts is_current=1.
     */
    private static void recordInitialPasswordHistory(int userId, String passwordHash,
                                                      String reason) {
        try (Connection conn = getConnection()) {
            // Mark any existing entries as not-current
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE password_history SET is_current = 0 WHERE user_id = ?")) {
                ps.setInt(1, userId);
                ps.executeUpdate();
            }
            // Insert the initial entry
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO password_history " +
                    "(user_id, password_hash, changed_at, changed_by_user_id, change_reason, is_current) " +
                    "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, 1)")) {
                ps.setInt(1, userId);
                ps.setString(2, passwordHash);
                ps.setInt(3, userId);   // changed_by = self (signup)
                ps.setString(4, reason);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING,
                "Could not record initial password history for user " + userId, e);
        }
    }

    /**
     * Repairs enrollments where student_id is 0 or doesn't match students table.
     *
     * Case 1: enrollment.student_id = 0  → find the student by looking at who
     *         enrolled around that time (best-effort: assign to first STUDENT user
     *         who has a students row).
     *
     * Case 2: enrollment.student_id matches users.user_id directly (not students.student_id)
     *         → update to the correct students.student_id.
     */
    private static void repairEnrollmentStudentIds() {
        try (Statement stmt = getConnection().createStatement()) {

            // Fix Case 2: enrollment.student_id = users.user_id (not students.student_id)
            // Find enrollments where student_id doesn't exist in students table
            // but DOES exist as a user_id in students table
            String fixSql =
                "UPDATE enrollments " +
                "SET student_id = (" +
                "  SELECT s.student_id FROM students s " +
                "  WHERE s.user_id = enrollments.student_id " +
                "  LIMIT 1 " +
                ") " +
                "WHERE student_id NOT IN (SELECT student_id FROM students) " +
                "AND student_id IN (SELECT user_id FROM students)";

            int fixed = stmt.executeUpdate(fixSql);
            if (fixed > 0) {
                LOGGER.info("Repaired " + fixed + " enrollment(s) with mismatched student_id");
            }

            // Fix Case 1: enrollment.student_id = 0 → try to find correct student
            // For each zero-id enrollment, look up the course and find a student
            // who should be enrolled (this is a best-effort repair)
            String zeroCheck = "SELECT COUNT(*) FROM enrollments WHERE student_id = 0";
            ResultSet rs = stmt.executeQuery(zeroCheck);
            if (rs.next() && rs.getInt(1) > 0) {
                LOGGER.warning("Found " + rs.getInt(1) +
                    " enrollment(s) with student_id=0. These need manual correction.");
            }

        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not repair enrollment student IDs", e);
        }
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                LOGGER.info("Database connection closed");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error closing database connection", e);
        }
    }

    public static void beginTransaction() throws SQLException {
        getConnection().setAutoCommit(false);
    }

    public static void commitTransaction() throws SQLException {
        getConnection().commit();
        getConnection().setAutoCommit(true);
    }

    public static void rollbackTransaction() {
        try {
            getConnection().rollback();
            getConnection().setAutoCommit(true);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error rolling back transaction", e);
        }
    }

    public static boolean testConnection() {
        try (Statement stmt = getConnection().createStatement()) {
            stmt.executeQuery("SELECT 1");
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database connection test failed", e);
            return false;
        }
    }
}