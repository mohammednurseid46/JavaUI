package com.lms.analytics.models;

import java.time.LocalDateTime;

/**
 * Represents one entry in the password_history table.
 * Every time a user's password changes, a row is inserted here.
 */
public class PasswordHistory {

    private int    historyId;
    private int    userId;
    private String passwordHash;       // BCrypt hash of the password at that time
    private LocalDateTime changedAt;
    private int    changedByUserId;    // 0 = self-change, otherwise admin user_id
    private String changeReason;       // SIGNUP | SELF_CHANGE | ADMIN_RESET | TEMP_PASSWORD
    private boolean isCurrent;         // true for the most recent entry

    public PasswordHistory() {}

    public PasswordHistory(int userId, String passwordHash,
                           int changedByUserId, String changeReason) {
        this.userId          = userId;
        this.passwordHash    = passwordHash;
        this.changedByUserId = changedByUserId;
        this.changeReason    = changeReason;
        this.changedAt       = LocalDateTime.now();
        this.isCurrent       = true;
    }

    // ── Getters & Setters ─────────────────────────────────────────────

    public int getHistoryId()                        { return historyId; }
    public void setHistoryId(int historyId)          { this.historyId = historyId; }

    public int getUserId()                           { return userId; }
    public void setUserId(int userId)                { this.userId = userId; }

    public String getPasswordHash()                  { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public LocalDateTime getChangedAt()              { return changedAt; }
    public void setChangedAt(LocalDateTime changedAt){ this.changedAt = changedAt; }

    public int getChangedByUserId()                  { return changedByUserId; }
    public void setChangedByUserId(int id)           { this.changedByUserId = id; }

    public String getChangeReason()                  { return changeReason; }
    public void setChangeReason(String reason)       { this.changeReason = reason; }

    public boolean isCurrent()                       { return isCurrent; }
    public void setCurrent(boolean current)          { this.isCurrent = current; }

    /** Human-readable label for the change reason */
    public String getChangeReasonLabel() {
        if (changeReason == null) return "Unknown";
        return switch (changeReason) {
            case "SIGNUP"       -> "Account Created";
            case "SELF_CHANGE"  -> "Self Changed";
            case "ADMIN_RESET"  -> "Admin Reset";
            case "TEMP_PASSWORD"-> "Temp Password";
            default             -> changeReason;
        };
    }

    @Override
    public String toString() {
        return "PasswordHistory{userId=" + userId
            + ", reason=" + changeReason
            + ", changedAt=" + changedAt + "}";
    }
}
