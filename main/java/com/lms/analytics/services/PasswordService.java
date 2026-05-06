package com.lms.analytics.services;

import com.lms.analytics.dao.PasswordHistoryDAO;
import com.lms.analytics.dao.UserDAO;
import com.lms.analytics.models.PasswordHistory;
import com.lms.analytics.utils.PasswordEncryptionUtil;
import com.lms.analytics.utils.SessionManager;

import java.util.List;

/**
 * Service layer for all password operations.
 * Integrates with password_history table for audit trail and reuse prevention.
 */
public class PasswordService {

    private final UserDAO            userDAO;
    private final PasswordHistoryDAO historyDAO;

    /** How many recent passwords to check for reuse prevention */
    private static final int REUSE_CHECK_LIMIT = 3;

    public PasswordService() {
        this.userDAO    = new UserDAO();
        this.historyDAO = new PasswordHistoryDAO();
    }

    // ── CHANGE PASSWORD (self) ────────────────────────────────────────

    /**
     * Changes a user's own password.
     * Verifies old password, checks strength, checks reuse, records history.
     */
    public boolean changePassword(int userId, String oldPassword, String newPassword) {
        var user = userDAO.getUserById(userId);
        if (user == null) return false;

        if (!PasswordEncryptionUtil.verifyPassword(oldPassword, user.getPasswordHash()))
            return false;

        if (!PasswordEncryptionUtil.isStrongPassword(newPassword))
            return false;

        if (historyDAO.isPasswordReused(userId, newPassword, REUSE_CHECK_LIMIT))
            return false;

        // changePassword already records history with reason SELF_CHANGE
        return userDAO.changePassword(userId, newPassword);
    }

    // ── RESET PASSWORD (admin) ────────────────────────────────────────

    /**
     * Admin resets a user's password without knowing the old one.
     * Records history with reason ADMIN_RESET.
     */
    public boolean resetPassword(int userId, String newPassword) {
        if (!PasswordEncryptionUtil.isStrongPassword(newPassword)) return false;

        int adminId = 0;
        try {
            var admin = SessionManager.getInstance().getCurrentUser();
            if (admin != null) adminId = admin.getUserId();
        } catch (Exception ignored) {}

        return userDAO.changePasswordWithReason(userId, newPassword, adminId, "ADMIN_RESET");
    }

    /**
     * Admin sets a temporary password (no strength check required).
     * Records history with reason TEMP_PASSWORD.
     */
    public boolean setTempPassword(int userId, String tempPassword) {
        int adminId = 0;
        try {
            var admin = SessionManager.getInstance().getCurrentUser();
            if (admin != null) adminId = admin.getUserId();
        } catch (Exception ignored) {}

        return userDAO.changePasswordWithReason(userId, tempPassword, adminId, "TEMP_PASSWORD");
    }

    // ── HISTORY ───────────────────────────────────────────────────────

    /**
     * Returns the full password change history for a user, newest first.
     */
    public List<PasswordHistory> getPasswordHistory(int userId) {
        return historyDAO.getHistoryByUser(userId);
    }

    /**
     * Returns how many times a user has changed their password.
     */
    public int getPasswordChangeCount(int userId) {
        return historyDAO.getChangeCount(userId);
    }

    /**
     * Checks if a proposed password was used in the last N passwords.
     */
    public boolean isPasswordReused(int userId, String newPassword) {
        return historyDAO.isPasswordReused(userId, newPassword, REUSE_CHECK_LIMIT);
    }

    // ── UTILITIES ─────────────────────────────────────────────────────

    public boolean isStrongPassword(String password) {
        return PasswordEncryptionUtil.isStrongPassword(password);
    }

    public String generateResetToken() {
        return PasswordEncryptionUtil.generateSecureToken();
    }

    public String hashPassword(String plainPassword) {
        return PasswordEncryptionUtil.hashPassword(plainPassword);
    }

    public boolean verifyPassword(String plain, String hashed) {
        return PasswordEncryptionUtil.verifyPassword(plain, hashed);
    }
}
