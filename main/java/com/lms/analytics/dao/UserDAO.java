package com.lms.analytics.dao;


import com.lms.analytics.models.User;
import com.lms.analytics.utils.DatabaseConnection;
import com.lms.analytics.utils.PasswordEncryptionUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    // CREATE
    public boolean createUser(User user) {
        String sql = "INSERT INTO users (username, password_hash, plain_password, email, full_name, role, is_active) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getUsername());
            pstmt.setString(2, user.getPasswordHash());
            pstmt.setString(3, user.getPlainPassword());
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getFullName());
            pstmt.setString(6, user.getRole());
            pstmt.setBoolean(7, user.isActive());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                // Retrieve the generated user_id
                try (Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
                    if (rs.next()) {
                        user.setUserId(rs.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }

        if (user.getUserId() <= 0) return false;

        // Record initial password in history AFTER the connection is fully closed.
        // Must be outside the try-with-resources to avoid SQLite single-connection conflicts.
        try {
            new com.lms.analytics.dao.PasswordHistoryDAO()
                .recordPasswordChange(user.getUserId(), user.getPasswordHash(),
                    user.getUserId(), "SIGNUP");
        } catch (Exception e) {
            // History failure must NOT block user creation — log and continue
            e.printStackTrace();
        }
        return true;
    }

    // READ - Get user by username
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // READ - Get user by ID
    public User getUserById(int userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return extractUserFromResultSet(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // READ - Get all users
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // READ - Get users by role
    public List<User> getUsersByRole(String role) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE role = ? ORDER BY full_name";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                users.add(extractUserFromResultSet(rs));}
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // UPDATE - Update user
    public boolean updateUser(User user) {
        String sql = "UPDATE users SET email = ?, full_name = ?, role = ?, is_active = ? WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, user.getEmail());
            pstmt.setString(2, user.getFullName());
            pstmt.setString(3, user.getRole());
            pstmt.setBoolean(4, user.isActive());
            pstmt.setInt(5, user.getUserId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // UPDATE - Change password (saves both hash and plain text, records history)
    public boolean changePassword(int userId, String newPassword) {
        String hashedPassword = PasswordEncryptionUtil.hashPassword(newPassword);
        String sql = "UPDATE users SET password_hash = ?, plain_password = ? WHERE user_id = ?";

        boolean updated = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, newPassword);
            pstmt.setInt(3, userId);
            updated = pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Record history AFTER connection is released
        if (updated) {
            try {
                int changedBy = resolveChangedBy(userId);
                String reason = (changedBy != userId && changedBy != 0)
                    ? "ADMIN_RESET" : "SELF_CHANGE";
                new com.lms.analytics.dao.PasswordHistoryDAO()
                    .recordPasswordChange(userId, hashedPassword, changedBy, reason);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return updated;
    }

    /**
     * Change password with explicit reason — used by admin operations.
     */
    public boolean changePasswordWithReason(int userId, String newPassword,
                                            int changedByUserId, String reason) {
        String hashedPassword = PasswordEncryptionUtil.hashPassword(newPassword);
        String sql = "UPDATE users SET password_hash = ?, plain_password = ? WHERE user_id = ?";

        boolean updated = false;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, hashedPassword);
            pstmt.setString(2, newPassword);
            pstmt.setInt(3, userId);
            updated = pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Record history AFTER connection is released
        if (updated) {
            try {
                new com.lms.analytics.dao.PasswordHistoryDAO()
                    .recordPasswordChange(userId, hashedPassword, changedByUserId, reason);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return updated;
    }

    /**
     * Resolves who is making the password change.
     * Returns the current logged-in user's ID, or 0 if unknown.
     */
    private int resolveChangedBy(int targetUserId) {
        try {
            com.lms.analytics.models.User current =
                com.lms.analytics.utils.SessionManager.getInstance().getCurrentUser();
            if (current != null) return current.getUserId();
        } catch (Exception ignored) {}
        return targetUserId; // self-change fallback
    }

    // Backfill plain passwords for all users who have NULL plain_password
    public int backfillPlainPasswords() {
        String selectSql =
            "SELECT user_id, role, password_hash FROM users " +
            "WHERE (plain_password IS NULL OR plain_password = '') AND role != 'ADMIN'";
        String updateSql = "UPDATE users SET plain_password = ? WHERE user_id = ?";
        int count = 0;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql)) {

            while (rs.next()) {
                int userId = rs.getInt("user_id");
                String role = rs.getString("role");
                String hash = rs.getString("password_hash");
                String defaultPass = "INSTRUCTOR".equals(role) ? "Instructor@123" : "Student@123";

                if (hash != null && PasswordEncryptionUtil.verifyPassword(defaultPass, hash)) {
                    try (PreparedStatement upd = conn.prepareStatement(updateSql)) {
                        upd.setString(1, defaultPass);
                        upd.setInt(2, userId);
                        upd.executeUpdate();
                        count++;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    // UPDATE - Store plain password when user logs in (captures it at login time)
    public void storePlainPasswordOnLogin(int userId, String plainPassword) {
        String sql = "UPDATE users SET plain_password = ? WHERE user_id = ? AND (plain_password IS NULL OR plain_password = '')";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, plainPassword);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // UPDATE - Update last login time
    public void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // DELETE - Delete user (soft delete)
    public boolean deleteUser(int userId) {
        String sql = "UPDATE users SET is_active = 0 WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ACTIVATE - Re-enable a previously deactivated user
    public boolean activateUser(int userId) {
        String sql = "UPDATE users SET is_active = 1 WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Hard delete (use carefully)
    public boolean hardDeleteUser(int userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Authenticate user
    public User authenticateUser(String username, String password) {
        User user = getUserByUsername(username);

        if (user != null && user.isActive()) {
            if (PasswordEncryptionUtil.verifyPassword(password, user.getPasswordHash())) {
                updateLastLogin(user.getUserId());
                return user;
            }
        }
        return null;
    }

    private User extractUserFromResultSet(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPasswordHash(rs.getString("password_hash"));
        // Read plain password — may be null for old records
        try { user.setPlainPassword(rs.getString("plain_password")); }
        catch (SQLException ignored) {}
        user.setEmail(rs.getString("email"));
        user.setFullName(rs.getString("full_name"));
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }

        return user;
    }

    // Get user count
    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users WHERE is_active = 1";

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
}
