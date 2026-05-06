package com.lms.analytics.utils;

import com.lms.analytics.models.User;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SessionManager {
    private static SessionManager instance;
    private Map<String, Object> sessionAttributes;
    private User currentUser;
    private LocalDateTime loginTime;
    private String sessionId;

    private SessionManager() {
        sessionAttributes = new HashMap<>();
        sessionId = generateSessionId();
    }

    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void startSession(User user) {
        this.currentUser = user;
        this.loginTime = LocalDateTime.now();
        this.sessionId = generateSessionId();
        sessionAttributes.clear();
    }

    public void endSession() {
        this.currentUser = null;
        this.loginTime = null;
        sessionAttributes.clear();
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isSessionActive() {
        return currentUser != null;
    }

    public LocalDateTime getLoginTime() {
        return loginTime;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setAttribute(String key, Object value) {
        sessionAttributes.put(key, value);
    }

    public Object getAttribute(String key) {
        return sessionAttributes.get(key);
    }

    public void removeAttribute(String key) {
        sessionAttributes.remove(key);
    }

    private String generateSessionId() {
        return java.util.UUID.randomUUID().toString();
    }

    public long getSessionDurationMinutes() {
        if (loginTime == null) return 0;
        return java.time.Duration.between(loginTime, LocalDateTime.now()).toMinutes();
    }
}