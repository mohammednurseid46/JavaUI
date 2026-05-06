package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import com.lms.analytics.dao.CourseDAO;
import com.lms.analytics.dao.EnrollmentDAO;
import com.lms.analytics.models.Course;
import com.lms.analytics.models.Enrollment;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.DataGridHelper;
import com.lms.analytics.utils.SessionManager;

import java.util.List;

public class GradeStudentsController {

    @FXML private StackPane root;

    // Left — course list
    private TableView<Course> courseTable;
    private DataGridHelper<Course> courseGrid;
    private Label courseCountLabel;

    // Right — student list + grade panel
    private TableView<Enrollment> studentTable;
    private DataGridHelper<Enrollment> studentGrid;
    private Label courseInfoLabel;
    private Label studentDetailLabel;
    private TextField gradeField;
    private Label statusLabel;
    private ComboBox<String> statusFilterCombo;

    private final CourseDAO     courseDAO     = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    @FXML
    public void initialize() { buildUI(); loadMyCourses(); }

    // ── BUILD UI ──────────────────────────────────────────────────────
    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");

        // ── Top bar ───────────────────────────────────────────────────
        VBox top = new VBox(6);
        top.setPadding(new Insets(14, 20, 10, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        HBox titleRow = new HBox(12);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e ->
            com.lms.analytics.utils.NavigationUtil.backToInstructorDashboard(courseTable));

        Label title = new Label("🎓  Grade Students");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-font-weight:bold;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().addAll("button", "btn-primary");
        refreshBtn.setStyle("-fx-padding:5 14;");
        refreshBtn.setOnAction(e -> {
            loadMyCourses();
            Course sel = courseTable.getSelectionModel().getSelectedItem();
            if (sel != null) loadStudentsForCourse(sel);
        });

        titleRow.getChildren().addAll(backBtn, title, sp, statusLabel, refreshBtn);

        Label subtitle = new Label(
            "Select one of your courses (left) → then select a student (right) to assign or update their grade.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        top.getChildren().addAll(titleRow, subtitle);
        layout.setTop(top);

        // ── LEFT: My courses ──────────────────────────────────────────
        courseTable = new TableView<>();
        courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        courseTable.setPlaceholder(new Label("No courses assigned to you yet."));

        TableColumn<Course, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getCourseCode()));
        codeCol.setSortable(true);

        TableColumn<Course, String> nameCol = new TableColumn<>("Course Name");
        nameCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getCourseName()));
        nameCol.setSortable(true);

        TableColumn<Course, String> enrolledCol = new TableColumn<>("Students");
        enrolledCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getEnrolledCount() + "/" + cd.getValue().getCapacity()));

        TableColumn<Course, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getStatus()));
        statusCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("ACTIVE".equals(s)
                    ? "-fx-text-fill:#16a34a; -fx-font-weight:bold;"
                    : "-fx-text-fill:#64748b;");
            }
        });

        courseTable.getColumns().addAll(codeCol, nameCol, enrolledCol, statusCol);
        courseTable.getSelectionModel().selectedItemProperty().addListener(
            (o, old, c) -> { if (c != null) loadStudentsForCourse(c); });

        courseGrid = new DataGridHelper<>(courseTable);
        courseGrid.addSortOption("Name",     java.util.Comparator.comparing(
            c -> c.getCourseName() != null ? c.getCourseName() : ""));
        courseGrid.addSortOption("Code",     java.util.Comparator.comparing(
            c -> c.getCourseCode() != null ? c.getCourseCode() : ""));
        courseGrid.addSortOption("Enrolled", java.util.Comparator.comparingInt(
            com.lms.analytics.models.Course::getEnrolledCount));
        courseGrid.addSortOption("Status",   java.util.Comparator.comparing(
            c -> c.getStatus() != null ? c.getStatus() : ""));
        HBox courseSearch = courseGrid.buildFilterBarWithSort(
            "Search courses...",
            val -> c -> (c.getCourseCode() != null && c.getCourseCode().toLowerCase().contains(val))
                     || (c.getCourseName() != null && c.getCourseName().toLowerCase().contains(val))
        );
        HBox coursePaging = courseGrid.buildPaginationBar();

        courseCountLabel = new Label();
        courseCountLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b; -fx-padding:4 12;");

        VBox leftBox = new VBox(0, courseSearch, courseTable, coursePaging, courseCountLabel);
        VBox.setVgrow(courseTable, Priority.ALWAYS);
        // No fixed width — let SplitPane control it
        leftBox.setMinWidth(180);
        leftBox.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 1 0 0;");

        // ── RIGHT: Students + grade panel ─────────────────────────────
        VBox rightBox = new VBox(0);
        rightBox.setStyle("-fx-background-color:#f8fafc;");
        rightBox.setMinWidth(120); // can be narrow initially, grows when user drags

        // Course info header
        VBox courseHeader = new VBox(6);
        courseHeader.setPadding(new Insets(10, 14, 8, 14));
        courseHeader.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        courseInfoLabel = new Label("← Select a course from the left to see enrolled students.");
        courseInfoLabel.setWrapText(true);
        courseInfoLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        // Status filter
        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Label filterLbl = new Label("Filter:");
        filterLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.setItems(FXCollections.observableArrayList(
            "ALL", "ENROLLED", "COMPLETED", "DROPPED"));
        statusFilterCombo.setValue("ALL");
        statusFilterCombo.setOnAction(e -> applyStudentFilter());
        filterRow.getChildren().addAll(filterLbl, statusFilterCombo);

        courseHeader.getChildren().addAll(courseInfoLabel, filterRow);

        // Student table
        studentTable = new TableView<>();
        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        studentTable.setPlaceholder(new Label("Select a course to see enrolled students."));

        TableColumn<Enrollment, String> sNameCol = new TableColumn<>("Student Name");
        sNameCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getStudentName()));
        sNameCol.setSortable(true);

        TableColumn<Enrollment, String> sDateCol = new TableColumn<>("Enrolled On");
        sDateCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getEnrollmentDate() != null
                ? cd.getValue().getEnrollmentDate().toString() : ""));
        sDateCol.setSortable(true);

        TableColumn<Enrollment, String> sStatusCol = new TableColumn<>("Status");
        sStatusCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getStatus()));
        sStatusCol.setSortable(true);
        sStatusCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("COMPLETED".equals(s) ? "-fx-text-fill:#16a34a; -fx-font-weight:bold;"
                       : "DROPPED".equals(s)   ? "-fx-text-fill:#dc2626; -fx-font-weight:bold;"
                       : "-fx-text-fill:#ea580c; -fx-font-weight:bold;");
            }
        });

        TableColumn<Enrollment, String> sGradeCol = new TableColumn<>("Grade (%)");
        sGradeCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getGrade() != null
                ? String.format("%.1f", cd.getValue().getGrade()) : "—"));
        sGradeCol.setSortable(true);

        TableColumn<Enrollment, String> sLetterCol = new TableColumn<>("Letter");
        sLetterCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getLetterGrade()));
        sLetterCol.setSortable(true);
        sLetterCol.setCellFactory(col -> new TableCell<>() {
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

        studentTable.getColumns().addAll(sNameCol, sDateCol, sStatusCol, sGradeCol, sLetterCol);
        studentTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> updateStudentDetail(sel));

        studentGrid = new DataGridHelper<>(studentTable);
        studentGrid.addSortOption("Student Name", java.util.Comparator.comparing(
            e -> e.getStudentName() != null ? e.getStudentName() : ""));
        studentGrid.addSortOption("Date",         java.util.Comparator.comparing(
            e -> e.getEnrollmentDate() != null ? e.getEnrollmentDate().toString() : ""));
        studentGrid.addSortOption("Status",       java.util.Comparator.comparing(
            e -> e.getStatus() != null ? e.getStatus() : ""));
        studentGrid.addSortOption("Grade",        java.util.Comparator.comparingDouble(
            e -> e.getGrade() != null ? e.getGrade() : -1.0));
        HBox studentSearch = studentGrid.buildFilterBarWithSort(
            "Search students...",
            val -> e -> e.getStudentName() != null
                     && e.getStudentName().toLowerCase().contains(val)
        );
        HBox studentPaging = studentGrid.buildPaginationBar();

        // ── Grade action panel ────────────────────────────────────────
        VBox gradePanel = new VBox(10);
        gradePanel.setPadding(new Insets(14, 16, 14, 16));
        gradePanel.setStyle(
            "-fx-background-color:white; " +
            "-fx-border-color:#e2e8f0; -fx-border-width:1 0 0 0;");

        studentDetailLabel = new Label("← Select a student from the table to assign a grade.");
        studentDetailLabel.setWrapText(true);
        studentDetailLabel.setMaxWidth(Double.MAX_VALUE);
        studentDetailLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:6; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:6; -fx-padding:8 10;");

        Label gradeSectionTitle = new Label("Assign / Update Grade");
        gradeSectionTitle.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        // Grade input row
        Label gradeLbl = new Label("Grade (0 – 100):");
        gradeLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        gradeField = new TextField();
        gradeField.setPromptText("e.g. 87.5");
        gradeField.setMaxWidth(Double.MAX_VALUE);
        gradeField.getStyleClass().add("text-field");
        gradeField.setOnAction(e -> handleSaveGrade());

        Button saveBtn = new Button("💾  Save Grade");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.getStyleClass().addAll("button", "btn-primary");
        saveBtn.setStyle("-fx-padding:9 0; -fx-font-size:13px;");
        saveBtn.setOnAction(e -> handleSaveGrade());

        // Status buttons — full width, stacked
        Label statusActionsLbl = new Label("Enrollment Status:");
        statusActionsLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155; -fx-padding:4 0 0 0;");

        Button markEnrolledBtn = new Button("🔄  Mark as Enrolled");
        markEnrolledBtn.setMaxWidth(Double.MAX_VALUE);
        markEnrolledBtn.getStyleClass().addAll("button", "btn-primary");
        markEnrolledBtn.setStyle("-fx-padding:8 0;");
        markEnrolledBtn.setOnAction(e -> handleMarkStatus("ENROLLED"));

        Button markCompleteBtn = new Button("✅  Mark as Completed");
        markCompleteBtn.setMaxWidth(Double.MAX_VALUE);
        markCompleteBtn.getStyleClass().addAll("button", "btn-primary");
        markCompleteBtn.setStyle("-fx-padding:8 0;");
        markCompleteBtn.setOnAction(e -> handleMarkStatus("COMPLETED"));

        Button markDropBtn = new Button("⊘  Mark as Dropped");
        markDropBtn.setMaxWidth(Double.MAX_VALUE);
        markDropBtn.getStyleClass().addAll("button", "btn-danger");
        markDropBtn.setStyle("-fx-padding:8 0;");
        markDropBtn.setOnAction(e -> handleMarkStatus("DROPPED"));

        gradePanel.getChildren().addAll(
            gradeSectionTitle, studentDetailLabel,
            gradeLbl, gradeField, saveBtn,
            new Separator(),
            statusActionsLbl,
            markEnrolledBtn, markCompleteBtn, markDropBtn
        );

        // ── RIGHT BOX: wrap everything in one single ScrollPane ───────
        // courseHeader + studentSearch + studentTable + studentPaging + gradePanel
        // all scroll together as one unified panel
        VBox rightContent = new VBox(0);
        rightContent.setStyle("-fx-background-color:#f8fafc;");

        // Student table section — grows vertically
        VBox studentSection = new VBox(0, studentSearch, studentTable, studentPaging);
        VBox.setVgrow(studentTable, Priority.ALWAYS);
        studentSection.setMinHeight(200);
        studentSection.setPrefHeight(260);

        rightContent.getChildren().addAll(
            courseHeader,
            studentSection,
            gradePanel
        );

        ScrollPane rightScroll = new ScrollPane(rightContent);
        rightScroll.setFitToWidth(true);
        rightScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rightScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rightScroll.setStyle(
            "-fx-background-color:#f8fafc; " +
            "-fx-background:#f8fafc;");

        rightBox.getChildren().add(rightScroll);
        VBox.setVgrow(rightScroll, Priority.ALWAYS);

        // ── Layout: SplitPane — left 68% (large), right 32% (small sidebar), freely draggable
        SplitPane split = new SplitPane(leftBox, rightBox);
        split.setDividerPositions(0.68);   // left takes most space, right is a sidebar
        split.setMaxHeight(Double.MAX_VALUE);
        split.setMaxWidth(Double.MAX_VALUE);
        SplitPane.setResizableWithParent(leftBox, true);
        SplitPane.setResizableWithParent(rightBox, true);

        layout.setCenter(split);

        layout.setMaxWidth(Double.MAX_VALUE);
        layout.setMaxHeight(Double.MAX_VALUE);
        root.setMaxWidth(Double.MAX_VALUE);
        root.setMaxHeight(Double.MAX_VALUE);
        root.getChildren().add(layout);
    }

    // ── DATA ──────────────────────────────────────────────────────────
    private void loadMyCourses() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) {
            setStatus("⚠ Not logged in.", false);
            return;
        }
        // ONLY courses where instructor_id = current user
        List<Course> courses = courseDAO.getCoursesByInstructor(user.getUserId());
        courseGrid.setData(courses);
        courseCountLabel.setText("You have " + courses.size() + " course(s) assigned.");
        if (courses.isEmpty()) {
            setStatus("No courses assigned to you yet.", false);
        } else {
            setStatus("My Courses: " + courses.size(), true);
        }
    }

    private void loadStudentsForCourse(Course c) {
        // Verify this course belongs to the current instructor
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && !courseDAO.isCourseTaughtByInstructor(c.getCourseId(), user.getUserId())) {
            setStatus("⚠ You are not the instructor of this course.", false);
            return;
        }

        List<Enrollment> enrollments = enrollmentDAO.getEnrollmentsByCourse(c.getCourseId());
        studentGrid.setData(enrollments);

        long graded    = enrollments.stream().filter(e -> e.getGrade() != null).count();
        long completed = enrollments.stream().filter(e -> "COMPLETED".equals(e.getStatus())).count();
        double avg     = enrollments.stream().filter(e -> e.getGrade() != null)
            .mapToDouble(Enrollment::getGrade).average().orElse(0);

        courseInfoLabel.setText(
            c.getCourseCode() + " — " + c.getCourseName() +
            "   |   Students: " + enrollments.size() +
            "   |   Graded: " + graded +
            "   |   Completed: " + completed +
            (avg > 0 ? "   |   Avg: " + String.format("%.1f%%", avg) : "")
        );

        // Reset filter
        statusFilterCombo.setValue("ALL");
        studentGrid.clearFilter();
    }

    private void applyStudentFilter() {
        String f = statusFilterCombo.getValue();
        if (f == null || "ALL".equals(f)) studentGrid.clearFilter();
        else studentGrid.setFilter(e -> f.equals(e.getStatus()));
    }

    // ── DETAIL PANEL ──────────────────────────────────────────────────
    private void updateStudentDetail(Enrollment e) {
        if (e == null) {
            studentDetailLabel.setText("← Select a student from the table to assign a grade.");
            studentDetailLabel.setStyle(
                "-fx-font-size:12px; -fx-text-fill:#64748b; " +
                "-fx-background-color:#f8fafc; -fx-background-radius:6; " +
                "-fx-border-color:#e2e8f0; -fx-border-radius:6; -fx-padding:8 10;");
            gradeField.clear();
            return;
        }
        String gradeStr = e.getGrade() != null
            ? String.format("%.1f%%  (%s)", e.getGrade(), e.getLetterGrade())
            : "Not graded yet";
        studentDetailLabel.setText(
            "👤  " + e.getStudentName() + "\n" +
            "Status:    " + e.getStatus() + "\n" +
            "Grade:     " + gradeStr + "\n" +
            "Enrolled:  " + (e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "—")
        );
        // Color the detail card based on status
        String bg = "COMPLETED".equals(e.getStatus()) ? "#f0fdf4"
            : "DROPPED".equals(e.getStatus()) ? "#fef2f2" : "#fefce8";
        String border = "COMPLETED".equals(e.getStatus()) ? "#86efac"
            : "DROPPED".equals(e.getStatus()) ? "#fca5a5" : "#fde047";
        studentDetailLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:" + bg + "; -fx-background-radius:6; " +
            "-fx-border-color:" + border + "; -fx-border-radius:6; -fx-padding:8 10;");

        if (e.getGrade() != null) gradeField.setText(String.format("%.1f", e.getGrade()));
        else gradeField.clear();
    }

    // ── GRADE ACTIONS ─────────────────────────────────────────────────
    private void handleSaveGrade() {
        Enrollment e = studentTable.getSelectionModel().getSelectedItem();
        if (e == null) { setStatus("⚠ Select a student first.", false); return; }

        // Security check — instructor can only grade their own courses
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && !courseDAO.isCourseTaughtByInstructor(e.getCourseId(), user.getUserId())) {
            setStatus("⚠ You can only grade students in your own courses.", false);
            return;
        }

        String text = gradeField.getText().trim();
        if (text.isEmpty()) { setStatus("⚠ Enter a grade value (0–100).", false); return; }

        try {
            double grade = Double.parseDouble(text);
            if (grade < 0 || grade > 100) { setStatus("⚠ Grade must be between 0 and 100.", false); return; }

            if (enrollmentDAO.updateGrade(e.getEnrollmentId(), grade)) {
                Course c = courseTable.getSelectionModel().getSelectedItem();
                if (c != null) loadStudentsForCourse(c);
                setStatus("✓ Grade saved: " + grade + "  (" + letterGrade(grade) + ")", true);
            } else {
                setStatus("⚠ Failed to save grade.", false);
            }
        } catch (NumberFormatException ex) {
            setStatus("⚠ Invalid number — enter a value like 87.5", false);
        }
    }

    private void handleMarkStatus(String status) {
        Enrollment e = studentTable.getSelectionModel().getSelectedItem();
        if (e == null) { setStatus("⚠ Select a student first.", false); return; }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && !courseDAO.isCourseTaughtByInstructor(e.getCourseId(), user.getUserId())) {
            setStatus("⚠ You can only manage students in your own courses.", false);
            return;
        }

        if ("DROPPED".equals(status)) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Mark " + e.getStudentName() + " as DROPPED from this course?",
                ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Confirm Drop");
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) {
                    enrollmentDAO.updateEnrollmentStatus(e.getEnrollmentId(), status);
                    Course c = courseTable.getSelectionModel().getSelectedItem();
                    if (c != null) loadStudentsForCourse(c);
                    setStatus("✓ Marked as DROPPED: " + e.getStudentName(), true);
                }
            });
        } else {
            enrollmentDAO.updateEnrollmentStatus(e.getEnrollmentId(), status);
            Course c = courseTable.getSelectionModel().getSelectedItem();
            if (c != null) loadStudentsForCourse(c);
            setStatus("✓ Marked as " + status + ": " + e.getStudentName(), true);
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private String letterGrade(double g) {
        if (g >= 90) return "A";
        if (g >= 80) return "B";
        if (g >= 70) return "C";
        if (g >= 60) return "D";
        return "F";
    }

    private void setStatus(String msg, boolean success) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:"
            + (success ? "#16a34a;" : msg.startsWith("⚠") ? "#dc2626;" : "#64748b;"));
    }
}
