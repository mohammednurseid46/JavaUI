package com.lms.analytics.dao;

import com.lms.analytics.models.PasswordHistory;
import com.lms.analytics.utils.DatabaseConnection;
import com.lms.analytics.utils.PasswordEncryptionUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO for the password_history table.
 *
 * Responsibilities:
 *  - Record every password change with who made it and why
 *  - Mark previous entries as not-current when a new password is set
 *  - Support password-reuse checking (prevent reusing last N passwords)
 *  - Provide full history list for admin display
 */
public class PasswordHistoryDAO {

    // ── CREATE ────────────────────────────────────────────────────────

    /**
     * Records a new password change.
     * Marks all previous entries for this user as is_current = 0,
     * then inserts the new entry with is_current = 1.
     *
     * @param userId          the user whose password changed
     * @param newPasswordHash BCrypt hash of the new password
     * @param changedByUserId user_id of who made the change (0 = self)
     * @param changeReason    SIGNUP | SELF_CHANGE | ADMIN_RESET | TEMP_PASSWORD
     * @return true on success
     */
    public boolean recordPasswordChange(int userId, String newPasswordHash,
                                        int changedByUserId, String changeReason) {
        // Step 1: mark all previous entries for this user as not-current
        String markOld = "UPDATE password_history SET is_current = 0 WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(markOld)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Step 2: insert the new entry (separate connection use)
        String insert =
            "INSERT INTO password_history " +
            "(user_id, password_hash, changed_at, changed_by_user_id, change_reason, is_current) " +
            "VALUES (?, ?, CURRENT_TIMESTAMP, ?, ?, 1)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setInt(1, userId);
            ps.setString(2, newPasswordHash);
            ps.setInt(3, changedByUserId);
            ps.setString(4, changeReason);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── READ ──────────────────────────────────────────────────────────

    /**
     * Returns the full password history for a user, newest first.
     */
    public List<PasswordHistory> getHistoryByUser(int userId) {
        List<PasswordHistory> list = new ArrayList<>();
        String sql =
            "SELECT ph.*, u.username AS changed_by_username " +
            "FROM password_history ph " +
            "LEFT JOIN users u ON ph.changed_by_user_id = u.user_id " +
            "WHERE ph.user_id = ? " +
            "ORDER BY ph.changed_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(extract(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    /**
     * Returns the most recent N password hashes for a user.
     * Used for password-reuse checking.
     */
    public List<String> getRecentHashes(int userId, int limit) {
        List<String> hashes = new ArrayList<>();
        String sql =
            "SELECT password_hash FROM password_history " +
            "WHERE user_id = ? " +
            "ORDER BY changed_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) hashes.add(rs.getString("password_hash"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return hashes;
    }

    /**
     * Returns the current (most recent) password history entry for a user.
     */
    public PasswordHistory getCurrentEntry(int userId) {
        String sql =
            "SELECT * FROM password_history " +
            "WHERE user_id = ? AND is_current = 1 LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return extract(rs);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Returns total number of password changes for a user.
     */
    public int getChangeCount(int userId) {
        String sql = "SELECT COUNT(*) FROM password_history WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ── BUSINESS LOGIC ────────────────────────────────────────────────

    /**
     * Checks whether a proposed new password was used in the last N passwords.
     * Prevents password reuse.
     *
     * @param userId      the user
     * @param newPassword plain text of the proposed new password
     * @param checkLast   how many recent passwords to check (e.g. 3)
     * @return true if the password was recently used (should be rejected)
     */
    public boolean isPasswordReused(int userId, String newPassword, int checkLast) {
        List<String> recentHashes = getRecentHashes(userId, checkLast);
        for (String hash : recentHashes) {
            if (PasswordEncryptionUtil.verifyPassword(newPassword, hash)) {
                return true;
            }
        }
        return false;
    }

    // ── DELETE ────────────────────────────────────────────────────────

    /**
     * Deletes all password history for a user.
     * Called when a user account is permanently deleted.
     */
    public boolean deleteHistoryForUser(int userId) {
        String sql = "DELETE FROM password_history WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            return ps.executeUpdate() >= 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // ── HELPER ────────────────────────────────────────────────────────

    private PasswordHistory extract(ResultSet rs) throws SQLException {
        PasswordHistory ph = new PasswordHistory();
        ph.setHistoryId(rs.getInt("history_id"));
        ph.setUserId(rs.getInt("user_id"));
        ph.setPasswordHash(rs.getString("password_hash"));
        ph.setChangedByUserId(rs.getInt("changed_by_user_id"));
        ph.setChangeReason(rs.getString("change_reason"));
        ph.setCurrent(rs.getBoolean("is_current"));

        String ts = rs.getString("changed_at");
        if (ts != null && !ts.isBlank()) {
            try {
                ph.setChangedAt(java.time.LocalDateTime.parse(
                    ts.substring(0, 19).replace(" ", "T")));
            } catch (Exception ignored) {}
        }
        return ph;
    }
}
