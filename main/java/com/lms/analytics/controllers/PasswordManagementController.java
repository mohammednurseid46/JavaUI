package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.lms.analytics.dao.PasswordHistoryDAO;
import com.lms.analytics.dao.UserDAO;
import com.lms.analytics.models.PasswordHistory;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.DataGridHelper;

import java.util.List;
import java.util.stream.Stream;

public class PasswordManagementController {

    @FXML private StackPane root;

    private TableView<User> table;
    private DataGridHelper<User> grid;
    private TextField searchField;
    private ComboBox<String> roleFilter;
    private Label statusLabel;

    // Right panel fields
    private Label userInfoLabel;
    private Label passwordDisplayLabel;
    private PasswordField newPassField;
    private PasswordField confirmPassField;
    private Label strengthLabel;
    private ProgressBar strengthBar;

    private final UserDAO            userDAO     = new UserDAO();
    private final PasswordHistoryDAO historyDAO  = new PasswordHistoryDAO();
    private final ObservableList<User> userList  = FXCollections.observableArrayList();

    // History table in right panel
    private TableView<PasswordHistory> historyTable;

    @FXML
    public void initialize() { buildUI(); loadUsers(); }

    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");

        // ── Top bar ───────────────────────────────────────────────────
        VBox top = new VBox(10);
        top.setPadding(new Insets(16, 20, 12, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> com.lms.analytics.utils.NavigationUtil.backToDashboard(table));
        Label title = new Label("🔑  Password Management");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        titleRow.getChildren().addAll(backBtn, title);

        Label subtitle = new Label("Select a student or instructor to view and manage their password.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        searchField = new TextField();
        searchField.setPromptText("🔍  Search by name, username or email...");
        searchField.setPrefWidth(320);
        searchField.textProperty().addListener((o, old, v) -> applyFilter());

        roleFilter = new ComboBox<>();
        roleFilter.setItems(FXCollections.observableArrayList("ALL", "STUDENT", "INSTRUCTOR"));
        roleFilter.setValue("ALL");
        roleFilter.setOnAction(e -> applyFilter());

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#38bdf8; -fx-font-weight:bold;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        // Sort button will be added after grid is created
        filterRow.getChildren().addAll(searchField, roleFilter, sp, statusLabel);
        top.getChildren().addAll(titleRow, subtitle, filterRow);
        layout.setTop(top);

        // ── User table ────────────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No users found."));

        TableColumn<User, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFullName()));
        nameCol.setSortable(true);

        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsername()));
        userCol.setSortable(true);

        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEmail()));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getRole()));
        roleCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) { setText(null); setStyle(""); return; }
                setText(r);
                setStyle("INSTRUCTOR".equals(r)
                    ? "-fx-text-fill:#1d4ed8; -fx-font-weight:bold;"
                    : "-fx-text-fill:#0284c7; -fx-font-weight:bold;");
            }
        });

        TableColumn<User, String> activeCol = new TableColumn<>("Status");
        activeCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().isActive() ? "✓ Active" : "✗ Inactive"));
        activeCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(s.startsWith("✓")
                    ? "-fx-text-fill:#16a34a; -fx-font-weight:bold;"
                    : "-fx-text-fill:#dc2626; -fx-font-weight:bold;");
            }
        });

        // Password column — shows plain password directly
        TableColumn<User, String> passCol = new TableColumn<>("Password");
        passCol.setCellValueFactory(cd -> {
            String plain = cd.getValue().getPlainPassword();
            return new SimpleStringProperty(
                (plain != null && !plain.isBlank()) ? plain : "—");
        });
        passCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("—".equals(s)
                    ? "-fx-text-fill:#94a3b8; -fx-font-style:italic;"
                    : "-fx-text-fill:#92400e; -fx-font-weight:bold; -fx-background-color:#fefce8;");
            }
        });

        table.getColumns().addAll(nameCol, userCol, emailCol, roleCol, activeCol, passCol);
        table.getSelectionModel().selectedItemProperty().addListener(
            (o, old, u) -> { if (u != null) onUserSelected(u); });

        // DataGrid: sorting + filtering + pagination
        grid = new DataGridHelper<>(table);

        // Sort options — Name, Username, Role, Status
        grid.addSortOption("Name",     java.util.Comparator.comparing(
            u -> u.getFullName() != null ? u.getFullName() : ""));
        grid.addSortOption("Username", java.util.Comparator.comparing(
            u -> u.getUsername() != null ? u.getUsername() : ""));
        grid.addSortOption("Role",     java.util.Comparator.comparing(
            u -> u.getRole() != null ? u.getRole() : ""));
        grid.addSortOption("Status",   java.util.Comparator.comparing(
            u -> u.isActive() ? "Active" : "Inactive"));

        // Add sort button to filter row now that grid is ready
        filterRow.getChildren().add(grid.buildSortButton());

        HBox paging = grid.buildPaginationBar();

        VBox tableBox = new VBox(0, table, paging);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setCenter(tableBox);

        // ── Right panel ───────────────────────────────────────────────
        ScrollPane rightScroll = new ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setPrefWidth(290);
        rightScroll.setStyle("-fx-background-color:white; -fx-background:white;");

        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setStyle("-fx-background-color:white;");

        // ── User info + password — single merged card ─────────────────
        Label panelTitle = new Label("👤  Selected User");
        panelTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        // One card that shows all user info + password together
        userInfoLabel = new Label("← Select a user from the table to see their details and password.");
        userInfoLabel.setWrapText(true);
        userInfoLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:12;");

        // Password display — shown inside the card area, updated on selection
        passwordDisplayLabel = new Label();
        passwordDisplayLabel.setWrapText(true);
        passwordDisplayLabel.setVisible(false);
        passwordDisplayLabel.setManaged(false);

        // ── Change password ───────────────────────────────────────────
        Separator sep2 = new Separator();
        Label changeTitle = new Label("🔑  Change Password");
        changeTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label newPassLbl = new Label("New Password");
        newPassLbl.getStyleClass().add("form-label");
        newPassField = new PasswordField();
        newPassField.setPromptText("Enter new password");
        newPassField.getStyleClass().add("text-field");
        newPassField.setMaxWidth(Double.MAX_VALUE);
        newPassField.textProperty().addListener((o, old, v) -> updateStrength(v));

        strengthBar = new ProgressBar(0);
        strengthBar.setMaxWidth(Double.MAX_VALUE);
        strengthBar.setPrefHeight(6);
        strengthBar.setStyle("-fx-accent:#ef4444;");

        strengthLabel = new Label("Password strength");
        strengthLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");

        Label confirmLbl = new Label("Confirm Password");
        confirmLbl.getStyleClass().add("form-label");
        confirmPassField = new PasswordField();
        confirmPassField.setPromptText("Confirm new password");
        confirmPassField.getStyleClass().add("text-field");
        confirmPassField.setMaxWidth(Double.MAX_VALUE);

        Button setPassBtn = new Button("💾  Set New Password");
        setPassBtn.setMaxWidth(Double.MAX_VALUE);
        setPassBtn.getStyleClass().addAll("button", "btn-primary");
        setPassBtn.setStyle("-fx-padding:10 0;");
        setPassBtn.setOnAction(e -> handleSetPassword());

        // ── Quick actions ─────────────────────────────────────────────
        Separator sep3 = new Separator();
        Label quickTitle = new Label("⚡  Quick Actions");
        quickTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Button tempPassBtn = new Button("🎲  Generate Temp Password");
        tempPassBtn.setMaxWidth(Double.MAX_VALUE);
        tempPassBtn.getStyleClass().addAll("button", "btn-primary");
        tempPassBtn.setStyle("-fx-padding:10 0;");
        tempPassBtn.setOnAction(e -> handleGenerateTempPassword());

        Button defaultPassBtn = new Button("🔄  Reset to Default");
        defaultPassBtn.setMaxWidth(Double.MAX_VALUE);
        defaultPassBtn.getStyleClass().addAll("button", "btn-primary");
        defaultPassBtn.setStyle("-fx-padding:10 0;");
        defaultPassBtn.setOnAction(e -> handleResetToDefault());

        Label hintLbl = new Label("Default: Student@123 / Instructor@123");
        hintLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#94a3b8;");

        panel.getChildren().addAll(
            panelTitle, userInfoLabel,
            sep2, changeTitle,
            newPassLbl, newPassField, strengthBar, strengthLabel,
            confirmLbl, confirmPassField, setPassBtn,
            sep3, quickTitle,
            tempPassBtn, defaultPassBtn, hintLbl,
            new Separator(), buildHistorySection(),
            new Separator(), buildEncryptionDemoSection()
        );

        rightScroll.setContent(panel);
        layout.setRight(rightScroll);
        root.getChildren().add(layout);
    }

    // ── DATA ──────────────────────────────────────────────────────────
    private void loadUsers() {
        List<User> users = Stream.concat(
            userDAO.getUsersByRole("STUDENT").stream(),
            userDAO.getUsersByRole("INSTRUCTOR").stream()
        ).toList();
        grid.setData(users);
        statusLabel.setText("Total: " + grid.getTotalFiltered() + " users");
    }

    private void applyFilter() {
        String role = roleFilter.getValue() != null ? roleFilter.getValue() : "ALL";
        String kw   = searchField.getText().trim().toLowerCase();

        List<User> base;
        if ("ALL".equals(role)) {
            base = Stream.concat(
                userDAO.getUsersByRole("STUDENT").stream(),
                userDAO.getUsersByRole("INSTRUCTOR").stream()
            ).toList();
        } else {
            base = userDAO.getUsersByRole(role);
        }

        grid.setData(base);

        if (!kw.isEmpty()) {
            grid.setFilter(u ->
                (u.getFullName() != null && u.getFullName().toLowerCase().contains(kw)) ||
                (u.getUsername() != null && u.getUsername().toLowerCase().contains(kw)) ||
                (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw)));
        } else {
            grid.clearFilter();
        }

        statusLabel.setText("Found: " + grid.getTotalFiltered());
    }

    // ── ON USER SELECTED — show password immediately ──────────────────
    private void onUserSelected(User u) {
        String plain = u.getPlainPassword();
        String passwordLine = (plain != null && !plain.isBlank())
            ? plain
            : "Not on record yet";

        int changeCount = historyDAO.getChangeCount(u.getUserId());

        // Single card with all info + password + change count
        userInfoLabel.setText(
            "Name:      " + u.getFullName() + "\n" +
            "Username:  " + u.getUsername() + "\n" +
            "Email:     " + u.getEmail() + "\n" +
            "Role:      " + u.getRole() + "\n" +
            "Status:    " + (u.isActive() ? "✓ Active" : "✗ Inactive") + "\n" +
            "Changes:   " + changeCount + " password change(s)\n" +
            "─────────────────────\n" +
            "Password:  " + passwordLine
        );

        // Style based on whether password is available
        if (plain != null && !plain.isBlank()) {
            userInfoLabel.setStyle(
                "-fx-font-size:12px; -fx-text-fill:#334155; " +
                "-fx-background-color:#fefce8; -fx-background-radius:8; " +
                "-fx-border-color:#fde047; -fx-border-radius:8; -fx-padding:12; " +
                "-fx-font-family:'Courier New', monospace;");
        } else {
            userInfoLabel.setStyle(
                "-fx-font-size:12px; -fx-text-fill:#334155; " +
                "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
                "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:12;");
        }

        // Load password history into the history table
        if (historyTable != null) {
            java.util.List<PasswordHistory> history = historyDAO.getHistoryByUser(u.getUserId());
            historyTable.setItems(FXCollections.observableArrayList(history));
        }

        // Clear change password fields
        newPassField.clear();
        confirmPassField.clear();
        strengthBar.setProgress(0);
        strengthLabel.setText("Password strength");
        strengthLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");
    }

    // ── ACTIONS ───────────────────────────────────────────────────────
    private void handleSetPassword() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { setStatus("⚠ Select a user first.", false); return; }

        String newPass = newPassField.getText();
        String confirm = confirmPassField.getText();

        if (newPass.isEmpty()) { setStatus("⚠ Enter a new password.", false); return; }
        if (!newPass.equals(confirm)) { setStatus("⚠ Passwords do not match.", false); return; }
        if (newPass.length() < 6) { setStatus("⚠ Minimum 6 characters.", false); return; }

        if (userDAO.changePassword(u.getUserId(), newPass)) {
            newPassField.clear();
            confirmPassField.clear();
            setStatus("✓ Password changed for " + u.getFullName(), true);
            User refreshed = userDAO.getUserById(u.getUserId());
            if (refreshed != null) { onUserSelected(refreshed); }
            loadUsers();
        } else {
            setStatus("⚠ Failed to change password.", false);
        }
    }

    private void handleGenerateTempPassword() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { setStatus("⚠ Select a user first.", false); return; }

        String tempPass = generateTempPassword();
        if (userDAO.changePassword(u.getUserId(), tempPass)) {
            setStatus("✓ Temp password set for " + u.getFullName(), true);
            User refreshed = userDAO.getUserById(u.getUserId());
            if (refreshed != null) { onUserSelected(refreshed); }
            loadUsers();
        } else {
            setStatus("⚠ Failed to generate temp password.", false);
        }
    }

    private void handleResetToDefault() {
        User u = table.getSelectionModel().getSelectedItem();
        if (u == null) { setStatus("⚠ Select a user first.", false); return; }

        String defaultPass = "INSTRUCTOR".equals(u.getRole()) ? "Instructor@123" : "Student@123";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Reset password for " + u.getFullName() + " to:\n\n" + defaultPass,
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Reset to Default");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (userDAO.changePassword(u.getUserId(), defaultPass)) {
                    setStatus("✓ Reset to default for " + u.getFullName(), true);
                    User refreshed = userDAO.getUserById(u.getUserId());
                    if (refreshed != null) { onUserSelected(refreshed); }
                    loadUsers();
                } else {
                    setStatus("⚠ Reset failed.", false);
                }
            }
        });
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private String generateTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#!";
        java.util.Random rnd = new java.util.Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
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

    private void setStatus(String msg, boolean success) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:"
            + (success ? "#16a34a;" : "#dc2626;"));
    }

    // ── Password History Section ──────────────────────────────────────
    private javafx.scene.Node buildHistorySection() {
        VBox section = new VBox(8);
        section.setPadding(new Insets(10));
        section.setStyle(
            "-fx-background-color:#f8fafc; -fx-background-radius:8; " +
            "-fx-border-color:#e2e8f0; -fx-border-radius:8; -fx-border-width:1;");

        Label histTitle = new Label("🕐  Password Change History");
        histTitle.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label histHint = new Label("Shows all password changes for the selected user.");
        histHint.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");
        histHint.setWrapText(true);

        // History table
        historyTable = new TableView<>();
        historyTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        historyTable.setPrefHeight(200);
        historyTable.setPlaceholder(new Label("No history — select a user."));

        TableColumn<PasswordHistory, String> dateCol = new TableColumn<>("Changed At");
        dateCol.setCellValueFactory(cd -> {
            java.time.LocalDateTime dt = cd.getValue().getChangedAt();
            String text = dt != null
                ? dt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                : "—";
            return new SimpleStringProperty(text);
        });

        TableColumn<PasswordHistory, String> reasonCol = new TableColumn<>("Reason");
        reasonCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getChangeReasonLabel()));
        reasonCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                String color = switch (s) {
                    case "Account Created" -> "-fx-text-fill:#16a34a; -fx-font-weight:bold;";
                    case "Admin Reset"     -> "-fx-text-fill:#dc2626; -fx-font-weight:bold;";
                    case "Temp Password"   -> "-fx-text-fill:#d97706; -fx-font-weight:bold;";
                    default                -> "-fx-text-fill:#0284c7; -fx-font-weight:bold;";
                };
                setStyle(color);
            }
        });

        // BCrypt hash column — shows the stored hash from password_history table
        TableColumn<PasswordHistory, String> hashCol = new TableColumn<>("BCrypt Hash");
        hashCol.setCellValueFactory(cd -> {
            String hash = cd.getValue().getPasswordHash();
            // Show first 20 chars + "..." to keep the column readable
            if (hash != null && hash.length() > 20) {
                return new SimpleStringProperty(hash.substring(0, 20) + "...");
            }
            return new SimpleStringProperty(hash != null ? hash : "—");
        });
        hashCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); setTooltip(null); return; }
                setText(s);
                setStyle(
                    "-fx-font-family:'Courier New', monospace; " +
                    "-fx-font-size:10px; " +
                    "-fx-text-fill:#16a34a;");
                // Full hash shown in tooltip on hover
                PasswordHistory ph = getTableView().getItems().get(getIndex());
                if (ph != null && ph.getPasswordHash() != null) {
                    Tooltip tip = new Tooltip(ph.getPasswordHash());
                    tip.setStyle(
                        "-fx-font-family:'Courier New', monospace; " +
                        "-fx-font-size:11px; " +
                        "-fx-background-color:#1e293b; " +
                        "-fx-text-fill:#4ade80; " +
                        "-fx-padding:8 12;");
                    tip.setWrapText(true);
                    tip.setMaxWidth(500);
                    setTooltip(tip);
                }
            }
        });
        hashCol.setPrefWidth(140);

        TableColumn<PasswordHistory, String> currentCol = new TableColumn<>("Current");
        currentCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().isCurrent() ? "✓ Yes" : "No"));
        currentCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(s.startsWith("✓")
                    ? "-fx-text-fill:#16a34a; -fx-font-weight:bold;"
                    : "-fx-text-fill:#94a3b8;");
            }
        });

        historyTable.getColumns().addAll(dateCol, reasonCol, hashCol, currentCol);

        section.getChildren().addAll(histTitle, histHint, historyTable);
        return section;
    }

    // ── BCrypt Encryption Demo Section ───────────────────────────────
    private javafx.scene.Node buildEncryptionDemoSection() {
        VBox demo = new VBox(10);
        demo.setPadding(new Insets(12));
        demo.setStyle(
            "-fx-background-color:#f8fafc; -fx-background-radius:8; " +
            "-fx-border-color:#e2e8f0; -fx-border-radius:8; -fx-border-width:1;");

        Label demoTitle = new Label("🔐  Password Encryption (BCrypt)");
        demoTitle.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label demoInfo = new Label(
            "This project uses BCrypt (workload=12) — a one-way hashing algorithm.\n" +
            "Plain text → BCrypt hash → stored in DB.\n" +
            "Verification: BCrypt.checkpw(input, storedHash)");
        demoInfo.setWrapText(true);
        demoInfo.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");

        // Input field
        Label inputLbl = new Label("Type any password to see its BCrypt hash:");
        inputLbl.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        TextField demoInput = new TextField();
        demoInput.setPromptText("e.g. Student@123");
        demoInput.getStyleClass().add("text-field");
        demoInput.setMaxWidth(Double.MAX_VALUE);

        // Hash output
        Label hashLbl = new Label("BCrypt Hash:");
        hashLbl.setStyle("-fx-font-size:11px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        TextField hashOutput = new TextField();
        hashOutput.setEditable(false);
        hashOutput.setPromptText("Hash will appear here...");
        hashOutput.setStyle(
            "-fx-background-color:#1e293b; -fx-text-fill:#4ade80; " +
            "-fx-border-color:#334155; -fx-border-radius:6; -fx-background-radius:6; " +
            "-fx-padding:8 10; -fx-font-size:10px; -fx-font-family:'Courier New';");
        hashOutput.setMaxWidth(Double.MAX_VALUE);

        // Strength indicator
        Label strengthInfoLbl = new Label();
        strengthInfoLbl.setWrapText(true);
        strengthInfoLbl.setStyle("-fx-font-size:11px;");

        // Verify result
        Label verifyLbl = new Label();
        verifyLbl.setWrapText(true);
        verifyLbl.setStyle("-fx-font-size:11px;");

        // Hash button
        Button hashBtn = new Button("🔒  Generate Hash");
        hashBtn.getStyleClass().addAll("button", "btn-primary");
        hashBtn.setStyle("-fx-padding:7 14; -fx-font-size:11px;");
        hashBtn.setMaxWidth(Double.MAX_VALUE);
        hashBtn.setOnAction(e -> {
            String pwd = demoInput.getText().trim();
            if (pwd.isEmpty()) {
                hashOutput.setText("Enter a password first.");
                return;
            }
            // Generate BCrypt hash
            String hash = com.lms.analytics.utils.PasswordEncryptionUtil.hashPassword(pwd);
            hashOutput.setText(hash);

            // Check strength
            boolean strong = com.lms.analytics.utils.PasswordEncryptionUtil.isStrongPassword(pwd);
            boolean hasUpper   = pwd.matches(".*[A-Z].*");
            boolean hasLower   = pwd.matches(".*[a-z].*");
            boolean hasDigit   = pwd.matches(".*\\d.*");
            boolean hasSpecial = pwd.matches(".*[^a-zA-Z0-9].*");
            boolean hasLength  = pwd.length() >= 8;

            strengthInfoLbl.setText(
                "Strength Criteria:\n" +
                (hasLength  ? "✓" : "✗") + " Min 8 characters (" + pwd.length() + " chars)\n" +
                (hasUpper   ? "✓" : "✗") + " Uppercase letter\n" +
                (hasLower   ? "✓" : "✗") + " Lowercase letter\n" +
                (hasDigit   ? "✓" : "✗") + " Number\n" +
                (hasSpecial ? "✓" : "✗") + " Special character\n" +
                "Result: " + (strong ? "✅ STRONG — accepted" : "❌ WEAK — rejected at signup")
            );
            strengthInfoLbl.setStyle("-fx-font-size:11px; -fx-text-fill:"
                + (strong ? "#16a34a;" : "#dc2626;"));

            // Verify the hash immediately to prove it works
            boolean verified = com.lms.analytics.utils.PasswordEncryptionUtil
                .verifyPassword(pwd, hash);
            verifyLbl.setText("Verification: BCrypt.checkpw(\"" + pwd + "\", hash) → " + verified);
            verifyLbl.setStyle("-fx-font-size:11px; -fx-text-fill:" +
                (verified ? "#16a34a;" : "#dc2626;"));
        });

        demo.getChildren().addAll(
            demoTitle, demoInfo, new Separator(),
            inputLbl, demoInput, hashBtn,
            hashLbl, hashOutput,
            strengthInfoLbl, verifyLbl
        );
        return demo;
    }
}
