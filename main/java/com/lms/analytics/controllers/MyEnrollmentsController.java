package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.lms.analytics.dao.EnrollmentDAO;
import com.lms.analytics.dao.StudentDAO;
import com.lms.analytics.models.Enrollment;
import com.lms.analytics.models.Student;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.DataGridHelper;
import com.lms.analytics.utils.DatabaseConnection;
import com.lms.analytics.utils.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MyEnrollmentsController {

    @FXML private StackPane root;

    private TableView<Enrollment> table;
    private Label statusLabel, summaryLabel, detailLabel;
    private ComboBox<String> statusFilterCombo;

    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final StudentDAO    studentDAO    = new StudentDAO();
    private DataGridHelper<Enrollment> grid;

    private User    currentUser;
    private Student currentStudent;

    @FXML
    public void initialize() {
        currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            currentStudent = studentDAO.getStudentByUserId(currentUser.getUserId());
            if (currentStudent == null && "STUDENT".equals(currentUser.getRole())) {
                int count = studentDAO.getTotalStudents();
                Student s = new Student(currentUser.getUserId(),
                    String.format("STU%05d", count + 1), "Undeclared");
                s.setEnrollmentDate(java.time.LocalDate.now());
                s.setCurrentSemester(1);
                if (studentDAO.createStudent(s))
                    currentStudent = studentDAO.getStudentByUserId(currentUser.getUserId());
            }
        }
        buildUI();
        loadEnrollments();
    }

    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");

        // ── Top bar ───────────────────────────────────────────────────
        VBox top = new VBox(8);
        top.setPadding(new Insets(16, 20, 10, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        Label title = new Label("📝  My Enrollments");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label subtitle = new Label("All your course enrollments. Click a row to see details.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#38bdf8;");

        // Status filter combo
        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Label filterLbl = new Label("Filter by Status:");
        filterLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");
        statusFilterCombo = new ComboBox<>();
        statusFilterCombo.setItems(FXCollections.observableArrayList(
            "ALL", "ENROLLED", "COMPLETED", "DROPPED"));
        statusFilterCombo.setValue("ALL");
        statusFilterCombo.setOnAction(e -> applyStatusFilter());

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().addAll("button", "btn-primary");
        refreshBtn.setStyle("-fx-padding:5 14;");
        refreshBtn.setOnAction(e -> loadEnrollments());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        filterRow.getChildren().addAll(filterLbl, statusFilterCombo, sp, statusLabel, refreshBtn);
        top.getChildren().addAll(title, subtitle, filterRow);
        layout.setTop(top);

        // ── Table (sortable columns) ──────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setEditable(false);
        table.setPlaceholder(new Label("No enrollments found. Browse courses to enroll."));

        TableColumn<Enrollment, String> courseCol = new TableColumn<>("Course Name");
        courseCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseName()));
        courseCol.setSortable(true);

        TableColumn<Enrollment, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseCode()));
        codeCol.setSortable(true);

        TableColumn<Enrollment, String> dateCol = new TableColumn<>("Enrolled On");
        dateCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getEnrollmentDate() != null
                ? cd.getValue().getEnrollmentDate().toString() : ""));
        dateCol.setSortable(true);

        TableColumn<Enrollment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
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
        letterCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getLetterGrade()));
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

        TableColumn<Enrollment, String> completedCol = new TableColumn<>("Completed");
        completedCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getCompletionDate() != null
                ? cd.getValue().getCompletionDate().toString() : "—"));

        table.getColumns().addAll(courseCol, codeCol, dateCol, statusCol,
                                  gradeCol, letterCol, completedCol);

        // Wire master-detail: row selection → detail panel
        table.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> updateDetailPanel(sel));

        // ── DataGrid helper (sorting + filtering + pagination) ────────
        grid = new DataGridHelper<>(table);

        // Sort options for My Enrollments
        grid.addSortOption("Course Name", java.util.Comparator.comparing(
            e -> e.getCourseName() != null ? e.getCourseName() : ""));
        grid.addSortOption("Date",        java.util.Comparator.comparing(
            e -> e.getEnrollmentDate() != null ? e.getEnrollmentDate().toString() : ""));
        grid.addSortOption("Status",      java.util.Comparator.comparing(
            e -> e.getStatus() != null ? e.getStatus() : ""));
        grid.addSortOption("Grade",       java.util.Comparator.comparingDouble(
            e -> e.getGrade() != null ? e.getGrade() : -1.0));

        // Search bar
        HBox searchBar = grid.buildFilterBarWithSort(
            "Search by course name or code...",
            val -> e -> (e.getCourseName() != null && e.getCourseName().toLowerCase().contains(val))
                     || (e.getCourseCode() != null && e.getCourseCode().toLowerCase().contains(val))
        );

        // Pagination bar
        HBox paginationBar = grid.buildPaginationBar();

        VBox centerBox = new VBox(0, searchBar, table, paginationBar);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setCenter(centerBox);

        // ── Right: detail + summary panel ────────────────────────────
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(250);
        panel.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 0 1;");

        Label panelTitle = new Label("Selected Course");
        panelTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        detailLabel = new Label("← Click a row to see details.");
        detailLabel.setWrapText(true);
        detailLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:10;");

        Label summaryTitle = new Label("My Summary");
        summaryTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        summaryLabel = new Label("Loading...");
        summaryLabel.setWrapText(true);
        summaryLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        Button dropBtn = new Button("⊘  Drop Selected");
        dropBtn.setMaxWidth(Double.MAX_VALUE);
        dropBtn.getStyleClass().addAll("button", "btn-danger");
        dropBtn.setOnAction(e -> handleDrop());

        panel.getChildren().addAll(
            panelTitle, detailLabel,
            new Separator(), summaryTitle, summaryLabel,
            new Separator(), dropBtn
        );
        layout.setRight(panel);
        root.getChildren().add(layout);
    }

    // ── MASTER-DETAIL ─────────────────────────────────────────────────
    private void updateDetailPanel(Enrollment e) {
        if (e == null) {
            detailLabel.setText("← Click a row to see details.");
            return;
        }
        detailLabel.setText(
            "Course:    " + e.getCourseName() + "\n" +
            "Code:      " + e.getCourseCode() + "\n" +
            "Status:    " + e.getStatus() + "\n" +
            "Grade:     " + (e.getGrade() != null
                ? String.format("%.1f  (%s)", e.getGrade(), e.getLetterGrade())
                : "Not graded yet") + "\n" +
            "Enrolled:  " + (e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "—") + "\n" +
            "Completed: " + (e.getCompletionDate() != null ? e.getCompletionDate() : "—")
        );
    }

    // ── LOAD DATA ─────────────────────────────────────────────────────
    private void loadEnrollments() {
        if (currentUser == null) { statusLabel.setText("Not logged in."); return; }
        List<Enrollment> all = loadMyEnrollments();
        grid.setData(all);
        updateSummary(all);
        statusLabel.setText("Total: " + all.size() + " enrollment(s)");
    }

    private void applyStatusFilter() {
        String filter = statusFilterCombo.getValue();
        if (filter == null || "ALL".equals(filter)) {
            grid.clearFilter();
        } else {
            grid.setFilter(e -> filter.equals(e.getStatus()));
        }
    }

    private void updateSummary(List<Enrollment> all) {
        long completed = all.stream().filter(e -> "COMPLETED".equals(e.getStatus())).count();
        long active    = all.stream().filter(e -> "ENROLLED".equals(e.getStatus())).count();
        long dropped   = all.stream().filter(e -> "DROPPED".equals(e.getStatus())).count();
        double avg     = all.stream().filter(e -> e.getGrade() != null)
            .mapToDouble(Enrollment::getGrade).average().orElse(0);

        String stuNum = currentStudent != null ? currentStudent.getStudentNumber() : "N/A";
        String major  = currentStudent != null && currentStudent.getMajor() != null
            ? currentStudent.getMajor() : "—";
        int sem = currentStudent != null ? currentStudent.getCurrentSemester() : 1;

        summaryLabel.setText(
            "Name:      " + (currentUser != null ? currentUser.getFullName() : "—") + "\n" +
            "Student #: " + stuNum + "\n" +
            "Major:     " + major + "\n" +
            "Semester:  " + sem + "\n\n" +
            "Total:     " + all.size() + "\n" +
            "Active:    " + active + "\n" +
            "Completed: " + completed + "\n" +
            "Dropped:   " + dropped + "\n" +
            "Avg Grade: " + (avg > 0 ? String.format("%.1f%%", avg) : "N/A")
        );
    }

    private List<Enrollment> loadMyEnrollments() {
        List<Enrollment> list = new ArrayList<>();
        if (currentUser == null) return list;
        int userId    = currentUser.getUserId();
        int studentId = currentStudent != null ? currentStudent.getStudentId() : -1;

        String sql =
            "SELECT e.enrollment_id, e.student_id, e.course_id, " +
            "       e.enrollment_date, e.status, e.grade, e.completion_date, " +
            "       COALESCE(c.course_code, '') AS course_code, " +
            "       COALESCE(c.course_name, 'Unknown Course') AS course_name " +
            "FROM enrollments e " +
            "LEFT JOIN courses c ON e.course_id = c.course_id " +
            "WHERE e.student_id = ? OR e.student_id = ? " +
            "ORDER BY e.enrollment_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId > 0 ? studentId : userId);
            ps.setInt(2, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Enrollment en = new Enrollment();
                en.setEnrollmentId(rs.getInt("enrollment_id"));
                en.setStudentId(rs.getInt("student_id"));
                en.setCourseId(rs.getInt("course_id"));
                en.setCourseName(rs.getString("course_name"));
                en.setCourseCode(rs.getString("course_code"));
                en.setStatus(rs.getString("status"));
                // Use getString to avoid SQLite date parsing issues
                String enrollDateStr = rs.getString("enrollment_date");
                if (enrollDateStr != null && !enrollDateStr.isBlank()) {
                    try { en.setEnrollmentDate(java.time.LocalDate.parse(enrollDateStr.substring(0, 10))); }
                    catch (Exception ignored) {}
                }
                double grade = rs.getDouble("grade");
                if (!rs.wasNull()) en.setGrade(grade);
                String compDateStr = rs.getString("completion_date");
                if (compDateStr != null && !compDateStr.isBlank()) {
                    try { en.setCompletionDate(java.time.LocalDate.parse(compDateStr.substring(0, 10))); }
                    catch (Exception ignored) {}
                }
                list.add(en);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private void handleDrop() {
        Enrollment e = table.getSelectionModel().getSelectedItem();
        if (e == null) { statusLabel.setText("⚠ Select an enrollment to drop."); return; }
        if ("COMPLETED".equals(e.getStatus())) {
            statusLabel.setText("⚠ Cannot drop a completed course.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Drop: " + e.getCourseName() + "?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Drop");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                enrollmentDAO.dropEnrollment(e.getEnrollmentId());
                loadEnrollments();
                statusLabel.setText("✓ Dropped: " + e.getCourseName());
            }
        });
    }
}
