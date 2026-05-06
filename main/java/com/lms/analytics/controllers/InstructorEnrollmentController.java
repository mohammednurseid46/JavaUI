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

import java.util.ArrayList;
import java.util.List;

/**
 * Instructor-only enrollment view.
 * Shows ONLY enrollments for courses taught by the logged-in instructor.
 */
public class InstructorEnrollmentController {

    @FXML private StackPane root;

    private TableView<Enrollment> table;
    private DataGridHelper<Enrollment> grid;
    private ComboBox<Course> courseFilterCombo;
    private ComboBox<String> statusFilterCombo;
    private Label statusLabel, totalLabel, detailLabel;

    private final CourseDAO     courseDAO     = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    private List<Course> myCourses = new ArrayList<>();

    @FXML
    public void initialize() { buildUI(); loadData(); }

    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");

        // ── Top bar ───────────────────────────────────────────────────
        VBox top = new VBox(8);
        top.setPadding(new Insets(16, 20, 12, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        Label title = new Label("📝  My Course Enrollments");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label subtitle = new Label("Showing only enrollments for courses you teach.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        // Filter row
        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        Label courseLbl = new Label("Course:");
        courseLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        courseFilterCombo = new ComboBox<>();
        courseFilterCombo.setPrefWidth(260);
        courseFilterCombo.setPromptText("All my courses");
        courseFilterCombo.setOnAction(e -> applyFilters());

        Label statusLbl = new Label("Status:");
        statusLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.setItems(FXCollections.observableArrayList(
            "ALL", "ENROLLED", "COMPLETED", "DROPPED"));
        statusFilterCombo.setValue("ALL");
        statusFilterCombo.setOnAction(e -> applyFilters());

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().addAll("button", "btn-primary");
        refreshBtn.setStyle("-fx-padding:6 14;");
        refreshBtn.setOnAction(e -> loadData());

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#38bdf8; -fx-font-weight:bold;");

        totalLabel = new Label();
        totalLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#1d4ed8; -fx-font-weight:bold;");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        filterRow.getChildren().addAll(
            courseLbl, courseFilterCombo, statusLbl, statusFilterCombo,
            sp, totalLabel, statusLabel, refreshBtn);

        top.getChildren().addAll(title, subtitle, filterRow);
        layout.setTop(top);

        // ── Table ─────────────────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No enrollments found for your courses."));

        TableColumn<Enrollment, String> studentCol = new TableColumn<>("Student Name");
        studentCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getStudentName()));
        studentCol.setSortable(true);

        TableColumn<Enrollment, String> courseCol = new TableColumn<>("Course");
        courseCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getCourseName()));
        courseCol.setSortable(true);

        TableColumn<Enrollment, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getCourseCode()));

        TableColumn<Enrollment, String> dateCol = new TableColumn<>("Enrolled On");
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getEnrollmentDate() != null
                ? cd.getValue().getEnrollmentDate().toString() : ""));
        dateCol.setSortable(true);

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
                ? String.format("%.1f  (%s)", cd.getValue().getGrade(), cd.getValue().getLetterGrade())
                : "—"));
        gradeCol.setSortable(true);

        table.getColumns().addAll(studentCol, courseCol, codeCol, dateCol, statusCol, gradeCol);
        table.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> updateDetail(sel));

        grid = new DataGridHelper<>(table);
        grid.addSortOption("Student Name", java.util.Comparator.comparing(
            e -> e.getStudentName() != null ? e.getStudentName() : ""));
        grid.addSortOption("Course Name",  java.util.Comparator.comparing(
            e -> e.getCourseName() != null ? e.getCourseName() : ""));
        grid.addSortOption("Date",         java.util.Comparator.comparing(
            e -> e.getEnrollmentDate() != null ? e.getEnrollmentDate().toString() : ""));
        grid.addSortOption("Status",       java.util.Comparator.comparing(
            e -> e.getStatus() != null ? e.getStatus() : ""));
        grid.addSortOption("Grade",        java.util.Comparator.comparingDouble(
            e -> e.getGrade() != null ? e.getGrade() : -1.0));
        HBox searchBar = grid.buildFilterBarWithSort(
            "Search by student name or course...",
            val -> e -> (e.getStudentName() != null && e.getStudentName().toLowerCase().contains(val))
                     || (e.getCourseName() != null && e.getCourseName().toLowerCase().contains(val))
        );
        HBox paginationBar = grid.buildPaginationBar();

        VBox centerBox = new VBox(0, searchBar, table, paginationBar);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setCenter(centerBox);

        // ── Right detail panel ────────────────────────────────────────
        VBox rightPanel = new VBox(12);
        rightPanel.setPadding(new Insets(16));
        rightPanel.setPrefWidth(250);
        rightPanel.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 0 1;");

        Label detailTitle = new Label("Selected Enrollment");
        detailTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        detailLabel = new Label("← Select an enrollment to see details.");
        detailLabel.setWrapText(true);
        detailLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:10;");

        // Status update (instructor can update status of their own course enrollments)
        Label actionsTitle = new Label("Update Status");
        actionsTitle.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Button markCompleteBtn = new Button("✅  Mark Completed");
        markCompleteBtn.setMaxWidth(Double.MAX_VALUE);
        markCompleteBtn.getStyleClass().addAll("button", "btn-primary");
        markCompleteBtn.setOnAction(e -> handleMarkStatus("COMPLETED"));

        Button markDropBtn = new Button("⊘  Mark Dropped");
        markDropBtn.setMaxWidth(Double.MAX_VALUE);
        markDropBtn.getStyleClass().addAll("button", "btn-danger");
        markDropBtn.setOnAction(e -> handleMarkStatus("DROPPED"));

        Button markEnrolledBtn = new Button("🔄  Mark Enrolled");
        markEnrolledBtn.setMaxWidth(Double.MAX_VALUE);
        markEnrolledBtn.getStyleClass().addAll("button", "btn-primary");
        markEnrolledBtn.setOnAction(e -> handleMarkStatus("ENROLLED"));

        rightPanel.getChildren().addAll(
            detailTitle, detailLabel,
            new Separator(), actionsTitle,
            markCompleteBtn, markDropBtn, markEnrolledBtn
        );
        layout.setRight(rightPanel);
        root.getChildren().add(layout);
    }

    // ── DATA ──────────────────────────────────────────────────────────
    private void loadData() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        // Load ONLY this instructor's courses
        myCourses = courseDAO.getCoursesByInstructor(user.getUserId());

        // Populate course filter combo
        List<Course> comboItems = new ArrayList<>();
        comboItems.add(null); // "All my courses" option
        comboItems.addAll(myCourses);
        courseFilterCombo.setItems(FXCollections.observableArrayList(comboItems));
        courseFilterCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Course c) {
                return c == null ? "All My Courses" : c.getCourseCode() + " — " + c.getCourseName();
            }
            public Course fromString(String s) { return null; }
        });
        courseFilterCombo.setValue(null);

        // Load all enrollments across all my courses
        List<Enrollment> all = new ArrayList<>();
        for (Course c : myCourses) {
            all.addAll(enrollmentDAO.getEnrollmentsByCourse(c.getCourseId()));
        }

        grid.setData(all);
        totalLabel.setText("Total: " + all.size());
        statusLabel.setText("My Courses: " + myCourses.size());
    }

    private void applyFilters() {
        Course selectedCourse = courseFilterCombo.getValue();
        String selectedStatus = statusFilterCombo.getValue();

        grid.setFilter(e -> {
            boolean courseMatch = selectedCourse == null
                || e.getCourseId() == selectedCourse.getCourseId();
            boolean statusMatch = selectedStatus == null || "ALL".equals(selectedStatus)
                || selectedStatus.equals(e.getStatus());
            return courseMatch && statusMatch;
        });
        totalLabel.setText("Showing: " + grid.getTotalFiltered());
    }

    private void updateDetail(Enrollment e) {
        if (e == null) {
            detailLabel.setText("← Select an enrollment to see details.");
            return;
        }
        detailLabel.setText(
            "Student:  " + e.getStudentName() + "\n" +
            "Course:   " + e.getCourseName() + "\n" +
            "Status:   " + e.getStatus() + "\n" +
            "Grade:    " + (e.getGrade() != null
                ? String.format("%.1f  (%s)", e.getGrade(), e.getLetterGrade())
                : "Not graded") + "\n" +
            "Enrolled: " + (e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "—")
        );
    }

    private void handleMarkStatus(String status) {
        Enrollment e = table.getSelectionModel().getSelectedItem();
        if (e == null) { statusLabel.setText("⚠ Select an enrollment first."); return; }

        // Security: verify this course belongs to the instructor
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null && !courseDAO.isCourseTaughtByInstructor(e.getCourseId(), user.getUserId())) {
            statusLabel.setText("⚠ You can only manage your own course enrollments.");
            return;
        }

        if ("DROPPED".equals(status)) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Mark " + e.getStudentName() + " as DROPPED?", ButtonType.YES, ButtonType.NO);
            confirm.showAndWait().ifPresent(r -> {
                if (r == ButtonType.YES) {
                    enrollmentDAO.updateEnrollmentStatus(e.getEnrollmentId(), status);
                    loadData();
                    statusLabel.setText("✓ Marked as DROPPED: " + e.getStudentName());
                }
            });
        } else {
            enrollmentDAO.updateEnrollmentStatus(e.getEnrollmentId(), status);
            loadData();
            statusLabel.setText("✓ Marked as " + status + ": " + e.getStudentName());
        }
    }
}
