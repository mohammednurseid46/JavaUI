package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.lms.analytics.dao.UserDAO;
import com.lms.analytics.dao.CourseDAO;
import com.lms.analytics.models.User;
import com.lms.analytics.models.Course;
import com.lms.analytics.utils.DataGridHelper;
import com.lms.analytics.utils.PasswordEncryptionUtil;

import java.util.List;

public class InstructorManagementController {

    @FXML private StackPane root;

    private TableView<User> table;
    private DataGridHelper<User> grid;
    private TextField searchField;
    private TextField fullNameField, usernameField, emailField;
    private PasswordField passwordField;
    private Label statusLabel;
    private Label formFeedbackLabel;

    // Status indicator card — shows current instructor status in the right panel
    private Label statusCardLabel;

    // Two separate buttons — only one visible at a time based on selected instructor
    private Button activateBtn;
    private Button deactivateBtn;

    private final UserDAO   userDAO   = new UserDAO();
    private final CourseDAO courseDAO = new CourseDAO();
    private final ObservableList<User> instructorList = FXCollections.observableArrayList();

    @FXML
    public void initialize() { buildUI(); loadInstructors(); }

    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");

        // ── Top bar ───────────────────────────────────────────────────
        VBox top = new VBox(10);
        top.setPadding(new Insets(16, 20, 12, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        Button backBtn = new Button("← Back");
        String backNormal = "-fx-background-color:transparent; -fx-text-fill:#38bdf8; " +
            "-fx-font-size:13px; -fx-font-weight:bold; -fx-cursor:hand; " +
            "-fx-border-color:#38bdf8; -fx-border-radius:6; -fx-background-radius:6; -fx-padding:5 12;";
        String backHover = "-fx-background-color:#38bdf8; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-font-weight:bold; -fx-cursor:hand; " +
            "-fx-border-color:#38bdf8; -fx-border-radius:6; -fx-background-radius:6; -fx-padding:5 12; " +
            "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.4),8,0,0,2);";
        backBtn.setStyle(backNormal);
        backBtn.setOnMouseEntered(e -> backBtn.setStyle(backHover));
        backBtn.setOnMouseExited(e  -> backBtn.setStyle(backNormal));
        backBtn.setOnAction(e -> com.lms.analytics.utils.NavigationUtil.backToDashboard(table));

        Label title = new Label("👨‍🏫  Instructor Management");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        HBox titleRow = new HBox(12, backBtn, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchField = new TextField();
        searchField.setPromptText("🔍  Search by name or email...");
        searchField.setPrefWidth(300);
        searchField.textProperty().addListener((o, old, v) -> filterInstructors(v));

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill:#38bdf8; -fx-font-size:12px; -fx-font-weight:bold;");

        Button refreshBtn = new Button("↻  Refresh");
        refreshBtn.getStyleClass().addAll("button", "btn-primary");
        refreshBtn.setOnAction(e -> loadInstructors());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        searchRow.getChildren().addAll(searchField, sp, statusLabel, refreshBtn);
        top.getChildren().addAll(titleRow, searchRow);
        layout.setTop(top);

        // ── Table ─────────────────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<User, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getFullName()));

        TableColumn<User, String> userCol = new TableColumn<>("Username");
        userCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getUsername()));

        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getEmail()));

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

        TableColumn<User, String> coursesCol = new TableColumn<>("Courses");
        coursesCol.setCellValueFactory(cd -> {
            int count = courseDAO.getCoursesByInstructor(cd.getValue().getUserId()).size();
            return new SimpleStringProperty(String.valueOf(count));
        });

        table.getColumns().addAll(nameCol, userCol, emailCol, activeCol, coursesCol);
        table.getSelectionModel().selectedItemProperty().addListener(
            (o, old, u) -> { if (u != null) populateForm(u); });

        // DataGrid: sorting + filtering + pagination
        grid = new DataGridHelper<>(table);
        grid.addSortOption("Name",     java.util.Comparator.comparing(
            u -> u.getFullName() != null ? u.getFullName() : ""));
        grid.addSortOption("Username", java.util.Comparator.comparing(
            u -> u.getUsername() != null ? u.getUsername() : ""));
        grid.addSortOption("Email",    java.util.Comparator.comparing(
            u -> u.getEmail() != null ? u.getEmail() : ""));
        grid.addSortOption("Status",   java.util.Comparator.comparing(
            u -> u.isActive() ? "Active" : "Inactive"));

        HBox searchBar = grid.buildFilterBarWithSort(
            "Search by name, username or email...",
            val -> u -> (u.getFullName() != null && u.getFullName().toLowerCase().contains(val))
                     || (u.getUsername() != null && u.getUsername().toLowerCase().contains(val))
                     || (u.getEmail() != null && u.getEmail().toLowerCase().contains(val))
        );
        HBox paging = grid.buildPaginationBar();

        VBox tableBox = new VBox(0, searchBar, table, paging);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setCenter(tableBox);

        // ── Right panel ───────────────────────────────────────────────
        ScrollPane rightScroll = new ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setPrefWidth(300);
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setStyle("-fx-background-color:white; -fx-background:white;");

        VBox form = new VBox(10);
        form.setPadding(new Insets(16));
        form.setStyle("-fx-background-color:white;");

        // ── SECTION 1: Register ───────────────────────────────────────
        Label formTitle = new Label("➕  Register New Instructor");
        formTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label formSubtitle = new Label("Fill all fields to register. Select a row to edit.");
        formSubtitle.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");
        formSubtitle.setWrapText(true);

        formFeedbackLabel = new Label();
        formFeedbackLabel.setWrapText(true);
        formFeedbackLabel.setMaxWidth(Double.MAX_VALUE);
        formFeedbackLabel.setVisible(false);
        formFeedbackLabel.setManaged(false);

        fullNameField = formField("e.g. Dr. John Smith");
        usernameField = formField("Choose a username");
        emailField    = formField("instructor@email.com");

        passwordField = new PasswordField();
        passwordField.setPromptText("Password (min 8 chars)");
        passwordField.getStyleClass().add("text-field");
        passwordField.setMaxWidth(Double.MAX_VALUE);

        Button registerBtn = new Button("➕  Register Instructor");
        registerBtn.setMaxWidth(Double.MAX_VALUE);
        registerBtn.getStyleClass().addAll("button", "btn-primary");
        registerBtn.setStyle("-fx-padding:11 0; -fx-font-size:14px;");
        registerBtn.setOnAction(e -> handleRegister());

        Button clearBtn = new Button("✖  Clear Form");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.getStyleClass().addAll("button", "btn-secondary");
        clearBtn.setOnAction(e -> clearForm());

        // ── SECTION 2: Edit selected ──────────────────────────────────
        Separator sep2 = new Separator();
        Label editTitle = new Label("✏️  Edit Selected Instructor");
        editTitle.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        // Status indicator card — updates when a row is selected
        statusCardLabel = new Label("← Select an instructor from the table.");
        statusCardLabel.setWrapText(true);
        statusCardLabel.setMaxWidth(Double.MAX_VALUE);
        statusCardLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:10;");

        Button updateBtn = new Button("💾  Save Changes");
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.getStyleClass().addAll("button", "btn-primary");
        updateBtn.setOnAction(e -> handleUpdate());

        Button resetPassBtn = new Button("🔒  Reset Password");
        resetPassBtn.setMaxWidth(Double.MAX_VALUE);
        resetPassBtn.getStyleClass().addAll("button", "btn-primary");
        resetPassBtn.setOnAction(e -> handleResetPassword());

        // ── SECTION 3: Activate / Deactivate ─────────────────────────
        Separator sep3 = new Separator();
        Label accountTitle = new Label("🔐  Account Status");
        accountTitle.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label accountHint = new Label(
            "Deactivating prevents the instructor from logging in.\n" +
            "Activating restores their access.");
        accountHint.setWrapText(true);
        accountHint.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");

        // Activate button — light blue, initially hidden
        activateBtn = new Button("✅  Activate Instructor");
        activateBtn.setMaxWidth(Double.MAX_VALUE);
        activateBtn.setStyle(
            "-fx-background-color:#38bdf8; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-background-radius:8; -fx-padding:11 0; -fx-cursor:hand; " +
            "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.4),8,0,0,2);");
        activateBtn.setOnMouseEntered(e -> activateBtn.setStyle(
            "-fx-background-color:#0ea5e9; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-background-radius:8; -fx-padding:11 0; -fx-cursor:hand;"));
        activateBtn.setOnMouseExited(e -> activateBtn.setStyle(
            "-fx-background-color:#38bdf8; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-background-radius:8; -fx-padding:11 0; -fx-cursor:hand; " +
            "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.4),8,0,0,2);"));
        activateBtn.setOnAction(e -> handleActivate());
        activateBtn.setVisible(false);
        activateBtn.setManaged(false);

        // Deactivate button — light blue, initially hidden
        deactivateBtn = new Button("⊘  Deactivate Instructor");
        deactivateBtn.setMaxWidth(Double.MAX_VALUE);
        deactivateBtn.setStyle(
            "-fx-background-color:#38bdf8; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-background-radius:8; -fx-padding:11 0; -fx-cursor:hand; " +
            "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.4),8,0,0,2);");
        deactivateBtn.setOnMouseEntered(e -> deactivateBtn.setStyle(
            "-fx-background-color:#0ea5e9; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-background-radius:8; -fx-padding:11 0; -fx-cursor:hand;"));
        deactivateBtn.setOnMouseExited(e -> deactivateBtn.setStyle(
            "-fx-background-color:#38bdf8; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-background-radius:8; -fx-padding:11 0; -fx-cursor:hand; " +
            "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.4),8,0,0,2);"));
        deactivateBtn.setOnAction(e -> handleDeactivate());
        deactivateBtn.setVisible(false);
        deactivateBtn.setManaged(false);

        // ── SECTION 4: View courses ───────────────────────────────────
        Separator sep4 = new Separator();
        Button viewCoursesBtn = new Button("📚  View Assigned Courses");
        viewCoursesBtn.setMaxWidth(Double.MAX_VALUE);
        viewCoursesBtn.getStyleClass().addAll("button", "btn-primary");
        viewCoursesBtn.setOnAction(e -> handleViewCourses());

        form.getChildren().addAll(
            // Register section
            formTitle, formSubtitle, new Separator(),
            formFeedbackLabel,
            fieldLabel("Full Name *"), fullNameField,
            fieldLabel("Username *"), usernameField,
            fieldLabel("Email *"),    emailField,
            fieldLabel("Password *"), passwordField,
            registerBtn, clearBtn,

            // Edit section
            sep2, editTitle,
            statusCardLabel,
            updateBtn, resetPassBtn,

            // Activate / Deactivate section
            sep3, accountTitle, accountHint,
            activateBtn, deactivateBtn,

            // View courses
            sep4, viewCoursesBtn
        );

        rightScroll.setContent(form);
        layout.setRight(rightScroll);
        root.getChildren().add(layout);
    }

    // ── DATA ──────────────────────────────────────────────────────────
    private void loadInstructors() {
        grid.setData(userDAO.getUsersByRole("INSTRUCTOR"));
        statusLabel.setText("Total: " + grid.getTotalFiltered() + " instructors");
    }

    private void filterInstructors(String kw) {
        if (kw == null || kw.isBlank()) { grid.clearFilter(); return; }
        String val = kw.toLowerCase();
        grid.setFilter(u ->
            (u.getFullName() != null && u.getFullName().toLowerCase().contains(val)) ||
            (u.getEmail() != null && u.getEmail().toLowerCase().contains(val)) ||
            (u.getUsername() != null && u.getUsername().toLowerCase().contains(val)));
        statusLabel.setText("Found: " + grid.getTotalFiltered());
    }

    /**
     * Called every time a row is selected.
     * Populates form fields and updates the status card + activate/deactivate buttons.
     */
    private void populateForm(User u) {
        fullNameField.setText(u.getFullName() != null ? u.getFullName() : "");
        usernameField.setText(u.getUsername() != null ? u.getUsername() : "");
        emailField.setText(u.getEmail() != null ? u.getEmail() : "");
        passwordField.clear();
        hideFeedback();

        // Update status card
        int courseCount = courseDAO.getCoursesByInstructor(u.getUserId()).size();
        String statusText = u.isActive() ? "✓ Active" : "✗ Inactive";
        String statusColor = u.isActive() ? "#16a34a" : "#dc2626";
        String cardBg = u.isActive() ? "#f0fdf4" : "#fef2f2";
        String cardBorder = u.isActive() ? "#86efac" : "#fca5a5";

        statusCardLabel.setText(
            "Name:     " + u.getFullName() + "\n" +
            "Username: " + u.getUsername() + "\n" +
            "Email:    " + u.getEmail() + "\n" +
            "Status:   " + statusText + "\n" +
            "Courses:  " + courseCount + " assigned"
        );
        statusCardLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:" + cardBg + "; -fx-background-radius:8; " +
            "-fx-border-color:" + cardBorder + "; -fx-border-radius:8; -fx-padding:10;");

        // Show the correct button based on current status
        if (u.isActive()) {
            // Instructor is active → show Deactivate, hide Activate
            activateBtn.setVisible(false);
            activateBtn.setManaged(false);
            deactivateBtn.setVisible(true);
            deactivateBtn.setManaged(true);
        } else {
            // Instructor is inactive → show Activate, hide Deactivate
            deactivateBtn.setVisible(false);
            deactivateBtn.setManaged(false);
            activateBtn.setVisible(true);
            activateBtn.setManaged(true);
        }
    }

    // ── REGISTER ──────────────────────────────────────────────────────
    private void handleRegister() {
        hideFeedback();

        String name  = fullNameField.getText().trim();
        String uname = usernameField.getText().trim();
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();

        if (name.isEmpty())  { showError("Full Name is required."); return; }
        if (uname.isEmpty()) { showError("Username is required."); return; }
        if (email.isEmpty()) { showError("Email is required."); return; }
        if (pass.isEmpty())  { showError("Password is required."); return; }
        if (pass.length() < 6) { showError("Password must be at least 6 characters."); return; }
        if (!com.lms.analytics.utils.PasswordEncryptionUtil.isValidEmail(email)) {
            showError("Invalid email. Use format: user@domain.com"); return;
        }

        if (userDAO.getUserByUsername(uname) != null) {
            showError("Username '" + uname + "' is already taken. Choose another.");
            return;
        }

        User u = new User(uname,
            PasswordEncryptionUtil.hashPassword(pass),
            email, name, "INSTRUCTOR");
        u.setPlainPassword(pass);
        u.setActive(true);

        if (userDAO.createUser(u)) {
            loadInstructors();
            clearForm();
            showSuccess("✓  Instructor registered: " + name);
        } else {
            showError("Registration failed. Please try again.");
        }
    }

    // ── UPDATE ────────────────────────────────────────────────────────
    private void handleUpdate() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select an instructor from the table first."); return; }

        String name  = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        if (name.isEmpty() || email.isEmpty()) { showError("Name and Email are required."); return; }

        selected.setFullName(name);
        selected.setEmail(email);

        if (userDAO.updateUser(selected)) {
            if (!passwordField.getText().isBlank())
                userDAO.changePassword(selected.getUserId(), passwordField.getText());
            loadInstructors();
            showSuccess("✓  Updated: " + selected.getFullName());
        } else {
            showError("Update failed.");
        }
    }

    // ── RESET PASSWORD ────────────────────────────────────────────────
    private void handleResetPassword() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select an instructor first."); return; }

        TextInputDialog dlg = new TextInputDialog("Instructor@123");
        dlg.setTitle("Reset Password");
        dlg.setHeaderText("Reset password for: " + selected.getFullName());
        dlg.setContentText("New password:");
        dlg.showAndWait().ifPresent(newPass -> {
            if (newPass.length() < 6) { showError("Password too short."); return; }
            if (userDAO.changePassword(selected.getUserId(), newPass))
                showSuccess("✓  Password reset for " + selected.getFullName());
            else
                showError("Password reset failed.");
        });
    }

    // ── ACTIVATE ─────────────────────────────────────────────────────
    private void handleActivate() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select an instructor first."); return; }
        if (selected.isActive()) { showError("This instructor is already active."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Activate instructor: " + selected.getFullName() + "?\n\n" +
            "They will be able to log in and access their courses again.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Activation");
        confirm.setHeaderText("✅  Activate Instructor");

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (userDAO.activateUser(selected.getUserId())) {
                    loadInstructors();
                    // Refresh the form with updated user data
                    User refreshed = userDAO.getUserById(selected.getUserId());
                    if (refreshed != null) populateForm(refreshed);
                    showSuccess("✓  Activated: " + selected.getFullName() +
                        " — they can now log in.");
                } else {
                    showError("Activation failed. Please try again.");
                }
            }
        });
    }

    // ── DEACTIVATE ────────────────────────────────────────────────────
    private void handleDeactivate() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select an instructor first."); return; }
        if (!selected.isActive()) { showError("This instructor is already inactive."); return; }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Deactivate instructor: " + selected.getFullName() + "?\n\n" +
            "They will no longer be able to log in.\n" +
            "Their courses and data will be preserved.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Deactivation");
        confirm.setHeaderText("⊘  Deactivate Instructor");

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (userDAO.deleteUser(selected.getUserId())) {
                    loadInstructors();
                    // Refresh the form with updated user data
                    User refreshed = userDAO.getUserById(selected.getUserId());
                    if (refreshed != null) populateForm(refreshed);
                    showSuccess("✓  Deactivated: " + selected.getFullName() +
                        " — login access removed.");
                } else {
                    showError("Deactivation failed. Please try again.");
                }
            }
        });
    }

    // ── VIEW COURSES ──────────────────────────────────────────────────
    private void handleViewCourses() {
        User selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { showError("Select an instructor first."); return; }
        List<Course> courses = courseDAO.getCoursesByInstructor(selected.getUserId());
        StringBuilder sb = new StringBuilder(selected.getFullName() + " teaches:\n\n");
        if (courses.isEmpty()) {
            sb.append("No courses assigned yet.");
        } else {
            courses.forEach(c -> sb.append("• ")
                .append(c.getCourseCode()).append(" — ")
                .append(c.getCourseName()).append("\n"));
        }
        Alert info = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        info.setTitle("Assigned Courses");
        info.setHeaderText(selected.getFullName() + " — " + courses.size() + " course(s)");
        info.showAndWait();
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private void clearForm() {
        fullNameField.clear(); usernameField.clear();
        emailField.clear(); passwordField.clear();
        table.getSelectionModel().clearSelection();
        hideFeedback();
        // Reset status card and hide both buttons
        statusCardLabel.setText("← Select an instructor from the table.");
        statusCardLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:10;");
        activateBtn.setVisible(false);   activateBtn.setManaged(false);
        deactivateBtn.setVisible(false); deactivateBtn.setManaged(false);
    }

    private void showError(String msg) {
        formFeedbackLabel.setText("⚠  " + msg);
        formFeedbackLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#dc2626; " +
            "-fx-background-color:#fee2e2; -fx-background-radius:6; -fx-padding:8 10;");
        formFeedbackLabel.setVisible(true);
        formFeedbackLabel.setManaged(true);
    }

    private void showSuccess(String msg) {
        formFeedbackLabel.setText(msg);
        formFeedbackLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#16a34a; " +
            "-fx-background-color:#dcfce7; -fx-background-radius:6; -fx-padding:8 10;");
        formFeedbackLabel.setVisible(true);
        formFeedbackLabel.setManaged(true);
    }

    private void hideFeedback() {
        formFeedbackLabel.setVisible(false);
        formFeedbackLabel.setManaged(false);
    }

    private TextField formField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field");
        tf.setMaxWidth(Double.MAX_VALUE);
        return tf;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }
}
