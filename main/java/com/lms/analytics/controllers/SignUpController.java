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

import com.lms.analytics.services.SignUpService;
import com.lms.analytics.services.SignUpService.RegisterResult;
import com.lms.analytics.utils.SceneUtil;

import java.util.Objects;

public class SignUpController {

    @FXML private StackPane signUpRoot;

    private TextField fullNameField, usernameField, emailField, majorField;
    private PasswordField passwordField, confirmPasswordField;
    private Label errorLabel, successLabel, strengthLabel;
    private ProgressBar strengthBar;
    private Button registerButton;
    private VBox formBox;

    private final SignUpService signUpService = new SignUpService();

    @FXML
    public void initialize() { buildUI(); }

    private void buildUI() {
        // ── Dark background ───────────────────────────────────────────
        signUpRoot.setStyle("-fx-background-color:#1e293b;");

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

        // ── RIGHT 75% — Scrollable form ───────────────────────────────
        ScrollPane formScroll = new ScrollPane();
        formScroll.setFitToWidth(true);
        formScroll.setStyle("-fx-background-color:#1e293b; -fx-background:#1e293b;");
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        HBox.setHgrow(formScroll, Priority.ALWAYS);

        formBox = new VBox(16);
        formBox.setAlignment(Pos.CENTER_LEFT);
        formBox.setPadding(new Insets(40, 80, 40, 80));
        formBox.setMaxWidth(560);

        // Back button
        Button backBtn = new Button("← Back to Login");
        backBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#94a3b8; " +
            "-fx-font-size:13px; -fx-cursor:hand; -fx-padding:0;");
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-cursor:hand; -fx-padding:0;"));
        backBtn.setOnMouseExited(e -> backBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#94a3b8; " +
            "-fx-font-size:13px; -fx-cursor:hand; -fx-padding:0;"));
        backBtn.setOnAction(e -> navigateToLogin());

        // Title
        Label title = new Label("Create Student Account");
        title.setStyle("-fx-font-size:32px; -fx-font-weight:bold; -fx-text-fill:white;");

        Label subtitle = new Label("Join OCES as a Student — fill in your details below");
        subtitle.setStyle("-fx-font-size:14px; -fx-text-fill:#94a3b8;");

        // Role badge
        Label roleBadge = new Label("👤  Role: STUDENT");
        roleBadge.setStyle(
            "-fx-background-color:rgba(59,130,246,0.15); " +
            "-fx-border-color:#3b82f6; -fx-border-radius:20; " +
            "-fx-background-radius:20; -fx-padding:5 16; " +
            "-fx-text-fill:#60a5fa; -fx-font-size:12px; -fx-font-weight:bold;");

        // Form fields
        fullNameField        = darkField("e.g. John Doe");
        usernameField        = darkField("Choose a username");
        emailField           = darkField("your@email.com");
        passwordField        = darkPassField("Min 8 chars, mixed case + number + symbol");
        confirmPasswordField = darkPassField("Re-enter your password");
        majorField           = darkField("e.g. Computer Science (optional)");

        // Password strength
        strengthBar = new ProgressBar(0);
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        strengthBar.setPrefHeight(5);
        strengthBar.setStyle("-fx-accent:#ef4444;");
        strengthLabel = new Label("Password strength");
        strengthLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");
        passwordField.textProperty().addListener((o, old, v) -> updateStrength(v));

        // Match indicator
        Label matchLabel = new Label();
        matchLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");
        confirmPasswordField.textProperty().addListener((o, old, v) -> {
            if (v.isEmpty()) matchLabel.setText("");
            else if (v.equals(passwordField.getText())) {
                matchLabel.setText("✓ Passwords match");
                matchLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#4ade80;");
            } else {
                matchLabel.setText("✗ Passwords do not match");
                matchLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#f87171;");
            }
        });

        // Feedback
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill:#fca5a5; -fx-font-size:12px; " +
            "-fx-background-color:rgba(220,38,38,0.15); -fx-background-radius:8; -fx-padding:10 14;");
        errorLabel.setVisible(false);
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        successLabel = new Label();
        successLabel.setStyle("-fx-text-fill:#86efac; -fx-font-size:12px; " +
            "-fx-background-color:rgba(34,197,94,0.15); -fx-background-radius:8; -fx-padding:10 14;");
        successLabel.setVisible(false);
        successLabel.setWrapText(true);
        successLabel.setMaxWidth(Double.MAX_VALUE);

        // Register button
        registerButton = new Button("Create Account");
        registerButton.setMaxWidth(Double.MAX_VALUE);
        registerButton.setStyle(greenGradientBtnStyle(false));
        registerButton.setOnAction(e -> handleRegister());
        registerButton.setOnMouseEntered(e -> {
            if (!registerButton.isDisabled()) registerButton.setStyle(greenGradientBtnStyle(true));
        });
        registerButton.setOnMouseExited(e -> registerButton.setStyle(greenGradientBtnStyle(false)));

        // Login link
        HBox loginLink = new HBox(6);
        loginLink.setAlignment(Pos.CENTER);
        Label alreadyLbl = new Label("Already have an account?");
        alreadyLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#94a3b8;");
        Button signInBtn = new Button("Sign In");
        signInBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#60a5fa; " +
            "-fx-font-size:13px; -fx-font-weight:bold; -fx-cursor:hand; -fx-padding:0;");
        signInBtn.setOnMouseEntered(e -> signInBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#93c5fd; " +
            "-fx-font-size:13px; -fx-font-weight:bold; -fx-cursor:hand; -fx-padding:0;"));
        signInBtn.setOnMouseExited(e -> signInBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#60a5fa; " +
            "-fx-font-size:13px; -fx-font-weight:bold; -fx-cursor:hand; -fx-padding:0;"));
        signInBtn.setOnAction(e -> navigateToLogin());
        loginLink.getChildren().addAll(alreadyLbl, signInBtn);

        formBox.getChildren().addAll(
            backBtn, title, subtitle, roleBadge,
            fieldLabel("Full Name *"), fullNameField,
            fieldLabel("Username *"), usernameField,
            fieldLabel("Email Address *"), emailField,
            fieldLabel("Password *"), passwordField,
            strengthBar, strengthLabel,
            fieldLabel("Confirm Password *"), confirmPasswordField,
            matchLabel,
            fieldLabel("Major (optional)"), majorField,
            errorLabel, successLabel,
            registerButton, loginLink
        );

        StackPane formWrapper = new StackPane(formBox);
        formWrapper.setPadding(new Insets(30, 0, 30, 0));
        formScroll.setContent(formWrapper);

        mainLayout.getChildren().addAll(logoPanel, formScroll);
        signUpRoot.getChildren().add(mainLayout);

        // Entrance animation
        formBox.setOpacity(0);
        formBox.setTranslateX(30);
        FadeTransition ft = new FadeTransition(Duration.millis(600), formBox);
        ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(600), formBox);
        tt.setToX(0);
        ft.play(); tt.play();
    }

    // ── REGISTER ──────────────────────────────────────────────────────
    private void handleRegister() {
        hideMessages();
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String email    = emailField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();
        String major    = majorField.getText().trim();

        registerButton.setText("Creating account...");
        registerButton.setDisable(true);

        RegisterResult result = signUpService.registerStudent(
            fullName, username, email, password, confirm, major);

        registerButton.setDisable(false);
        registerButton.setText("Create Account");

        switch (result) {
            case SUCCESS -> {
                showSuccess("✓  Account created successfully! Redirecting to login...");
                clearForm();
                new Timeline(new KeyFrame(Duration.seconds(2), e -> navigateToLogin())).play();
            }
            case USERNAME_TAKEN  -> showError("⚠  Username '" + username + "' is already taken.");
            case EMAIL_TAKEN     -> showError("⚠  Email already registered.");
            case INVALID_EMAIL   -> showError("⚠  Invalid email address. Use format: user@domain.com");
            case PASSWORDS_MISMATCH -> showError("⚠  Passwords do not match.");
            case WEAK_PASSWORD   -> showError("⚠  Password too weak.\n" + signUpService.getPasswordRequirements());
            case MISSING_FIELDS  -> showError("⚠  Please fill in all required fields.");
            default              -> showError("⚠  Registration failed. Please try again.");
        }
        if (result != RegisterResult.SUCCESS) shake();
    }

    // ── NAVIGATION ────────────────────────────────────────────────────
    private void navigateToLogin() {
        try {
            Stage stage = (Stage) signUpRoot.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();
            Scene scene = SceneUtil.create(root, javafx.scene.paint.Color.web("#1e293b"), getClass());
            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), formBox);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                stage.setScene(scene);
                stage.setWidth(1100); stage.setHeight(720);
                stage.setResizable(true); stage.setMinWidth(900);
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
    private TextField darkField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setStyle(darkFieldStyle(false));
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.focusedProperty().addListener((o, old, f) -> tf.setStyle(darkFieldStyle(f)));
        return tf;
    }

    private PasswordField darkPassField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setStyle(darkFieldStyle(false));
        pf.setMaxWidth(Double.MAX_VALUE);
        pf.focusedProperty().addListener((o, old, f) -> pf.setStyle(darkFieldStyle(f)));
        return pf;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:600; -fx-text-fill:#cbd5e1;");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private void updateStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;
        strengthBar.setProgress(score / 5.0);
        String color, text;
        if (score <= 1)      { color = "#ef4444"; text = "Very Weak"; }
        else if (score == 2) { color = "#f97316"; text = "Weak"; }
        else if (score == 3) { color = "#eab308"; text = "Fair"; }
        else if (score == 4) { color = "#22c55e"; text = "Strong"; }
        else                 { color = "#16a34a"; text = "Very Strong ✓"; }
        strengthBar.setStyle("-fx-accent:" + color + ";");
        strengthLabel.setText("Strength: " + text);
        strengthLabel.setStyle("-fx-font-size:11px; -fx-text-fill:" + color + ";");
    }

    private void shake() {
        TranslateTransition shake = new TranslateTransition(Duration.millis(55), formBox);
        shake.setFromX(0); shake.setByX(10);
        shake.setCycleCount(6); shake.setAutoReverse(true);
        shake.setOnFinished(e -> formBox.setTranslateX(0));
        shake.play();
    }

    private void showError(String msg) {
        errorLabel.setText(msg); errorLabel.setVisible(true); successLabel.setVisible(false);
    }

    private void showSuccess(String msg) {
        successLabel.setText(msg); successLabel.setVisible(true); errorLabel.setVisible(false);
    }

    private void hideMessages() {
        errorLabel.setVisible(false); successLabel.setVisible(false);
    }

    private void clearForm() {
        fullNameField.clear(); usernameField.clear(); emailField.clear();
        passwordField.clear(); confirmPasswordField.clear(); majorField.clear();
        strengthBar.setProgress(0);
        strengthLabel.setText("Password strength");
        strengthLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");
    }

    private String darkFieldStyle(boolean focused) {
        String border = focused ? "#60a5fa" : "#334155";
        String shadow = focused
            ? "-fx-effect:dropshadow(gaussian,rgba(96,165,250,0.4),10,0,0,0);" : "";
        return "-fx-background-color:#0f172a; " +
               "-fx-border-color:" + border + "; " +
               "-fx-border-radius:8; -fx-background-radius:8; " +
               "-fx-text-fill:white; -fx-prompt-text-fill:#64748b; " +
               "-fx-padding:12 16; -fx-font-size:13px; " + shadow;
    }

    private String greenGradientBtnStyle(boolean hover) {
        String bg = hover
            ? "linear-gradient(to right, #15803d, #16a34a)"
            : "linear-gradient(to right, #16a34a, #22c55e)";
        return "-fx-background-color:" + bg + "; -fx-text-fill:white; " +
               "-fx-font-size:15px; -fx-font-weight:bold; " +
               "-fx-background-radius:8; -fx-padding:14 0; -fx-cursor:hand; " +
               "-fx-effect:dropshadow(gaussian,rgba(34,197,94,0.5),16,0,0,4);";
    }
}
