package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.lms.analytics.dao.EnrollmentDAO;
import com.lms.analytics.dao.StudentDAO;
import com.lms.analytics.models.Enrollment;
import com.lms.analytics.models.Student;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.DataGridHelper;
import com.lms.analytics.utils.SessionManager;

import java.util.ArrayList;
import java.util.List;

public class MyGradesController {

    @FXML private StackPane root;

    private TableView<Enrollment> table;
    private Label statusLabel, gpaLabel, detailLabel;
    private ComboBox<String> gradeFilterCombo;
    private BarChart<String, Number> gradeChart;
    private DataGridHelper<Enrollment> grid;

    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final StudentDAO    studentDAO    = new StudentDAO();
    private Student currentStudent;
    private User    currentUser;

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
        loadGrades();
    }

    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");

        // ── Top bar ───────────────────────────────────────────────────
        VBox top = new VBox(8);
        top.setPadding(new Insets(16, 20, 10, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        Label title = new Label("🎓  My Grades");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label subtitle = new Label("View your grades across all courses. Click a row to see details.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#38bdf8;");

        // Grade filter
        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        Label filterLbl = new Label("Filter by Grade:");
        filterLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");
        gradeFilterCombo = new ComboBox<>();
        gradeFilterCombo.setItems(FXCollections.observableArrayList(
            "ALL", "A (90-100)", "B (80-89)", "C (70-79)", "D (60-69)", "F (<60)", "Not Graded"));
        gradeFilterCombo.setValue("ALL");
        gradeFilterCombo.setOnAction(e -> applyGradeFilter());

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().addAll("button", "btn-primary");
        refreshBtn.setStyle("-fx-padding:5 14;");
        refreshBtn.setOnAction(e -> loadGrades());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        filterRow.getChildren().addAll(filterLbl, gradeFilterCombo, sp, statusLabel, refreshBtn);
        top.getChildren().addAll(title, subtitle, filterRow);
        layout.setTop(top);

        // ── Table ─────────────────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        table.setPlaceholder(new Label("No grades yet. Enroll in courses to see your grades here."));

        TableColumn<Enrollment, String> courseCol = new TableColumn<>("Course Name");
        courseCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseName()));
        courseCol.setSortable(true);

        TableColumn<Enrollment, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseCode()));
        codeCol.setSortable(true);

        TableColumn<Enrollment, String> gradeCol = new TableColumn<>("Grade (%)");
        gradeCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getGrade() != null
                ? String.format("%.1f", cd.getValue().getGrade()) : "Pending"));
        gradeCol.setSortable(true);

        TableColumn<Enrollment, String> letterCol = new TableColumn<>("Letter");
        letterCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getLetterGrade()));
        letterCol.setSortable(true);
        letterCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("A".equals(s) ? "-fx-text-fill:#16a34a; -fx-font-weight:bold; -fx-font-size:14px;"
                       : "B".equals(s) ? "-fx-text-fill:#1d4ed8; -fx-font-weight:bold; -fx-font-size:14px;"
                       : "C".equals(s) ? "-fx-text-fill:#b45309; -fx-font-weight:bold; -fx-font-size:14px;"
                       : "F".equals(s) ? "-fx-text-fill:#dc2626; -fx-font-weight:bold; -fx-font-size:14px;"
                       : "-fx-text-fill:#64748b;");
            }
        });

        TableColumn<Enrollment, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        statusCol.setSortable(true);

        TableColumn<Enrollment, String> completedCol = new TableColumn<>("Completed");
        completedCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getCompletionDate() != null
                ? cd.getValue().getCompletionDate().toString() : "—"));

        table.getColumns().addAll(courseCol, codeCol, gradeCol, letterCol, statusCol, completedCol);

        // Master-detail wiring
        table.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, sel) -> updateDetailPanel(sel));

        grid = new DataGridHelper<>(table);

        // Sort options for My Grades
        grid.addSortOption("Course Name", java.util.Comparator.comparing(
            e -> e.getCourseName() != null ? e.getCourseName() : ""));
        grid.addSortOption("Grade",       java.util.Comparator.comparingDouble(
            e -> e.getGrade() != null ? e.getGrade() : -1.0));
        grid.addSortOption("Status",      java.util.Comparator.comparing(
            e -> e.getStatus() != null ? e.getStatus() : ""));
        grid.addSortOption("Date",        java.util.Comparator.comparing(
            e -> e.getEnrollmentDate() != null ? e.getEnrollmentDate().toString() : ""));

        HBox searchBar = grid.buildFilterBarWithSort(
            "Search by course name or code...",
            val -> e -> (e.getCourseName() != null && e.getCourseName().toLowerCase().contains(val))
                     || (e.getCourseCode() != null && e.getCourseCode().toLowerCase().contains(val))
        );
        HBox paginationBar = grid.buildPaginationBar();

        // Chart
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 10);
        y.setLabel("Grade (%)");
        gradeChart = new BarChart<>(x, y);
        gradeChart.setAnimated(false);
        gradeChart.setLegendVisible(false);
        gradeChart.setPrefHeight(180);
        gradeChart.setStyle(
            "-fx-background-color:white; " +
            "-fx-plot-background-color:white;");
        // Style axis labels light blue
        x.setStyle("-fx-tick-label-fill:#38bdf8;");
        y.setStyle("-fx-tick-label-fill:#64748b;");

        VBox centerBox = new VBox(0, searchBar, table, paginationBar, gradeChart);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setCenter(centerBox);

        // ── Right: detail + GPA panel ─────────────────────────────────
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(230);
        panel.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 0 1;");

        Label panelTitle = new Label("Selected Course");
        panelTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        detailLabel = new Label("← Click a row to see details.");
        detailLabel.setWrapText(true);
        detailLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:10;");

        Label gpaTitle = new Label("Average Grade");
        gpaTitle.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        gpaLabel = new Label("—");
        gpaLabel.setStyle("-fx-font-size:32px; -fx-font-weight:bold; -fx-text-fill:#1f6feb;");

        panel.getChildren().addAll(panelTitle, detailLabel, new Separator(), gpaTitle, gpaLabel);
        layout.setRight(panel);
        root.getChildren().add(layout);
    }

    private void updateDetailPanel(Enrollment e) {
        if (e == null) { detailLabel.setText("← Click a row to see details."); return; }
        detailLabel.setText(
            "Course:  " + e.getCourseName() + "\n" +
            "Code:    " + e.getCourseCode() + "\n" +
            "Grade:   " + (e.getGrade() != null
                ? String.format("%.1f%%  (%s)", e.getGrade(), e.getLetterGrade())
                : "Not graded yet") + "\n" +
            "Status:  " + e.getStatus() + "\n" +
            "Done:    " + (e.getCompletionDate() != null ? e.getCompletionDate() : "—")
        );
    }

    private void applyGradeFilter() {
        String f = gradeFilterCombo.getValue();
        if (f == null || "ALL".equals(f)) { grid.clearFilter(); return; }
        grid.setFilter(e -> {
            if ("Not Graded".equals(f)) return e.getGrade() == null;
            if (e.getGrade() == null) return false;
            double g = e.getGrade();
            return switch (f) {
                case "A (90-100)" -> g >= 90;
                case "B (80-89)"  -> g >= 80 && g < 90;
                case "C (70-79)"  -> g >= 70 && g < 80;
                case "D (60-69)"  -> g >= 60 && g < 70;
                case "F (<60)"    -> g < 60;
                default -> true;
            };
        });
    }

    private void loadGrades() {
        if (currentUser == null) { statusLabel.setText("Not logged in."); return; }
        List<Enrollment> all = loadMyGrades();
        grid.setData(all);

        double avg = all.stream().filter(e -> e.getGrade() != null)
            .mapToDouble(Enrollment::getGrade).average().orElse(0);
        long graded = all.stream().filter(e -> e.getGrade() != null).count();

        statusLabel.setText("Courses: " + all.size() + "  |  Graded: " + graded);
        gpaLabel.setText(avg > 0 ? String.format("%.1f%%", avg) : "N/A");

        // Update chart
        gradeChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        all.stream().filter(e -> e.getGrade() != null).forEach(e ->
            series.getData().add(new XYChart.Data<>(
                e.getCourseCode() != null ? e.getCourseCode() : "?", e.getGrade())));
        gradeChart.getData().add(series);

        // Style bars light blue after chart renders
        javafx.application.Platform.runLater(() -> {
            gradeChart.lookupAll(".chart-bar").forEach(node ->
                node.setStyle(
                    "-fx-bar-fill:#38bdf8; " +
                    "-fx-background-radius:4 4 0 0;"));
            // Style chart plot background white
            javafx.scene.Node plotBg = gradeChart.lookup(".chart-plot-background");
            if (plotBg != null) plotBg.setStyle("-fx-background-color:white;");
        });
    }

    private List<Enrollment> loadMyGrades() {
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

        try (java.sql.Connection conn = com.lms.analytics.utils.DatabaseConnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, studentId > 0 ? studentId : userId);
            ps.setInt(2, userId);
            java.sql.ResultSet rs = ps.executeQuery();
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
        } catch (java.sql.SQLException e) { e.printStackTrace(); }
        return list;
    }
}
