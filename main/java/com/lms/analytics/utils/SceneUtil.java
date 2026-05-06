package com.lms.analytics.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.util.Objects;

/**
 * Utility for creating scenes with proper fill colors to eliminate
 * the white gap between the OS title bar and app content.
 */
public class SceneUtil {

    /** Light background — used for landing, login, signup pages */
    public static final Color LIGHT_BG = Color.web("#f0f4ff");

    /** Dark background — used for main app */
    public static final Color DARK_BG  = Color.web("#0d1117");

    /**
     * Creates a Scene with the correct fill color and stylesheet applied.
     * This eliminates the white flash/gap between the OS title bar and content.
     */
    public static Scene create(Parent root, Color fillColor, Class<?> caller) {
        Scene scene = new Scene(root);
        scene.setFill(fillColor);
        try {
            scene.getStylesheets().add(Objects.requireNonNull(
                caller.getResource("/css/main.css")).toExternalForm());
        } catch (Exception ignored) {}
        try {
            scene.getStylesheets().add(Objects.requireNonNull(
                caller.getResource("/css/app.css")).toExternalForm());
        } catch (Exception ignored) {}
        return scene;
    }

    /** Convenience: light scene (landing / login / signup) */
    public static Scene light(Parent root, Class<?> caller) {
        return create(root, LIGHT_BG, caller);
    }

    /** Convenience: dark scene (main app) */
    public static Scene dark(Parent root, Class<?> caller) {
        return create(root, DARK_BG, caller);
    }
}
