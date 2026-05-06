package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.lms.analytics.dao.CourseDAO;
import com.lms.analytics.dao.EnrollmentDAO;
import com.lms.analytics.models.Course;
import com.lms.analytics.models.Enrollment;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.DataGridHelper;
import com.lms.analytics.utils.SessionManager;

import java.util.List;

public class InstructorCoursesController {

    @FXML private StackPane root;

    private TableView<Course>     courseTable;
    private TableView<Enrollment> studentTable;
    private Label statusLabel, courseInfoLabel, studentDetailLabel;
    private ComboBox<String> statusFilterCombo;
    private TextField gradeEditField;

    private final CourseDAO     courseDAO     = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    private DataGridHelper<Course>     courseGrid;
    private DataGridHelper<Enrollment> studentGrid;

    @FXML
    public void initialize() { buildUI(); loadCourses(); }

    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");

        // ── Top bar ───────────────────────────────────────────────────
        VBox top = new VBox(8);
        top.setPadding(new Insets(16, 20, 10, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        Label title = new Label("📚  My Courses");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label subtitle = new Label("Select a course to view enrolled students. Click a student to manage their grade.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#38bdf8;");

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().addAll("button", "btn-primary");
        refreshBtn.setStyle("-fx-padding:5 14;");
        refreshBtn.setOnAction(e -> loadCourses());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox topRow = new HBox(10, sp, statusLabel, refreshBtn);
        topRow.setAlignment(Pos.CENTER_RIGHT);
        top.getChildren().addAll(title, subtitle, topRow);
        layout.setTop(top);

        // ── LEFT: Course table with DataGrid ──────────────────────────
        courseTable = new TableView<>();
        courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        courseTable.setPlaceholder(new Label("No courses assigned to you yet."));

        TableColumn<Course, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseCode()));
        codeCol.setSortable(true);

        TableColumn<Course, String> nameCol = new TableColumn<>("Course Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseName()));
        nameCol.setSortable(true);

        TableColumn<Course, String> enrolledCol = new TableColumn<>("Enrolled");
        enrolledCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getEnrolledCount() + " / " + cd.getValue().getCapacity()));

        TableColumn<Course, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        statusCol.setSortable(true);
        statusCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("ACTIVE".equals(s) ? "-fx-text-fill:#16a34a; -fx-font-weight:bold;"
                       : "-fx-text-fill:#64748b; -fx-font-weight:bold;");
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

        VBox leftBox = new VBox(0, courseSearch, courseTable, coursePaging);
        VBox.setVgrow(courseTable, Priority.ALWAYS);
        leftBox.setMinWidth(160); // no fixed pref width — freely resizable

        // ── RIGHT: Students in selected course ────────────────────────
        VBox rightBox = new VBox(0);
        rightBox.setMinWidth(120); // starts narrow, grows when user drags
        rightBox.setStyle("-fx-background-color:white;");

        // Course info header
        VBox courseHeader = new VBox(4);
        courseHeader.setPadding(new Insets(10, 12, 8, 12));
        courseHeader.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");
        courseInfoLabel = new Label("← Select a course to see enrolled students.");
        courseInfoLabel.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        // Status filter for students
        HBox studentFilterRow = new HBox(10);
        studentFilterRow.setAlignment(Pos.CENTER_LEFT);
        studentFilterRow.setPadding(new Insets(6, 12, 6, 12));
        Label sfLbl = new Label("Filter:");
        sfLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.setItems(FXCollections.observableArrayList(
            "ALL", "ENROLLED", "COMPLETED", "DROPPED"));
        statusFilterCombo.setValue("ALL");
        statusFilterCombo.setOnAction(e -> applyStudentFilter());
        studentFilterRow.getChildren().addAll(sfLbl, statusFilterCombo);

        courseHeader.getChildren().addAll(courseInfoLabel, studentFilterRow);

        // Student table
        studentTable = new TableView<>();
        studentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        studentTable.setPlaceholder(new Label("Select a course to see enrolled students."));

        TableColumn<Enrollment, String> sNameCol = new TableColumn<>("Student Name");
        sNameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStudentName()));
        sNameCol.setSortable(true);

        TableColumn<Enrollment, String> sDateCol = new TableColumn<>("Enrolled On");
        sDateCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getEnrollmentDate() != null
                ? cd.getValue().getEnrollmentDate().toString() : ""));
        sDateCol.setSortable(true);

        TableColumn<Enrollment, String> sStatusCol = new TableColumn<>("Status");
        sStatusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
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
        sLetterCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getLetterGrade()));
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
            val -> e -> e.getStudentName() != null && e.getStudentName().toLowerCase().contains(val)
        );
        HBox studentPaging = studentGrid.buildPaginationBar();

        // ── Inline grade editing panel ────────────────────────────────
        VBox gradePanel = new VBox(8);
        gradePanel.setPadding(new Insets(10, 12, 10, 12));
        gradePanel.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0; -fx-border-width:1 0 0 0;");

        studentDetailLabel = new Label("← Select a student to assign grade.");
        studentDetailLabel.setWrapText(true);
        studentDetailLabel.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:6; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:6; -fx-padding:8;");

        HBox gradeRow = new HBox(8);
        gradeRow.setAlignment(Pos.CENTER_LEFT);
        Label gradeLbl = new Label("Grade (0-100):");
        gradeLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");
        gradeEditField = new TextField();
        gradeEditField.setPromptText("e.g. 87.5");
        gradeEditField.setPrefWidth(100);
        gradeEditField.getStyleClass().add("text-field");

        Button saveGradeBtn = new Button("💾 Save Grade");
        saveGradeBtn.getStyleClass().addAll("button", "btn-primary");
        saveGradeBtn.setStyle("-fx-padding:6 14;");
        saveGradeBtn.setOnAction(e -> handleSaveGrade());

        Button markCompleteBtn = new Button("✅ Complete");
        markCompleteBtn.getStyleClass().addAll("button", "btn-primary");
        markCompleteBtn.setStyle("-fx-padding:6 10;");
        markCompleteBtn.setOnAction(e -> handleMarkStatus("COMPLETED"));

        Button markDropBtn = new Button("⊘ Drop");
        markDropBtn.getStyleClass().addAll("button", "btn-danger");
        markDropBtn.setStyle("-fx-padding:6 10;");
        markDropBtn.setOnAction(e -> handleMarkStatus("DROPPED"));

        gradeRow.getChildren().addAll(gradeLbl, gradeEditField, saveGradeBtn,
                                       markCompleteBtn, markDropBtn);
        gradePanel.getChildren().addAll(studentDetailLabel, gradeRow);

        rightBox.getChildren().addAll(courseHeader, studentSearch, studentTable,
                                       studentPaging, gradePanel);
        VBox.setVgrow(studentTable, Priority.ALWAYS);

        SplitPane split = new SplitPane(leftBox, rightBox);
        split.setDividerPositions(0.68); // left large, right sidebar — drag to expand
        layout.setCenter(split);
        root.getChildren().add(layout);
    }

    // ── DETAIL PANEL ──────────────────────────────────────────────────
    private void updateStudentDetail(Enrollment e) {
        if (e == null) {
            studentDetailLabel.setText("← Select a student to assign grade.");
            gradeEditField.clear();
            return;
        }
        studentDetailLabel.setText(
            "Student: " + e.getStudentName() + "  |  " +
            "Status: " + e.getStatus() + "  |  " +
            "Grade: " + (e.getGrade() != null
                ? String.format("%.1f  (%s)", e.getGrade(), e.getLetterGrade())
                : "Not graded")
        );
        if (e.getGrade() != null) gradeEditField.setText(String.format("%.1f", e.getGrade()));
        else gradeEditField.clear();
    }

    // ── DATA ──────────────────────────────────────────────────────────
    private void loadCourses() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;
        List<Course> courses = courseDAO.getCoursesByInstructor(user.getUserId());
        courseGrid.setData(courses);
        statusLabel.setText("My Courses: " + courses.size());
    }

    private void loadStudentsForCourse(Course c) {
        List<Enrollment> enrollments = enrollmentDAO.getEnrollmentsByCourse(c.getCourseId());
        studentGrid.setData(enrollments);
        long graded = enrollments.stream().filter(e -> e.getGrade() != null).count();
        double avg  = enrollments.stream().filter(e -> e.getGrade() != null)
            .mapToDouble(Enrollment::getGrade).average().orElse(0);
        courseInfoLabel.setText(
            c.getCourseCode() + " — " + c.getCourseName() +
            "  |  Students: " + enrollments.size() +
            "  |  Graded: " + graded +
            (avg > 0 ? "  |  Avg: " + String.format("%.1f%%", avg) : "")
        );
    }

    private void applyStudentFilter() {
        String f = statusFilterCombo.getValue();
        if (f == null || "ALL".equals(f)) studentGrid.clearFilter();
        else studentGrid.setFilter(e -> f.equals(e.getStatus()));
    }

    // ── GRADE ACTIONS ─────────────────────────────────────────────────
    private void handleSaveGrade() {
        Enrollment e = studentTable.getSelectionModel().getSelectedItem();
        if (e == null) { showStatus("⚠ Select a student first."); return; }

        // Security: verify instructor owns this course
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && !courseDAO.isCourseTaughtByInstructor(e.getCourseId(), user.getUserId())) {
            showStatus("⚠ You can only grade students in your own courses.");
            return;
        }

        String text = gradeEditField.getText().trim();
        if (text.isEmpty()) { showStatus("⚠ Enter a grade value."); return; }
        try {
            double grade = Double.parseDouble(text);
            if (grade < 0 || grade > 100) { showStatus("⚠ Grade must be 0–100."); return; }
            if (enrollmentDAO.updateGrade(e.getEnrollmentId(), grade)) {
                Course c = courseTable.getSelectionModel().getSelectedItem();
                if (c != null) loadStudentsForCourse(c);
                showStatus("✓ Grade saved: " + grade);
            } else {
                showStatus("⚠ Failed to save grade.");
            }
        } catch (NumberFormatException ex) {
            showStatus("⚠ Invalid grade value.");
        }
    }

    private void handleMarkStatus(String status) {
        Enrollment e = studentTable.getSelectionModel().getSelectedItem();
        if (e == null) { showStatus("⚠ Select a student first."); return; }

        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && !courseDAO.isCourseTaughtByInstructor(e.getCourseId(), user.getUserId())) {
            showStatus("⚠ You can only manage students in your own courses.");
            return;
        }

        if ("DROPPED".equals(status)) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Mark " + e.getStudentName() + " as DROPPED?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) {
                    enrollmentDAO.updateEnrollmentStatus(e.getEnrollmentId(), status);
                    Course c = courseTable.getSelectionModel().getSelectedItem();
                    if (c != null) loadStudentsForCourse(c);
                    showStatus("✓ Marked as " + status + ": " + e.getStudentName());
                }
            });
        } else {
            enrollmentDAO.updateEnrollmentStatus(e.getEnrollmentId(), status);
            Course c = courseTable.getSelectionModel().getSelectedItem();
            if (c != null) loadStudentsForCourse(c);
            showStatus("✓ Marked as " + status + ": " + e.getStudentName());
        }
    }

    private void showStatus(String msg) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:" +
            (msg.startsWith("✓") ? "#16a34a;" : msg.startsWith("⚠") ? "#dc2626;" : "#38bdf8;"));
    }
}
