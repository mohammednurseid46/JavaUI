package com.lms.analytics.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.lms.analytics.services.AuthService;
import com.lms.analytics.utils.SessionManager;
import com.lms.analytics.utils.SceneUtil;

import java.util.Objects;

public class AuthController {

    @FXML private StackPane loginRoot;

    private TextField usernameField;
    private PasswordField passwordField;
    private CheckBox rememberMeCheck;
    private Label errorLabel;
    private Button loginButton;
    private VBox formBox;
    private AuthService authService;

    @FXML
    public void initialize() {
        authService = new AuthService();
        buildUI();
    }

    private void buildUI() {
        // ── Dark background ───────────────────────────────────────────
        loginRoot.setStyle("-fx-background-color:#1e293b;");

        HBox mainLayout = new HBox(0);
        mainLayout.setMaxWidth(Double.MAX_VALUE);
        mainLayout.setMaxHeight(Double.MAX_VALUE);

        // ── LEFT 25% — Logo panel ─────────────────────────────────────
        VBox logoPanel = new VBox(20);
        logoPanel.setAlignment(Pos.CENTER);
        logoPanel.setStyle("-fx-background-color:#0f172a;");
        logoPanel.prefWidthProperty().bind(mainLayout.widthProperty().multiply(0.25));
        logoPanel.minWidthProperty().bind(mainLayout.widthProperty().multiply(0.25));
        logoPanel.maxWidthProperty().bind(mainLayout.widthProperty().multiply(0.25));

        // Logo image
        try {
            javafx.scene.image.Image logoImg = new javafx.scene.image.Image(
                Objects.requireNonNull(getClass().getResourceAsStream("/images/logo.png")));
            javafx.scene.image.ImageView logoView = new javafx.scene.image.ImageView(logoImg);
            logoView.setPreserveRatio(true);
            logoView.setFitWidth(180);
            logoView.setFitHeight(180);
            logoView.setSmooth(true);
            logoPanel.getChildren().add(logoView);
        } catch (Exception e) {
            Label fallbackLogo = new Label("🎓");
            fallbackLogo.setStyle("-fx-font-size:80px;");
            logoPanel.getChildren().add(fallbackLogo);
        }

        // University name
        Label uniName = new Label("OCES");
        uniName.setStyle("-fx-font-size:32px; -fx-font-weight:bold; -fx-text-fill:white;");

        Label tagline = new Label("Online Course Enrollment System");
        tagline.setStyle("-fx-font-size:14px; -fx-text-fill:#94a3b8; -fx-text-alignment:center;");
        tagline.setWrapText(true);
        tagline.setMaxWidth(220);

        Label empowerText = new Label("Empowering Students through Digital Education");
        empowerText.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b; " +
            "-fx-text-alignment:center; -fx-font-style:italic;");
        empowerText.setWrapText(true);
        empowerText.setMaxWidth(220);

        logoPanel.getChildren().addAll(uniName, tagline, empowerText);

        // ── RIGHT 75% — Form panel ────────────────────────────────────
        StackPane formPanel = new StackPane();
        formPanel.setStyle("-fx-background-color:#1e293b;");
        HBox.setHgrow(formPanel, Priority.ALWAYS);

        formBox = new VBox(20);
        formBox.setAlignment(Pos.CENTER_LEFT);
        formBox.setPadding(new Insets(0, 80, 0, 80));
        formBox.setMaxWidth(520);

        // Back button
        Button backBtn = new Button("← Back");
        backBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#94a3b8; " +
            "-fx-font-size:13px; -fx-cursor:hand; -fx-padding:0;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-cursor:hand; -fx-padding:0;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#94a3b8; " +
            "-fx-font-size:13px; -fx-cursor:hand; -fx-padding:0;"));
        backBtn.setOnAction(e -> handleBack());
        HBox backRow = new HBox(backBtn);
        backRow.setAlignment(Pos.TOP_LEFT);

        // Title
        Label title = new Label("Welcome Back");
        title.setStyle("-fx-font-size:36px; -fx-font-weight:bold; -fx-text-fill:white;");

        Label subtitle = new Label("Please sign in to access your portal");
        subtitle.setStyle("-fx-font-size:15px; -fx-text-fill:#94a3b8;");

        // Username field
        Label userLbl = new Label("Username");
        userLbl.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:#cbd5e1;");
        usernameField = new TextField();
        usernameField.setPromptText("Enter your username");
        usernameField.setStyle(darkFieldStyle(false));
        usernameField.setMaxWidth(Double.MAX_VALUE);
        usernameField.setOnAction(e -> passwordField.requestFocus());
        usernameField.focusedProperty().addListener((o, old, f) ->
            usernameField.setStyle(darkFieldStyle(f)));

        // Password field
        Label passLbl = new Label("Password");
        passLbl.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:#cbd5e1;");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");
        passwordField.setStyle(darkFieldStyle(false));
        passwordField.setMaxWidth(Double.MAX_VALUE);
        passwordField.setOnAction(e -> handleLogin());
        passwordField.focusedProperty().addListener((o, old, f) ->
            passwordField.setStyle(darkFieldStyle(f)));

        // Remember me checkbox
        rememberMeCheck = new CheckBox("Remember Me");
        rememberMeCheck.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:13px;");

        // Error label
        errorLabel = new Label();
        errorLabel.setStyle(
            "-fx-text-fill:#fca5a5; -fx-font-size:12px; " +
            "-fx-background-color:rgba(220,38,38,0.15); " +
            "-fx-background-radius:8; -fx-padding:10 14;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        // Sign In button
        loginButton = new Button("Sign In");
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setStyle(gradientBtnStyle(false));
        loginButton.setOnAction(e -> handleLogin());
        loginButton.setOnMouseEntered(e -> {
            if (!loginButton.isDisabled()) loginButton.setStyle(gradientBtnStyle(true));
        });
        loginButton.setOnMouseExited(e -> loginButton.setStyle(gradientBtnStyle(false)));

        // Sign up link
        HBox signUpRow = new HBox(6);
        signUpRow.setAlignment(Pos.CENTER);
        Label newUserLbl = new Label("New to our platform?");
        newUserLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#94a3b8;");
        Button signUpBtn = new Button("Create an Account");
        signUpBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#60a5fa; " +
            "-fx-font-size:13px; -fx-font-weight:bold; -fx-cursor:hand; -fx-padding:0;");
        signUpBtn.setOnMouseEntered(e -> signUpBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#93c5fd; " +
            "-fx-font-size:13px; -fx-font-weight:bold; -fx-cursor:hand; -fx-padding:0;"));
        signUpBtn.setOnMouseExited(e -> signUpBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#60a5fa; " +
            "-fx-font-size:13px; -fx-font-weight:bold; -fx-cursor:hand; -fx-padding:0;"));
        signUpBtn.setOnAction(e -> navigateToSignUp());
        signUpRow.getChildren().addAll(newUserLbl, signUpBtn);

        formBox.getChildren().addAll(
            backRow, title, subtitle,
            userLbl, usernameField,
            passLbl, passwordField,
            rememberMeCheck,
            errorLabel, loginButton, signUpRow
        );

        formPanel.getChildren().add(formBox);
        mainLayout.getChildren().addAll(logoPanel, formPanel);
        loginRoot.getChildren().add(mainLayout);

        // Entrance animation
        formBox.setOpacity(0);
        formBox.setTranslateX(30);
        FadeTransition ft = new FadeTransition(Duration.millis(600), formBox);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(600), formBox);
        tt.setToX(0);
        ft.play(); tt.play();
    }

    // ── HANDLERS ──────────────────────────────────────────────────────
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password");
            shake(); return;
        }
        loginButton.setText("Signing in...");
        loginButton.setDisable(true);

        if (authService.login(username, password)) {
            errorLabel.setVisible(false);
            SessionManager.getInstance().startSession(authService.getCurrentUser());
            try {
                Stage stage = (Stage) loginRoot.getScene().getWindow();
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
                Parent root = loader.load();
                Scene scene = SceneUtil.dark(root, getClass());
                FadeTransition fadeOut = new FadeTransition(Duration.millis(300), formBox);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(e -> {
                    stage.setScene(scene);
                    stage.setResizable(true);
                    stage.setMinWidth(1024); stage.setMinHeight(768);
                    stage.setMaximized(true);
                });
                fadeOut.play();
            } catch (Exception e) {
                e.printStackTrace(); resetButton();
                showError("Failed to load main view: " + e.getMessage());
            }
        } else {
            resetButton();
            showError("Invalid username or password");
            shake();
        }
    }

    private void handleBack() {
        try {
            Stage stage = (Stage) loginRoot.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LandingView.fxml"));
            Parent root = loader.load();
            Scene scene = SceneUtil.light(root, getClass());
            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), formBox);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                stage.setScene(scene);
                stage.setWidth(1100); stage.setHeight(720);
                stage.setResizable(true);
                stage.setMinWidth(900); stage.setMinHeight(600);
                stage.centerOnScreen();
            });
            fadeOut.play();
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void navigateToSignUp() {
        try {
            Stage stage = (Stage) loginRoot.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/SignUpView.fxml"));
            Parent root = loader.load();
            Scene scene = SceneUtil.create(root, javafx.scene.paint.Color.web("#1e293b"), getClass());
            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), formBox);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                stage.setScene(scene);
                stage.setWidth(1100); stage.setHeight(720);
                stage.setResizable(true);
                stage.setMinWidth(900);
                stage.centerOnScreen();
                root.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private void shake() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), formBox);
        shake.setFromX(0); shake.setByX(10);
        shake.setCycleCount(6); shake.setAutoReverse(true);
        shake.setOnFinished(e -> formBox.setTranslateX(0));
        shake.play();
    }

    private void resetButton() {
        loginButton.setText("Sign In");
        loginButton.setDisable(false);
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        passwordField.clear();
    }

    private String darkFieldStyle(boolean focused) {
        String border = focused ? "#60a5fa" : "#334155";
        String shadow = focused
            ? "-fx-effect:dropshadow(gaussian,rgba(96,165,250,0.4),10,0,0,0);" : "";
        return "-fx-background-color:#0f172a; " +
               "-fx-border-color:" + border + "; " +
               "-fx-border-radius:8; -fx-background-radius:8; " +
               "-fx-text-fill:white; -fx-prompt-text-fill:#64748b; " +
               "-fx-padding:12 16; -fx-font-size:14px; " + shadow;
    }

    private String gradientBtnStyle(boolean hover) {
        String bg = hover
            ? "linear-gradient(to right, #7c8ef0, #a78bfa)"
            : "linear-gradient(to right, #667eea, #9333ea)";
        return "-fx-background-color:" + bg + "; -fx-text-fill:white; " +
               "-fx-font-size:15px; -fx-font-weight:bold; " +
               "-fx-background-radius:8; -fx-padding:14 0; -fx-cursor:hand; " +
               "-fx-effect:dropshadow(gaussian,rgba(102,126,234,0.5),16,0,0,4);";
    }
}
