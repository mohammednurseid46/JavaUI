package com.lms.analytics.utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

/**
 * Singleton that holds the current dark/light mode state.
 * All controllers read from this to apply correct colors.
 */
public class ThemeManager {

    private static ThemeManager instance;
    private final BooleanProperty darkMode = new SimpleBooleanProperty(true);

    private ThemeManager() {}

    public static ThemeManager getInstance() {
        if (instance == null) instance = new ThemeManager();
        return instance;
    }

    public boolean isDark() { return darkMode.get(); }
    public void toggle()    { darkMode.set(!darkMode.get()); }
    public BooleanProperty darkModeProperty() { return darkMode; }

    // ── THEME COLORS ──────────────────────────────────────────────────

    public String bg()          { return isDark() ? "#0d1117" : "#f0f2f5"; }
    public String surface()     { return isDark() ? "#161b22" : "#ffffff"; }
    public String sidebar()     { return isDark() ? "#0d1117" : "#1e293b"; }
    public String sidebarBorder(){ return isDark() ? "#21262d" : "#334155"; }
    public String topBar()      { return isDark() ? "#161b22" : "#1e293b"; }
    public String topBorder()   { return isDark() ? "#21262d" : "#334155"; }
    public String textPrimary() { return isDark() ? "#ffffff"  : "#0f172a"; }
    public String textSecondary(){ return isDark() ? "#c9d1d9" : "#334155"; }
    public String textMuted()   { return isDark() ? "#8b949e"  : "#64748b"; }
    public String navSection()  { return isDark() ? "#6e7681"  : "#94a3b8"; }
    public String navInactive() { return isDark() ? "#c9d1d9"  : "#e2e8f0"; }
    public String navHover()    { return isDark() ? "#21262d"  : "#334155"; }
    public String statusBar()   { return isDark() ? "#161b22"  : "#1e293b"; }
    public String statusText()  { return isDark() ? "#c9d1d9"  : "#e2e8f0"; }
    public String devBoxBg()    { return isDark()
        ? "rgba(255,255,255,0.08)" : "rgba(255,255,255,0.15)"; }
    public String devBoxBorder(){ return isDark()
        ? "rgba(255,255,255,0.18)" : "rgba(255,255,255,0.35)"; }
    public String contentBg()   { return isDark() ? "#0d1117"  : "#f0f2f5"; }
    public String onlineColor() { return "#3fb950"; }

    public String toggleBtnStyle() {
        return isDark()
            ? "-fx-background-color:#21262d; -fx-text-fill:#c9d1d9; " +
              "-fx-font-size:16px; -fx-background-radius:20; " +
              "-fx-padding:4 10; -fx-cursor:hand; " +
              "-fx-border-color:#30363d; -fx-border-radius:20; -fx-border-width:1;"
            : "-fx-background-color:#f1f5f9; -fx-text-fill:#334155; " +
              "-fx-font-size:16px; -fx-background-radius:20; " +
              "-fx-padding:4 10; -fx-cursor:hand; " +
              "-fx-border-color:#cbd5e1; -fx-border-radius:20; -fx-border-width:1;";
    }
}
