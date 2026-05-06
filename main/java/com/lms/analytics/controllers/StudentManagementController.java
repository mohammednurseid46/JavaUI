package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;

import com.lms.analytics.dao.StudentDAO;
import com.lms.analytics.dao.UserDAO;
import com.lms.analytics.dao.EnrollmentDAO;
import com.lms.analytics.dao.CourseDAO;
import com.lms.analytics.models.Student;
import com.lms.analytics.models.User;
import com.lms.analytics.models.Course;
import com.lms.analytics.models.Enrollment;
import com.lms.analytics.utils.DataGridHelper;

import java.util.List;

public class StudentManagementController {

    @FXML private StackPane root;

    // Table
    private TableView<Student> table;
    private DataGridHelper<Student> grid;
    private TextField searchField;
    private Label statusLabel;

    // Edit form fields
    private TextField editFullNameField, editEmailField, editMajorField,
                      editPhoneField, editAddressField;
    private Spinner<Integer> semesterSpinner;
    private Label infoLabel;

    // Enrollment panel
    private ComboBox<Course> courseCombo;
    private TableView<Enrollment> enrollmentTable;

    private final StudentDAO    studentDAO    = new StudentDAO();
    private final UserDAO       userDAO       = new UserDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final CourseDAO     courseDAO     = new CourseDAO();

    private final ObservableList<Student>    studentList    = FXCollections.observableArrayList();
    private final ObservableList<Enrollment> enrollmentList = FXCollections.observableArrayList();

    @FXML
    public void initialize() { buildUI(); loadStudents(); }
    // ── BUILD UI ──────────────────────────────────────────────────────
    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");
        // Make layout fill the entire StackPane root — no height limit
        layout.setMaxWidth(Double.MAX_VALUE);
        layout.setMaxHeight(Double.MAX_VALUE);
        StackPane.setAlignment(layout, javafx.geometry.Pos.TOP_LEFT);

        // Top bar
        VBox top = new VBox(8);
        top.setPadding(new Insets(16, 20, 12, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        Label title = new Label("👥  Student Management — Registered Students");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        // Back button — interactive with hover
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
        backBtn.setOnAction(e ->
            com.lms.analytics.utils.NavigationUtil.backToDashboard(table));

        HBox titleRow = new HBox(12, backBtn, title);
        titleRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label subtitle = new Label(
            "Students who self-registered via Sign Up. Admin can edit details, reset passwords, and manage enrollments.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        subtitle.setWrapText(true);

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchField = new TextField();
        searchField.setPromptText("🔍  Search by name, student number or major...");
        searchField.setPrefWidth(360);
        searchField.setStyle("-fx-background-radius:20; -fx-border-radius:20; -fx-padding:7 14;");
        searchField.textProperty().addListener((o, old, v) -> filterStudents(v));

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill:#667eea; -fx-font-size:12px;");

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.setStyle("-fx-background-color:#1f6feb; -fx-text-fill:white; " +
            "-fx-background-radius:8; -fx-padding:6 14; -fx-cursor:hand;");
        refreshBtn.setOnAction(e -> loadStudents());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        searchRow.getChildren().addAll(searchField, sp, statusLabel, refreshBtn);
        top.getChildren().addAll(titleRow, subtitle, searchRow);
        layout.setTop(top);

        // Student table
        table = new TableView<>();
        table.setStyle("-fx-background-color:white;");
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No registered students found."));

        TableColumn<Student, String> numCol = new TableColumn<>("Student #");
        numCol.setCellValueFactory(new PropertyValueFactory<>("studentNumber"));
        numCol.setPrefWidth(100);

        TableColumn<Student, String> nameCol = new TableColumn<>("Full Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        nameCol.setPrefWidth(160);

        TableColumn<Student, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(180);

        TableColumn<Student, String> majorCol = new TableColumn<>("Major");
        majorCol.setCellValueFactory(new PropertyValueFactory<>("major"));
        majorCol.setPrefWidth(130);

        TableColumn<Student, String> semCol = new TableColumn<>("Sem.");
        semCol.setCellValueFactory(cd ->
            new SimpleStringProperty(String.valueOf(cd.getValue().getCurrentSemester())));
        semCol.setPrefWidth(50);

        TableColumn<Student, String> dateCol = new TableColumn<>("Reg. Date");
        dateCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getEnrollmentDate() != null
                ? cd.getValue().getEnrollmentDate().toString() : ""));
        dateCol.setPrefWidth(100);

        TableColumn<Student, String> coursesCol = new TableColumn<>("Courses");
        coursesCol.setCellValueFactory(cd -> {
            int count = enrollmentDAO.getEnrollmentsByStudent(cd.getValue().getStudentId()).size();
            return new SimpleStringProperty(String.valueOf(count));
        });
        coursesCol.setPrefWidth(70);

        // Account status column
        TableColumn<Student, String> activeCol = new TableColumn<>("Account");
        activeCol.setCellValueFactory(cd -> {
            User u = userDAO.getUserById(cd.getValue().getUserId());
            return new SimpleStringProperty(u != null && u.isActive() ? "✓ Active" : "✗ Inactive");
        });
        activeCol.setPrefWidth(80);
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

        table.getColumns().addAll(numCol, nameCol, emailCol, majorCol, semCol, dateCol, coursesCol, activeCol);
        table.getSelectionModel().selectedItemProperty().addListener(
            (o, old, s) -> { if (s != null) onStudentSelected(s); });

        // DataGrid: sorting + filtering + pagination
        grid = new DataGridHelper<>(table);

        // Register sort options (Name, Major, Semester, Registration Date, Courses)
        grid.addSortOption("Name",      java.util.Comparator.comparing(
            s -> s.getFullName() != null ? s.getFullName() : ""));
        grid.addSortOption("Major",     java.util.Comparator.comparing(
            s -> s.getMajor() != null ? s.getMajor() : ""));
        grid.addSortOption("Semester",  java.util.Comparator.comparingInt(
            com.lms.analytics.models.Student::getCurrentSemester));
        grid.addSortOption("Reg. Date", java.util.Comparator.comparing(
            s -> s.getEnrollmentDate() != null ? s.getEnrollmentDate().toString() : ""));
        grid.addSortOption("Student #", java.util.Comparator.comparing(
            s -> s.getStudentNumber() != null ? s.getStudentNumber() : ""));

        HBox searchBar = grid.buildFilterBarWithSort(
            "Search by name, student number or major...",
            val -> s -> (s.getFullName() != null && s.getFullName().toLowerCase().contains(val))
                     || (s.getStudentNumber() != null && s.getStudentNumber().toLowerCase().contains(val))
                     || (s.getMajor() != null && s.getMajor().toLowerCase().contains(val))
        );
        HBox paging = grid.buildPaginationBar();

        VBox tableBox = new VBox(0, searchBar, table, paging);
        VBox.setVgrow(table, Priority.ALWAYS);
        tableBox.setMaxHeight(Double.MAX_VALUE);

        // Right panel with tabs — NO fixed size, grows with window
        TabPane tabs = new TabPane();
        tabs.setMinWidth(300);
        tabs.setStyle("-fx-background-color:white;");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        Tab editTab   = buildEditTab();
        Tab enrollTab = buildEnrollTab();
        tabs.getTabs().addAll(editTab, enrollTab);

        // SplitPane: table left, tabs right — fully resizable, no fixed widths
        SplitPane split = new SplitPane(tableBox, tabs);
        split.setDividerPositions(0.55);
        split.setMaxHeight(Double.MAX_VALUE);
        split.setMaxWidth(Double.MAX_VALUE);
        SplitPane.setResizableWithParent(tableBox, true);
        SplitPane.setResizableWithParent(tabs, true);
        layout.setCenter(split);

        // Ensure root StackPane has no size limit
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMaxHeight(Double.MAX_VALUE);
        root.getChildren().add(layout);
    }

    // ── TAB 1: EDIT ───────────────────────────────────────────────────
    private Tab buildEditTab() {
        Tab tab = new Tab("✏  Edit Student");

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:white; -fx-background:white;");

        VBox form = new VBox(10);
        form.setPadding(new Insets(16));

        // ── Student info banner ───────────────────────────────────────
        Label formTitle = new Label("Student Management");
        formTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        infoLabel = new Label("← Select a student from the table.");
        infoLabel.setWrapText(true);
        infoLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:10;");

        // ── Edit fields ───────────────────────────────────────────────
        editFullNameField = formField("Full name");
        editEmailField    = formField("Email address");
        editMajorField    = formField("Major / Field of study");
        editPhoneField    = formField("Phone number");
        editAddressField  = formField("Address");

        semesterSpinner = new Spinner<>(1, 12, 1);
        semesterSpinner.setEditable(true);
        semesterSpinner.setMaxWidth(Double.MAX_VALUE);

        // ── CRUD Actions ──────────────────────────────────────────────
        Label crudTitle = sectionLabel("Student Actions");

        Button updateBtn = actionBtn("💾  Save Changes", "#1f6feb");
        updateBtn.setOnAction(e -> handleUpdateStudent());
        Tooltip.install(updateBtn, new Tooltip("Save edits to name, email, major, phone, address, semester"));

        Button viewEnrollBtn = actionBtn("📋  View Enrollments", "#0284c7");
        viewEnrollBtn.setOnAction(e -> handleViewEnrollments());
        Tooltip.install(viewEnrollBtn, new Tooltip("See all courses this student is enrolled in"));

        Button activateBtn = actionBtn("✅  Activate Account", "#16a34a");
        activateBtn.setOnAction(e -> handleActivate());
        Tooltip.install(activateBtn, new Tooltip("Re-activate a deactivated student account"));

        Button deactivateBtn = actionBtn("⊘  Deactivate Account", "#dc2626");
        deactivateBtn.setOnAction(e -> handleDeactivate());
        Tooltip.install(deactivateBtn, new Tooltip("Soft-delete: disables login without removing data"));

        Button deleteBtn = actionBtn("🗑  Delete Student", "#7f1d1d");
        deleteBtn.setOnAction(e -> handleDeleteStudent());
        Tooltip.install(deleteBtn, new Tooltip("Permanently remove this student and all their data"));

        // ── Password Management ───────────────────────────────────────
        Label passTitle = sectionLabel("Password Management");

        Button resetPassBtn = actionBtn("🔑  Reset Password", "#7c3aed");
        resetPassBtn.setOnAction(e -> handleResetPassword());
        Tooltip.install(resetPassBtn, new Tooltip("Set a new password for this student"));

        Button tempPassBtn = actionBtn("🎲  Generate Temporary Password", "#0891b2");
        tempPassBtn.setOnAction(e -> handleGenerateTempPassword());
        Tooltip.install(tempPassBtn, new Tooltip("Auto-generate a random temporary password and show it"));

        Button forceResetBtn = actionBtn("⚠  Force Reset on Next Login", "#b45309");
        forceResetBtn.setOnAction(e -> handleForcePasswordReset());
        Tooltip.install(forceResetBtn, new Tooltip("Student must change password when they next log in"));

        form.getChildren().addAll(
            formTitle, new Separator(), infoLabel,
            fieldLabel("Full Name"), editFullNameField,
            fieldLabel("Email"), editEmailField,
            fieldLabel("Major"), editMajorField,
            fieldLabel("Phone"), editPhoneField,
            fieldLabel("Address"), editAddressField,
            fieldLabel("Current Semester"), semesterSpinner,
            new Separator(),
            crudTitle,
            updateBtn, viewEnrollBtn, activateBtn, deactivateBtn, deleteBtn,
            new Separator(),
            passTitle,
            resetPassBtn, tempPassBtn, forceResetBtn
        );

        scroll.setContent(form);
        tab.setContent(scroll);
        return tab;
    }

    // ── TAB 2: ENROLL ─────────────────────────────────────────────────
    private Tab buildEnrollTab() {
        Tab tab = new Tab("📝  Enrollments");

        // Single ScrollPane wrapping everything — fully scrollable
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:white; -fx-background:white;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);

        VBox container = new VBox(10);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color:white;");

        // ── Section title ─────────────────────────────────────────────
        Label formTitle = new Label("📝  Manage Enrollments");
        formTitle.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        // ── Selected student info — updates when row is clicked ───────
        Label selectedInfo = new Label("← Select a student from the table first.");
        selectedInfo.setWrapText(true);
        selectedInfo.setMaxWidth(Double.MAX_VALUE);
        selectedInfo.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:6; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:6; -fx-padding:8;");

        table.getSelectionModel().selectedItemProperty().addListener((o, old, s) -> {
            if (s != null) {
                int enrollCount = enrollmentDAO.getEnrollmentsByStudent(s.getStudentId()).size();
                selectedInfo.setText(
                    "👤  " + s.getFullName() + "\n" +
                    "ID:        " + s.getStudentNumber() + "\n" +
                    "Major:     " + (s.getMajor() != null ? s.getMajor() : "—") + "\n" +
                    "Semester:  " + s.getCurrentSemester() + "\n" +
                    "Courses:   " + enrollCount
                );
                selectedInfo.setStyle(
                    "-fx-font-size:11px; -fx-text-fill:#334155; " +
                    "-fx-background-color:#f0fdf4; -fx-background-radius:6; " +
                    "-fx-border-color:#86efac; -fx-border-radius:6; -fx-padding:8;");
            } else {
                selectedInfo.setText("← Select a student from the table first.");
                selectedInfo.setStyle(
                    "-fx-font-size:11px; -fx-text-fill:#334155; " +
                    "-fx-background-color:#f0f9ff; -fx-background-radius:6; " +
                    "-fx-border-color:#bae6fd; -fx-border-radius:6; -fx-padding:8;");
            }
        });

        // ── Enroll in course ──────────────────────────────────────────
        Label courseLabel = new Label("Enroll in Course:");
        courseLabel.getStyleClass().add("form-label");

        courseCombo = new ComboBox<>();
        courseCombo.setMaxWidth(Double.MAX_VALUE);
        courseCombo.setPromptText("Choose a course...");
        loadCourses();
        courseCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Course c) {
                return c == null ? "" : c.getCourseCode() + " — " + c.getCourseName()
                    + "  (" + c.getAvailableSeats() + " seats)";
            }
            public Course fromString(String s) { return null; }
        });

        Button enrollBtn = new Button("✅  Enroll Student");
        enrollBtn.setMaxWidth(Double.MAX_VALUE);
        enrollBtn.getStyleClass().addAll("button", "btn-primary");
        enrollBtn.setStyle("-fx-padding:9 0;");
        enrollBtn.setOnAction(e -> handleEnrollStudent());

        // ── Current Enrollments header ────────────────────────────────
        Label enrollHeader = new Label("  Current Enrollments");
        enrollHeader.setMaxWidth(Double.MAX_VALUE);
        enrollHeader.setStyle(
            "-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:white; " +
            "-fx-background-color:#38bdf8; -fx-padding:6 10; " +
            "-fx-background-radius:4 4 0 0;");

        // ── Enrollment table — grows with content ─────────────────────
        enrollmentTable = new TableView<>();
        enrollmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        enrollmentTable.setPlaceholder(new Label("No enrollments — select a student."));
        // Fixed height so it's always visible without needing to scroll past it
        enrollmentTable.setPrefHeight(220);
        enrollmentTable.setMinHeight(120);

        TableColumn<Enrollment, String> courseCol = new TableColumn<>("Course Name");
        courseCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getCourseName()));
        courseCol.setSortable(true);

        TableColumn<Enrollment, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getCourseCode()));
        codeCol.setSortable(true);

        TableColumn<Enrollment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getStatus()));
        statusCol.setSortable(true);
        statusCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("COMPLETED".equals(s) ? "-fx-text-fill:#16a34a; -fx-font-weight:bold;"
                       : "DROPPED".equals(s)   ? "-fx-text-fill:#dc2626; -fx-font-weight:bold;"
                       : "-fx-text-fill:#ea580c; -fx-font-weight:bold;");
            }
        });

        TableColumn<Enrollment, String> gradeCol = new TableColumn<>("Grade");
        gradeCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getGrade() != null
                ? String.format("%.1f", cd.getValue().getGrade()) : "—"));
        gradeCol.setSortable(true);

        TableColumn<Enrollment, String> letterCol = new TableColumn<>("Letter");
        letterCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getLetterGrade()));
        letterCol.setSortable(true);
        letterCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("A".equals(s) ? "-fx-text-fill:#16a34a; -fx-font-weight:bold;"
                       : "B".equals(s) ? "-fx-text-fill:#1d4ed8; -fx-font-weight:bold;"
                       : "C".equals(s) ? "-fx-text-fill:#b45309; -fx-font-weight:bold;"
                       : "F".equals(s) ? "-fx-text-fill:#dc2626; -fx-font-weight:bold;"
                       : "-fx-text-fill:#64748b;");
            }
        });

        TableColumn<Enrollment, String> dateCol = new TableColumn<>("Enrolled On");
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getEnrollmentDate() != null
                ? cd.getValue().getEnrollmentDate().toString() : "—"));

        enrollmentTable.getColumns().addAll(courseCol, codeCol, statusCol, gradeCol, letterCol, dateCol);
        enrollmentTable.setItems(enrollmentList);

        // ── Detail card — updates when enrollment row is clicked ──────
        Label detailCard = new Label("← Click an enrollment row to see full details.");
        detailCard.setWrapText(true);
        detailCard.setMaxWidth(Double.MAX_VALUE);
        detailCard.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#64748b; " +
            "-fx-background-color:#f8fafc; -fx-background-radius:6; " +
            "-fx-border-color:#e2e8f0; -fx-border-radius:6; -fx-padding:8 10;");

        enrollmentTable.getSelectionModel().selectedItemProperty().addListener((obs, old, e) -> {
            if (e == null) {
                detailCard.setText("← Click an enrollment row to see full details.");
                detailCard.setStyle(
                    "-fx-font-size:11px; -fx-text-fill:#64748b; " +
                    "-fx-background-color:#f8fafc; -fx-background-radius:6; " +
                    "-fx-border-color:#e2e8f0; -fx-border-radius:6; -fx-padding:8 10;");
                return;
            }
            String gradeStr = e.getGrade() != null
                ? String.format("%.1f%%  (%s)", e.getGrade(), e.getLetterGrade())
                : "Not graded yet";
            detailCard.setText(
                "📚  " + e.getCourseName() + "  [" + e.getCourseCode() + "]\n" +
                "Status:    " + e.getStatus() + "\n" +
                "Grade:     " + gradeStr + "\n" +
                "Enrolled:  " + (e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "—") + "\n" +
                "Completed: " + (e.getCompletionDate() != null ? e.getCompletionDate() : "—")
            );
            String border = "COMPLETED".equals(e.getStatus()) ? "#86efac"
                : "DROPPED".equals(e.getStatus()) ? "#fca5a5" : "#fde047";
            String bg = "COMPLETED".equals(e.getStatus()) ? "#f0fdf4"
                : "DROPPED".equals(e.getStatus()) ? "#fef2f2" : "#fefce8";
            detailCard.setStyle(
                "-fx-font-size:11px; -fx-text-fill:#334155; " +
                "-fx-background-color:" + bg + "; -fx-background-radius:6; " +
                "-fx-border-color:" + border + "; -fx-border-radius:6; -fx-padding:8 10;");
        });

        // ── Action buttons ────────────────────────────────────────────
        HBox btnRow = new HBox(8);
        Button dropBtn = new Button("⊘  Drop Selected");
        dropBtn.setMaxWidth(Double.MAX_VALUE);
        dropBtn.getStyleClass().addAll("button", "btn-danger");
        dropBtn.setStyle("-fx-padding:8 0;");
        dropBtn.setOnAction(e -> handleDropEnrollment());
        HBox.setHgrow(dropBtn, Priority.ALWAYS);

        Button refreshEnrollBtn = new Button("↻  Refresh");
        refreshEnrollBtn.setMaxWidth(Double.MAX_VALUE);
        refreshEnrollBtn.getStyleClass().addAll("button", "btn-primary");
        refreshEnrollBtn.setStyle("-fx-padding:8 0;");
        refreshEnrollBtn.setOnAction(e -> {
            Student s = table.getSelectionModel().getSelectedItem();
            if (s != null) loadEnrollmentsForStudent(s);
            else statusLabel.setText("⚠ Select a student first.");
        });
        HBox.setHgrow(refreshEnrollBtn, Priority.ALWAYS);
        btnRow.getChildren().addAll(dropBtn, refreshEnrollBtn);

        // ── Assemble everything into one scrollable VBox ──────────────
        container.getChildren().addAll(
            formTitle,
            selectedInfo,
            new Separator(),
            courseLabel, courseCombo, enrollBtn,
            new Separator(),
            enrollHeader, enrollmentTable,
            detailCard,
            btnRow
        );

        scroll.setContent(container);
        tab.setContent(scroll);
        return tab;
    }

    // ── DATA ──────────────────────────────────────────────────────────
    private void loadStudents() {
        // Step 1: Get all STUDENT-role users
        List<User> allStudentUsers = userDAO.getUsersByRole("STUDENT");

        if (allStudentUsers.isEmpty()) {
            studentList.clear();
            statusLabel.setText("Total: 0 registered students");
            return;
        }

        // Step 2: Get existing student profiles
        List<Student> existing = studentDAO.getAllStudents();
        java.util.Set<Integer> existingUserIds = existing.stream()
            .map(Student::getUserId)
            .collect(java.util.stream.Collectors.toSet());

        // Step 3: Auto-create missing student profiles
        for (User u : allStudentUsers) {
            if (!existingUserIds.contains(u.getUserId())) {
                int count = studentDAO.getTotalStudents();
                String stuNum = String.format("STU%05d", count + 1);
                Student s = new Student(u.getUserId(), stuNum, "Undeclared");
                s.setEnrollmentDate(java.time.LocalDate.now());
                s.setCurrentSemester(1);
                boolean created = studentDAO.createStudent(s);
                System.out.println("Auto-created student profile for user: "
                    + u.getUsername() + " → " + created);
            }
        }

        // Step 4: Build final list — merge users + student profiles
        // This ensures every STUDENT user appears even if profile creation failed
        List<Student> finalList = studentDAO.getAllStudents();

        // Fallback: if still empty but users exist, build synthetic Student objects
        if (finalList.isEmpty() && !allStudentUsers.isEmpty()) {
            for (User u : allStudentUsers) {
                Student s = new Student();
                s.setUserId(u.getUserId());
                s.setFullName(u.getFullName());
                s.setEmail(u.getEmail());
                s.setStudentNumber("STU-" + u.getUserId());
                s.setMajor("Undeclared");
                s.setCurrentSemester(1);
                s.setEnrollmentDate(java.time.LocalDate.now());
                finalList.add(s);
            }
        }

        studentList.setAll(finalList);
        grid.setData(finalList);
        statusLabel.setText("Total: " + finalList.size() + " registered students");
    }

    private void filterStudents(String kw) {
        if (kw == null || kw.isBlank()) { grid.clearFilter(); return; }
        String val = kw.toLowerCase();
        grid.setFilter(s ->
            (s.getFullName() != null && s.getFullName().toLowerCase().contains(val)) ||
            (s.getStudentNumber() != null && s.getStudentNumber().toLowerCase().contains(val)) ||
            (s.getMajor() != null && s.getMajor().toLowerCase().contains(val)));
        statusLabel.setText("Found: " + grid.getTotalFiltered());
    }

    private void loadCourses() {
        courseCombo.setItems(FXCollections.observableArrayList(courseDAO.getActiveCourses()));
    }

    private void onStudentSelected(Student s) {
        editFullNameField.setText(s.getFullName() != null ? s.getFullName() : "");
        editEmailField.setText(s.getEmail() != null ? s.getEmail() : "");
        editMajorField.setText(s.getMajor() != null ? s.getMajor() : "");
        editPhoneField.setText(s.getPhone() != null ? s.getPhone() : "");
        editAddressField.setText(s.getAddress() != null ? s.getAddress() : "");
        semesterSpinner.getValueFactory().setValue(s.getCurrentSemester());

        int enrollCount = enrollmentDAO.getEnrollmentsByStudent(s.getStudentId()).size();
        infoLabel.setText(
            "Student #: " + s.getStudentNumber() + "\n" +
            "Semester: " + s.getCurrentSemester() + "\n" +
            "Total Enrollments: " + enrollCount + "\n" +
            "Registered: " + (s.getEnrollmentDate() != null ? s.getEnrollmentDate() : "—")
        );

        loadEnrollmentsForStudent(s);
    }

    private void loadEnrollmentsForStudent(Student s) {
        enrollmentList.setAll(enrollmentDAO.getEnrollmentsByStudent(s.getStudentId()));
    }

    // ── ACTIONS ───────────────────────────────────────────────────────
    private void handleUpdateStudent() {
        Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { statusLabel.setText("⚠ Select a student first."); return; }

        User user = userDAO.getUserById(selected.getUserId());
        if (user != null) {
            user.setFullName(editFullNameField.getText().trim());
            user.setEmail(editEmailField.getText().trim());
            userDAO.updateUser(user);
        }

        selected.setMajor(editMajorField.getText().trim());
        selected.setPhone(editPhoneField.getText().trim());
        selected.setAddress(editAddressField.getText().trim());
        selected.setCurrentSemester(semesterSpinner.getValue());

        if (studentDAO.updateStudent(selected)) {
            loadStudents();
            statusLabel.setText("✓ Updated: " + selected.getFullName());
        } else {
            statusLabel.setText("⚠ Update failed.");
        }
    }

    private void handleDeactivate() {
        Student s = table.getSelectionModel().getSelectedItem();
        if (s == null) { statusLabel.setText("⚠ Select a student first."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Deactivate account for: " + s.getFullName() + "?\nThis disables login but keeps all data.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Deactivation");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                userDAO.deleteUser(s.getUserId());
                loadStudents();
                statusLabel.setText("✓ Deactivated: " + s.getFullName());
            }
        });
    }

    private void handleResetPassword() {
        Student s = table.getSelectionModel().getSelectedItem();
        if (s == null) { statusLabel.setText("⚠ Select a student first."); return; }
        TextInputDialog dlg = new TextInputDialog("Student@123");
        dlg.setTitle("Reset Password");
        dlg.setHeaderText("Reset password for: " + s.getFullName());
        dlg.setContentText("New password:");
        dlg.showAndWait().ifPresent(newPass -> {
            if (userDAO.changePassword(s.getUserId(), newPass))
                statusLabel.setText("✓ Password reset for " + s.getFullName());
            else
                statusLabel.setText("⚠ Password reset failed.");
        });
    }

    // ── ACTIVATE ──────────────────────────────────────────────────────
    private void handleActivate() {
        Student s = table.getSelectionModel().getSelectedItem();
        if (s == null) { statusLabel.setText("⚠ Select a student first."); return; }
        com.lms.analytics.models.User u = userDAO.getUserById(s.getUserId());
        if (u == null) { statusLabel.setText("⚠ User not found."); return; }
        u.setActive(true);
        if (userDAO.updateUser(u)) {
            loadStudents();
            statusLabel.setText("✓ Account activated: " + s.getFullName());
        } else {
            statusLabel.setText("⚠ Activation failed.");
        }
    }

    // ── DELETE STUDENT (permanent) ────────────────────────────────────
    private void handleDeleteStudent() {
        Student s = table.getSelectionModel().getSelectedItem();
        if (s == null) { statusLabel.setText("⚠ Select a student first."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "PERMANENTLY DELETE student: " + s.getFullName() + "?\n\n" +
            "This will remove ALL their data including enrollments and grades.\n" +
            "This action CANNOT be undone.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("⚠ Permanent Delete");
        confirm.setHeaderText("Are you absolutely sure?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                // Drop all enrollments first
                enrollmentDAO.getEnrollmentsByStudent(s.getStudentId())
                    .forEach(e -> enrollmentDAO.dropEnrollment(e.getEnrollmentId()));
                // Delete student profile
                studentDAO.deleteStudent(s.getStudentId());
                // Hard delete user
                userDAO.hardDeleteUser(s.getUserId());
                loadStudents();
                statusLabel.setText("✓ Permanently deleted: " + s.getFullName());
            }
        });
    }

    // ── VIEW ENROLLMENTS ──────────────────────────────────────────────
    private void handleViewEnrollments() {
        Student s = table.getSelectionModel().getSelectedItem();
        if (s == null) { statusLabel.setText("⚠ Select a student first."); return; }
        var enrollments = enrollmentDAO.getEnrollmentsByStudent(s.getStudentId());
        StringBuilder sb = new StringBuilder(s.getFullName() + " — Enrollments:\n\n");
        if (enrollments.isEmpty()) {
            sb.append("No enrollments found.");
        } else {
            enrollments.forEach(e -> sb.append("• ")
                .append(e.getCourseCode()).append(" — ").append(e.getCourseName())
                .append("  [").append(e.getStatus()).append("]")
                .append(e.getGrade() != null ? "  Grade: " + String.format("%.1f", e.getGrade()) : "")
                .append("\n"));
        }
        Alert info = new Alert(Alert.AlertType.INFORMATION, sb.toString(), ButtonType.OK);
        info.setTitle("Student Enrollments");
        info.setHeaderText(s.getFullName() + " — " + enrollments.size() + " enrollment(s)");
        info.showAndWait();
    }

    // ── GENERATE TEMPORARY PASSWORD ───────────────────────────────────
    private void handleGenerateTempPassword() {
        Student s = table.getSelectionModel().getSelectedItem();
        if (s == null) { statusLabel.setText("⚠ Select a student first."); return; }

        // Generate a random 10-char temp password
        String tempPass = generateTempPassword();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Generate temporary password for: " + s.getFullName() + "?\n\n" +
            "Temporary password: " + tempPass + "\n\n" +
            "Share this with the student. They should change it after login.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Generate Temporary Password");
        confirm.setHeaderText("Temporary Password");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (userDAO.changePassword(s.getUserId(), tempPass)) {
                    statusLabel.setText("✓ Temp password set for " + s.getFullName() + ": " + tempPass);
                    // Show the password in a copyable dialog
                    TextInputDialog showDlg = new TextInputDialog(tempPass);
                    showDlg.setTitle("Temporary Password Generated");
                    showDlg.setHeaderText("Password for: " + s.getFullName());
                    showDlg.setContentText("Copy and share this password:");
                    showDlg.showAndWait();
                } else {
                    statusLabel.setText("⚠ Failed to set temporary password.");
                }
            }
        });
    }

    // ── FORCE PASSWORD RESET ON NEXT LOGIN ────────────────────────────
    private void handleForcePasswordReset() {
        Student s = table.getSelectionModel().getSelectedItem();
        if (s == null) { statusLabel.setText("⚠ Select a student first."); return; }

        // Set a known temp password and inform admin to tell student
        String tempPass = "Change@" + s.getStudentNumber();

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Force password reset for: " + s.getFullName() + "?\n\n" +
            "Their password will be set to a temporary value.\n" +
            "They must change it on next login.\n\n" +
            "Temporary password: " + tempPass,
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Force Password Reset");
        confirm.setHeaderText("Force Reset on Next Login");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (userDAO.changePassword(s.getUserId(), tempPass)) {
                    statusLabel.setText("✓ Password reset forced for " + s.getFullName()
                        + ". Temp: " + tempPass);
                } else {
                    statusLabel.setText("⚠ Failed to force password reset.");
                }
            }
        });
    }

    private String generateTempPassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#!";
        java.util.Random rnd = new java.util.Random();
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        return sb.toString();
    }

    private void handleEnrollStudent() {        Student s = table.getSelectionModel().getSelectedItem();
        Course  c = courseCombo.getValue();
        if (s == null) { statusLabel.setText("⚠ Select a student first."); return; }
        if (c == null) { statusLabel.setText("⚠ Select a course."); return; }
        if (enrollmentDAO.isEnrolled(s.getStudentId(), c.getCourseId())) {
            statusLabel.setText("⚠ Already enrolled in " + c.getCourseName()); return;
        }
        if (c.getAvailableSeats() <= 0) {
            statusLabel.setText("⚠ No seats available in " + c.getCourseName()); return;
        }
        if (enrollmentDAO.enrollStudent(s.getStudentId(), c.getCourseId())) {
            loadEnrollmentsForStudent(s);
            loadStudents();
            loadCourses(); // refresh seat counts
            statusLabel.setText("✓ Enrolled " + s.getFullName() + " in " + c.getCourseName());
        } else {
            statusLabel.setText("⚠ Enrollment failed.");
        }
    }

    private void handleDropEnrollment() {
        Enrollment e = enrollmentTable.getSelectionModel().getSelectedItem();
        if (e == null) { statusLabel.setText("⚠ Select an enrollment to drop."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Drop: " + e.getCourseName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                enrollmentDAO.dropEnrollment(e.getEnrollmentId());
                Student s = table.getSelectionModel().getSelectedItem();
                if (s != null) loadEnrollmentsForStudent(s);
                loadStudents();
                statusLabel.setText("✓ Enrollment dropped.");
            }
        });
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a; " +
            "-fx-padding:6 0 2 0;");
        return l;
    }

    private TextField formField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.getStyleClass().add("text-field");
        return tf;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    private Button actionBtn(String text, String color) {
        Button b = new Button(text);
        b.setMaxWidth(Double.MAX_VALUE);
        // Map color to CSS class for consistency
        String cssClass = switch (color) {
            case "#1f6feb", "#1d4ed8", "#2563eb" -> "btn-primary";
            case "#238636", "#16a34a", "#22c55e"  -> "btn-success";
            case "#da3633", "#dc2626", "#b91c1c"  -> "btn-danger";
            case "#7c3aed", "#9333ea"             -> "btn-warning";
            default                               -> "btn-secondary";
        };
        b.getStyleClass().addAll("button", cssClass);
        b.setStyle("-fx-padding:10 0;");
        return b;
    }
}
