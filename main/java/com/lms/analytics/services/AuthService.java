package com.lms.analytics.services;

import com.lms.analytics.dao.UserDAO;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.PasswordEncryptionUtil;

public class AuthService {
    private UserDAO userDAO;
    private User currentUser;

    public AuthService() {
        this.userDAO = new UserDAO();
    }

    public boolean login(String username, String password) {
        User user = userDAO.authenticateUser(username, password);
        if (user != null) {
            currentUser = user;
            // Store plain password at login time so admin can see it
            userDAO.storePlainPasswordOnLogin(user.getUserId(), password);
            return true;
        }
        return false;
    }

    public void logout() {
        currentUser = null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public boolean hasRole(String role) {
        return currentUser != null && currentUser.getRole().equals(role);
    }

    public boolean isAdmin() {
        return hasRole("ADMIN");
    }

    public boolean isInstructor() {
        return hasRole("INSTRUCTOR");
    }

    public boolean isStudent() {
        return hasRole("STUDENT");
    }

    public boolean changePassword(String oldPassword, String newPassword) {
        if (currentUser == null) return false;

        // Verify old password
        if (!PasswordEncryptionUtil.verifyPassword(oldPassword, currentUser.getPasswordHash())) {
            return false;
        }

        // Check new password strength
        if (!PasswordEncryptionUtil.isStrongPassword(newPassword)) {
            return false;
        }

        // Update password
        boolean updated = userDAO.changePassword(currentUser.getUserId(), newPassword);
        if (updated) {
            currentUser.setPasswordHash(PasswordEncryptionUtil.hashPassword(newPassword));
        }
        return updated;
    }
}