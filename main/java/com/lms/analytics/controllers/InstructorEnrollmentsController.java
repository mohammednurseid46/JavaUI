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

public class InstructorEnrollmentsController {

    @FXML private StackPane root;

    private TableView<Enrollment> table;
    private DataGridHelper<Enrollment> grid;
    private Label statusLabel, detailLabel;
    private ComboBox<Course> courseFilterCombo;
    private ComboBox<String> statusFilterCombo;

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
        top.setPadding(new Insets(16, 20, 10, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        Label title = new Label("📝  My Course Enrollments");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label subtitle = new Label("Showing only students enrolled in your courses.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#38bdf8; -fx-font-weight:bold;");

        // Filters row
        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);

        Label courseLbl = new Label("Course:");
        courseLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        courseFilterCombo = new ComboBox<>();
        courseFilterCombo.setPromptText("All my courses");
        courseFilterCombo.setPrefWidth(260);
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
        refreshBtn.setStyle("-fx-padding:5 14;");
        refreshBtn.setOnAction(e -> loadData());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        filterRow.getChildren().addAll(
            courseLbl, courseFilterCombo,
            statusLbl, statusFilterCombo,
            sp, statusLabel, refreshBtn);

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
        codeCol.setSortable(true);

        TableColumn<Enrollment, String> dateCol = new TableColumn<>("Enrolled On");
        dateCol.setCellValueFactory(cd -> {
            java.time.LocalDate d = cd.getValue().getEnrollmentDate();
            if (d != null) return new SimpleStringProperty(d.toString());
            // Fallback: show enrollment ID date if available
            return new SimpleStringProperty("—");
        });
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

        TableColumn<Enrollment, String> gradeCol = new TableColumn<>("Grade (%)");
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

        table.getColumns().addAll(
            studentCol, courseCol, codeCol, dateCol, statusCol, gradeCol, letterCol);

        // Master-detail
        table.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> updateDetail(sel));

        grid = new DataGridHelper<>(table);

        // Sort options for instructor enrollments
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
        HBox paging = grid.buildPaginationBar();

        VBox center = new VBox(0, searchBar, table, paging);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setCenter(center);

        // ── Right detail panel ────────────────────────────────────────
        VBox rightPanel = new VBox(12);
        rightPanel.setPadding(new Insets(16));
        rightPanel.setPrefWidth(240);
        rightPanel.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 0 1;");

        Label detailTitle = new Label("Selected Enrollment");
        detailTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        detailLabel = new Label("← Click a row to see details.");
        detailLabel.setWrapText(true);
        detailLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:10;");

        rightPanel.getChildren().addAll(detailTitle, detailLabel);
        layout.setRight(rightPanel);
        root.getChildren().add(layout);
    }

    private void updateDetail(Enrollment e) {
        if (e == null) { detailLabel.setText("← Click a row to see details."); return; }
        detailLabel.setText(
            "Student:  " + e.getStudentName() + "\n" +
            "Course:   " + e.getCourseName() + "\n" +
            "Code:     " + e.getCourseCode() + "\n" +
            "Status:   " + e.getStatus() + "\n" +
            "Grade:    " + (e.getGrade() != null
                ? String.format("%.1f%%  (%s)", e.getGrade(), e.getLetterGrade())
                : "Not graded") + "\n" +
            "Enrolled: " + (e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "—")
        );
    }

    // ── DATA — only instructor's own courses ──────────────────────────
    private void loadData() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        // Load only THIS instructor's courses
        myCourses = courseDAO.getCoursesByInstructor(user.getUserId());

        // Populate course filter combo
        List<Course> comboItems = new ArrayList<>();
        comboItems.add(null); // null = "All my courses"
        comboItems.addAll(myCourses);
        courseFilterCombo.setItems(FXCollections.observableArrayList(comboItems));
        courseFilterCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Course c) {
                return c == null ? "All my courses" : c.getCourseCode() + " — " + c.getCourseName();
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
        statusLabel.setText("Total: " + all.size() + " enrollment(s) across " + myCourses.size() + " course(s)");
    }

    private void applyFilters() {
        Course selectedCourse = courseFilterCombo.getValue();
        String selectedStatus = statusFilterCombo.getValue();

        // Reload base data
        List<Enrollment> all = new ArrayList<>();
        List<Course> coursesToLoad = (selectedCourse != null)
            ? List.of(selectedCourse) : myCourses;

        for (Course c : coursesToLoad) {
            all.addAll(enrollmentDAO.getEnrollmentsByCourse(c.getCourseId()));
        }

        grid.setData(all);

        // Apply status filter
        if (selectedStatus != null && !"ALL".equals(selectedStatus)) {
            grid.setFilter(e -> selectedStatus.equals(e.getStatus()));
        } else {
            grid.clearFilter();
        }

        statusLabel.setText("Showing: " + grid.getTotalFiltered() + " enrollment(s)");
    }
}
