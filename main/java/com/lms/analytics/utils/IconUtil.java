package com.lms.analytics.utils;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * Utility class for creating standardized icons across the application.
 * Provides consistent sizing, styling, and behavior for all UI icons.
 */
public class IconUtil {

    // ── STANDARD ICON SIZES ───────────────────────────────────────────
    public static final int SIZE_SMALL  = 16;
    public static final int SIZE_MEDIUM = 24;
    public static final int SIZE_LARGE  = 32;
    public static final int SIZE_XLARGE = 48;

    // ── STANDARD COLORS ───────────────────────────────────────────────
    public static final String COLOR_PRIMARY   = "#667eea";
    public static final String COLOR_SUCCESS   = "#22c55e";
    public static final String COLOR_WARNING   = "#f97316";
    public static final String COLOR_DANGER    = "#dc2626";
    public static final String COLOR_INFO      = "#1f6feb";
    public static final String COLOR_NEUTRAL   = "#64748b";

    /**
     * Creates a simple emoji icon label with standard styling.
     */
    public static Label icon(String emoji, int size) {
        Label icon = new Label(emoji);
        icon.setStyle(
            "-fx-font-size:" + size + "px; " +
            "-fx-padding:0; " +
            "-fx-alignment:center;");
        return icon;
    }

    /**
     * Creates an icon with a colored circular background.
     */
    public static StackPane iconWithBackground(String emoji, int size, String bgColor) {
        StackPane container = new StackPane();
        
        Circle circle = new Circle(size * 1.2);
        circle.setStyle("-fx-fill:" + bgColor + ";");
        
        Label icon = new Label(emoji);
        icon.setStyle("-fx-font-size:" + size + "px; -fx-text-fill:white;");
        
        container.getChildren().addAll(circle, icon);
        return container;
    }

    /**
     * Creates a small icon badge (for status indicators, counts, etc.)
     */
    public static Label badge(String text, String bgColor, String textColor) {
        Label badge = new Label(text);
        badge.setStyle(
            "-fx-background-color:" + bgColor + "; " +
            "-fx-text-fill:" + textColor + "; " +
            "-fx-font-size:11px; " +
            "-fx-font-weight:bold; " +
            "-fx-background-radius:10; " +
            "-fx-padding:3 10; " +
            "-fx-alignment:center;");
        return badge;
    }

    /**
     * Creates a status dot indicator.
     */
    public static Circle statusDot(String color, double radius) {
        Circle dot = new Circle(radius);
        dot.setStyle("-fx-fill:" + color + ";");
        return dot;
    }

    /**
     * Standard icon for navigation buttons.
     */
    public static Label navIcon(String emoji) {
        return icon(emoji, SIZE_MEDIUM);
    }

    /**
     * Standard icon for cards/panels.
     */
    public static Label cardIcon(String emoji) {
        return icon(emoji, SIZE_LARGE);
    }

    /**
     * Standard icon for headers/titles.
     */
    public static Label headerIcon(String emoji) {
        return icon(emoji, SIZE_XLARGE);
    }

    /**
     * Standard icon for buttons.
     */
    public static Label buttonIcon(String emoji) {
        return icon(emoji, SIZE_SMALL);
    }

    /**
     * Creates a success badge (green).
     */
    public static Label successBadge(String text) {
        return badge(text, "#dcfce7", "#16a34a");
    }

    /**
     * Creates a warning badge (orange).
     */
    public static Label warningBadge(String text) {
        return badge(text, "#fed7aa", "#ea580c");
    }

    /**
     * Creates a danger badge (red).
     */
    public static Label dangerBadge(String text) {
        return badge(text, "#fee2e2", "#dc2626");
    }

    /**
     * Creates an info badge (blue).
     */
    public static Label infoBadge(String text) {
        return badge(text, "#dbeafe", "#1d4ed8");
    }

    /**
     * Creates a neutral badge (gray).
     */
    public static Label neutralBadge(String text) {
        return badge(text, "#f1f5f9", "#475569");
    }

    /**
     * Standard online status dot (green).
     */
    public static Circle onlineDot() {
        return statusDot("#22c55e", 4);
    }

    /**
     * Standard offline status dot (gray).
     */
    public static Circle offlineDot() {
        return statusDot("#94a3b8", 4);
    }

    /**
     * Standard error status dot (red).
     */
    public static Circle errorDot() {
        return statusDot("#dc2626", 4);
    }

    /**
     * Standard warning status dot (orange).
     */
    public static Circle warningDot() {
        return statusDot("#f97316", 4);
    }
}
