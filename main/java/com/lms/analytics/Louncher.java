package com.lms.analytics;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import com.lms.analytics.utils.DatabaseConnection;
import com.lms.analytics.utils.SessionManager;
import com.lms.analytics.utils.SceneUtil;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URL;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Louncher extends Application {

    private static final Logger LOGGER = Logger.getLogger(Louncher.class.getName());
    private Stage primaryStage;
    private TrayIcon trayIcon;
    private SystemTray systemTray;

    @Override
    public void init() throws Exception {
        super.init();
        LOGGER.info("Initializing OCES Platform...");
        try {
            DatabaseConnection.initializeDatabase();
            LOGGER.info("Database initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
            throw e;
        }
    }

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        try {
            Platform.setImplicitExit(false);
            configurePrimaryStage();
            showLoginView();
            setupSystemTray();
            addShutdownHook();
            LOGGER.info("Application started successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error starting application", e);
            showErrorAlert("Failed to start application: " + e.getMessage());
        }
    }

    // ── SYSTEM TRAY ───────────────────────────────────────────────────
    private void setupSystemTray() {
        if (!SystemTray.isSupported()) {
            LOGGER.warning("System tray not supported on this platform.");
            return;
        }

        systemTray = SystemTray.getSystemTray();

        java.awt.Image trayImage = null;
        try {
            URL iconUrl = getClass().getResource("/images/logo.png");
            if (iconUrl != null) {
                trayImage = Toolkit.getDefaultToolkit().getImage(iconUrl);
                int size = SystemTray.getSystemTray().getTrayIconSize().width;
                trayImage = trayImage.getScaledInstance(size, size, java.awt.Image.SCALE_SMOOTH);
            }
        } catch (Exception e) {
            LOGGER.warning("Could not load tray icon: " + e.getMessage());
        }

        if (trayImage == null) {
            int size = 16;
            java.awt.image.BufferedImage img =
                new java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(new Color(56, 189, 248));
            g.fillOval(0, 0, size, size);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 9));
            g.drawString("O", 4, 11);
            g.dispose();
            trayImage = img;
        }

        PopupMenu popup = new PopupMenu();

        MenuItem showItem = new MenuItem("Open OCES");
        showItem.addActionListener(e -> Platform.runLater(this::showWindow));

        MenuItem hideItem = new MenuItem("Hide to Tray");
        hideItem.addActionListener(e -> Platform.runLater(this::hideWindow));

        MenuItem exitItem = new MenuItem("Exit OCES");
        exitItem.addActionListener(e -> Platform.runLater(() -> {
            removeTrayIcon();
            Platform.exit();
            System.exit(0);
        }));

        popup.add(showItem);
        popup.add(hideItem);
        popup.addSeparator();
        popup.add(new MenuItem("OCES v2025 — Online Course Enrollment"));
        popup.addSeparator();
        popup.add(exitItem);

        trayIcon = new TrayIcon(trayImage, "OCES — Online Course Enrollment System", popup);
        trayIcon.setImageAutoSize(true);

        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    Platform.runLater(() -> showWindow());
            }
        });

        try {
            systemTray.add(trayIcon);
            LOGGER.info("System tray icon added.");
        } catch (AWTException e) {
            LOGGER.warning("Could not add tray icon: " + e.getMessage());
        }

        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            hideWindow();
            if (trayIcon != null)
                trayIcon.displayMessage(
                    "OCES is still running",
                    "Minimized to system tray. Double-click to reopen.",
                    TrayIcon.MessageType.INFO);
        });
    }

    private void showWindow() {
        if (primaryStage != null) {
            primaryStage.show();
            primaryStage.toFront();
            primaryStage.requestFocus();
        }
    }

    private void hideWindow() {
        if (primaryStage != null) primaryStage.hide();
    }

    private void removeTrayIcon() {
        if (systemTray != null && trayIcon != null)
            systemTray.remove(trayIcon);
    }

    // ── STAGE SETUP ───────────────────────────────────────────────────
    private void configurePrimaryStage() {
        primaryStage.setTitle("OCES — Online Course Enrollment System");
        primaryStage.setWidth(1100);
        primaryStage.setHeight(720);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        try {
            Image icon = new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/images/logo.png")));
            primaryStage.getIcons().add(icon);
        } catch (Exception e) {
            LOGGER.warning("Application icon not found");
        }
    }

    private void showLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LandingView.fxml"));
            Parent root = loader.load();
            Scene scene = SceneUtil.create(root,
                javafx.scene.paint.Color.web("#0f172a"), getClass());
            addAppCss(scene);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load landing view", e);
            showErrorAlert("Failed to load landing screen: " + e.getMessage());
        }
    }

    public void showMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            Parent root = loader.load();
            Scene scene = SceneUtil.dark(root, getClass());
            addAppCss(scene);
            primaryStage.setScene(scene);
            primaryStage.setMaximized(true);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to load main view", e);
            showErrorAlert("Failed to load main application view: " + e.getMessage());
        }
    }

    private void addAppCss(Scene scene) {
        try {
            String appCss = Objects.requireNonNull(
                getClass().getResource("/css/app.css")).toExternalForm();
            if (!scene.getStylesheets().contains(appCss))
                scene.getStylesheets().add(appCss);
        } catch (Exception e) {
            LOGGER.warning("app.css not found");
        }
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Shutting down application...");
            try {
                SessionManager.getInstance().endSession();
                DatabaseConnection.closeConnection();
                removeTrayIcon();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during shutdown", e);
            }
        }));
    }

    private void showErrorAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle("Startup Error");
        alert.setHeaderText("Failed to Start OCES");
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        SessionManager.getInstance().endSession();
        DatabaseConnection.closeConnection();
        removeTrayIcon();
    }

    public static void main(String[] args) {
        System.setProperty("prism.lcdtext", "false");
        System.setProperty("prism.text", "t2k");
        System.setProperty("java.awt.headless", "false");
        launch(args);
    }
}
