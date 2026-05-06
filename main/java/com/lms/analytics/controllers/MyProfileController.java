package com.lms.analytics.controllers;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;

import com.lms.analytics.dao.StudentDAO;
import com.lms.analytics.dao.UserDAO;
import com.lms.analytics.models.Student;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.PasswordEncryptionUtil;
import com.lms.analytics.utils.SessionManager;

public class MyProfileController {

    @FXML private StackPane root;

    private TextField fullNameField, emailField, majorField, phoneField;
    private PasswordField oldPassField, newPassField, confirmPassField;
    private ProgressBar passStrengthBar;
    private Label passStrengthLabel;
    private Label feedbackLabel;

    private final UserDAO    userDAO    = new UserDAO();
    private final StudentDAO studentDAO = new StudentDAO();
    private User    currentUser;
    private Student currentStudent;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null)
            currentStudent = studentDAO.getStudentByUserId(currentUser.getUserId());
        buildUI();
        loadProfile();
    }

    private void buildUI() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#f0f2f5; -fx-background:#f0f2f5;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox page = new VBox(20);
        page.setPadding(new Insets(24, 32, 32, 32));
        page.setMaxWidth(Double.MAX_VALUE);
        page.setStyle("-fx-background-color:#f0f2f5;");
        scroll.setContent(page);

        // ── Page header ───────────────────────────────────────────────
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> com.lms.analytics.utils.NavigationUtil.backToDashboard(root));
        Label title = new Label("👤  My Profile");
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        headerRow.getChildren().addAll(backBtn, title);

        // ── Feedback banner ───────────────────────────────────────────
        feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);
        feedbackLabel.setMaxWidth(Double.MAX_VALUE);
        feedbackLabel.setVisible(false);
        feedbackLabel.setManaged(false);

        // ── Profile header card ───────────────────────────────────────
        HBox profileCard = buildProfileCard();

        // ── Two-column layout: Edit Profile | Change Password ─────────
        HBox twoCol = new HBox(20);
        twoCol.setMaxWidth(Double.MAX_VALUE);

        VBox editCard   = buildEditProfileCard();
        VBox passCard   = buildChangePasswordCard();
        HBox.setHgrow(editCard, Priority.ALWAYS);
        HBox.setHgrow(passCard, Priority.ALWAYS);
        twoCol.getChildren().addAll(editCard, passCard);

        page.getChildren().addAll(headerRow, feedbackLabel, profileCard, twoCol);
        root.getChildren().add(scroll);
    }

    // ── PROFILE HEADER CARD ───────────────────────────────────────────
    private HBox buildProfileCard() {
        HBox card = new HBox(24);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(24));
        card.setMaxWidth(Double.MAX_VALUE);

        // Consistent light blue for all roles
        String accentColor = "#38bdf8";
        String accentDark  = "#0284c7";

        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:12; " +
            "-fx-border-color:" + accentColor + "; -fx-border-width:0 0 0 5; " +
            "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.15),10,0,0,3);");

        // ── Avatar ────────────────────────────────────────────────────
        StackPane avatarPane = new StackPane();
        Circle circle = new Circle(44);
        circle.setStyle("-fx-fill:" + accentColor + ";");
        String initials = currentUser != null && !currentUser.getFullName().isEmpty()
            ? String.valueOf(currentUser.getFullName().charAt(0)).toUpperCase() : "U";
        Label initLbl = new Label(initials);
        initLbl.setStyle("-fx-font-size:32px; -fx-font-weight:bold; -fx-text-fill:white;");
        avatarPane.getChildren().addAll(circle, initLbl);

        // ── User info ─────────────────────────────────────────────────
        VBox infoBox = new VBox(6);
        infoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);

        if (currentUser != null) {
            Label nameLbl = new Label(currentUser.getFullName());
            nameLbl.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

            Label usernameLbl = new Label("@" + currentUser.getUsername());
            usernameLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#64748b;");

            Label emailLbl = new Label(currentUser.getEmail());
            emailLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#64748b;");

            // Role badge — consistent light blue for all roles
            String role = currentUser.getRole();
            Label roleBadge = new Label("● " + role);
            roleBadge.setStyle(
                "-fx-background-color:#e0f2fe; -fx-text-fill:#0284c7; " +
                "-fx-font-size:11px; -fx-font-weight:bold; " +
                "-fx-background-radius:20; -fx-padding:4 14;");

            infoBox.getChildren().addAll(nameLbl, usernameLbl, emailLbl, roleBadge);

            // Extra info for students
            if (currentStudent != null) {
                Label stuInfo = new Label(
                    "Student #: " + currentStudent.getStudentNumber() +
                    "   ·   Semester: " + currentStudent.getCurrentSemester() +
                    (currentStudent.getMajor() != null ? "   ·   " + currentStudent.getMajor() : ""));
                stuInfo.setStyle("-fx-font-size:12px; -fx-text-fill:#94a3b8;");
                infoBox.getChildren().add(stuInfo);
            }
        }

        // ── Stats strip ───────────────────────────────────────────────
        VBox statsBox = new VBox(4);
        statsBox.setAlignment(Pos.CENTER_RIGHT);
        statsBox.setPadding(new Insets(0, 8, 0, 0));

        String role = currentUser != null ? currentUser.getRole() : "";
        Label roleIcon = new Label(
            "ADMIN".equals(role) ? "🛡" : "INSTRUCTOR".equals(role) ? "🎓" : "📚");
        roleIcon.setStyle("-fx-font-size:32px;");

        Label roleDesc = new Label(
            "ADMIN".equals(role) ? "System Admin" :
            "INSTRUCTOR".equals(role) ? "Course Instructor" : "Student");
        roleDesc.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");

        statsBox.getChildren().addAll(roleIcon, roleDesc);

        card.getChildren().addAll(avatarPane, infoBox, statsBox);
        return card;
    }

    // ── EDIT PROFILE CARD ─────────────────────────────────────────────
    private VBox buildEditProfileCard() {
        VBox card = sectionCard("✏️  Edit Profile");

        fullNameField = formField("e.g. John Doe");
        emailField    = formField("e.g. john@email.com");
        phoneField    = formField("e.g. +251 911 000 000");

        card.getChildren().addAll(
            fieldRow("Full Name *",  fullNameField),
            fieldRow("Email *",      emailField),
            fieldRow("Phone",        phoneField)
        );

        // Major only for students
        if (currentUser != null && "STUDENT".equals(currentUser.getRole())) {
            majorField = formField("e.g. Computer Science");
            card.getChildren().add(fieldRow("Major / Field of Study", majorField));
        }

        Button saveBtn = new Button("💾  Save Profile");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.getStyleClass().addAll("button", "btn-primary");
        saveBtn.setStyle("-fx-padding:11 0; -fx-font-size:14px;");
        saveBtn.setOnAction(e -> handleSaveProfile());

        card.getChildren().add(saveBtn);
        return card;
    }

    // ── CHANGE PASSWORD CARD ──────────────────────────────────────────
    private VBox buildChangePasswordCard() {
        VBox card = sectionCard("🔒  Change Password");

        oldPassField     = passField("Enter current password");
        newPassField     = passField("Min 8 chars, mixed case + number");
        confirmPassField = passField("Re-enter new password");

        // Password strength bar
        passStrengthBar = new ProgressBar(0);
        passStrengthBar.setMaxWidth(Double.MAX_VALUE);
        passStrengthBar.setPrefHeight(6);
        passStrengthBar.setStyle("-fx-accent:#ef4444;");

        passStrengthLabel = new Label("Password strength");
        passStrengthLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");

        newPassField.textProperty().addListener((o, old, v) -> updateStrength(v));

        // Match indicator
        Label matchLabel = new Label();
        matchLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");
        confirmPassField.textProperty().addListener((o, old, v) -> {
            if (v.isEmpty()) { matchLabel.setText(""); return; }
            if (v.equals(newPassField.getText())) {
                matchLabel.setText("✓ Passwords match");
                matchLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#16a34a;");
            } else {
                matchLabel.setText("✗ Passwords do not match");
                matchLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#dc2626;");
            }
        });

        Label hint = new Label("Min 8 chars · Uppercase · Lowercase · Number · Special char");
        hint.setStyle("-fx-font-size:10px; -fx-text-fill:#94a3b8;");
        hint.setWrapText(true);

        Button changeBtn = new Button("🔒  Change Password");
        changeBtn.setMaxWidth(Double.MAX_VALUE);
        changeBtn.getStyleClass().addAll("button", "btn-primary");
        changeBtn.setStyle("-fx-padding:11 0; -fx-font-size:14px;");
        changeBtn.setOnAction(e -> handleChangePassword());

        card.getChildren().addAll(
            fieldRow("Current Password", oldPassField),
            fieldRow("New Password",     newPassField),
            passStrengthBar, passStrengthLabel,
            fieldRow("Confirm Password", confirmPassField),
            matchLabel, hint,
            changeBtn
        );
        return card;
    }

    // ── DATA ──────────────────────────────────────────────────────────
    private void loadProfile() {
        if (currentUser == null) return;
        fullNameField.setText(currentUser.getFullName() != null ? currentUser.getFullName() : "");
        emailField.setText(currentUser.getEmail() != null ? currentUser.getEmail() : "");
        if (currentStudent != null) {
            if (majorField != null)
                majorField.setText(currentStudent.getMajor() != null ? currentStudent.getMajor() : "");
            if (phoneField != null)
                phoneField.setText(currentStudent.getPhone() != null ? currentStudent.getPhone() : "");
        }
    }

    private void handleSaveProfile() {
        if (currentUser == null) return;
        String name  = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        if (name.isEmpty())  { showError("Full Name is required."); return; }
        if (!com.lms.analytics.utils.PasswordEncryptionUtil.isValidEmail(email)) {
            showError("Invalid email. Use format: user@domain.com"); return;
        }

        currentUser.setFullName(name);
        currentUser.setEmail(email);

        if (userDAO.updateUser(currentUser)) {
            if (currentStudent != null) {
                if (majorField != null) currentStudent.setMajor(majorField.getText().trim());
                if (phoneField != null) currentStudent.setPhone(phoneField.getText().trim());
                studentDAO.updateStudent(currentStudent);
            }
            SessionManager.getInstance().startSession(currentUser);
            showSuccess("✓  Profile updated successfully.");
        } else {
            showError("Failed to update profile.");
        }
    }

    private void handleChangePassword() {
        String oldPass = oldPassField.getText();
        String newPass = newPassField.getText();
        String confirm = confirmPassField.getText();

        if (oldPass.isEmpty()) { showError("Enter your current password."); return; }
        if (!newPass.equals(confirm)) { showError("New passwords do not match."); return; }
        if (!PasswordEncryptionUtil.isStrongPassword(newPass)) {
            showError("Password too weak. Min 8 chars, mixed case + number + symbol."); return;
        }
        if (!PasswordEncryptionUtil.verifyPassword(oldPass, currentUser.getPasswordHash())) {
            showError("Current password is incorrect."); return;
        }
        if (userDAO.changePassword(currentUser.getUserId(), newPass)) {
            currentUser.setPasswordHash(PasswordEncryptionUtil.hashPassword(newPass));
            oldPassField.clear(); newPassField.clear(); confirmPassField.clear();
            passStrengthBar.setProgress(0);
            passStrengthLabel.setText("Password strength");
            showSuccess("✓  Password changed successfully.");
        } else {
            showError("Failed to change password.");
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private VBox sectionCard(String title) {
        VBox card = new VBox(12);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:12; " +
            "-fx-border-color:#e2e8f0; -fx-border-radius:12; -fx-border-width:1; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        card.getChildren().addAll(lbl, new Separator());
        return card;
    }

    /** A label + field stacked vertically */
    private VBox fieldRow(String labelText, javafx.scene.Node field) {
        VBox row = new VBox(4);
        Label lbl = new Label(labelText);
        lbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");
        row.getChildren().addAll(lbl, field);
        return row;
    }

    private TextField formField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private PasswordField passField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.getStyleClass().add("text-field");
        pf.setMaxWidth(Double.MAX_VALUE);
        return pf;
    }

    private void updateStrength(String password) {
        int score = 0;
        if (password.length() >= 8) score++;
        if (password.matches(".*[A-Z].*")) score++;
        if (password.matches(".*[a-z].*")) score++;
        if (password.matches(".*\\d.*")) score++;
        if (password.matches(".*[^a-zA-Z0-9].*")) score++;
        passStrengthBar.setProgress(score / 5.0);
        String color, text;
        if (score <= 1)      { color = "#ef4444"; text = "Very Weak"; }
        else if (score == 2) { color = "#f97316"; text = "Weak"; }
        else if (score == 3) { color = "#eab308"; text = "Fair"; }
        else if (score == 4) { color = "#22c55e"; text = "Strong"; }
        else                 { color = "#16a34a"; text = "Very Strong ✓"; }
        passStrengthBar.setStyle("-fx-accent:" + color + ";");
        passStrengthLabel.setText("Strength: " + text);
        passStrengthLabel.setStyle("-fx-font-size:11px; -fx-text-fill:" + color + ";");
    }

    private void showError(String msg) {
        feedbackLabel.setText("⚠  " + msg);
        feedbackLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#dc2626; " +
            "-fx-background-color:#fee2e2; -fx-background-radius:8; -fx-padding:10 14;");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        feedbackLabel.setText(msg);
        feedbackLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#16a34a; " +
            "-fx-background-color:#dcfce7; -fx-background-radius:8; -fx-padding:10 14;");
        feedbackLabel.setVisible(true);
        feedbackLabel.setManaged(true);
    }
}
