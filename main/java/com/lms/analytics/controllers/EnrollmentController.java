package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.util.StringConverter;

import com.lms.analytics.dao.CourseDAO;
import com.lms.analytics.dao.EnrollmentDAO;
import com.lms.analytics.dao.StudentDAO;
import com.lms.analytics.dao.UserDAO;
import com.lms.analytics.models.Course;
import com.lms.analytics.models.Enrollment;
import com.lms.analytics.models.Student;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.DataGridHelper;
import com.lms.analytics.utils.DatabaseConnection;
import com.lms.analytics.utils.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EnrollmentController {

    @FXML private TableView<Enrollment> enrollmentTable;
    @FXML private TableColumn<Enrollment, String> studentCol;
    @FXML private TableColumn<Enrollment, String> courseCol;
    @FXML private TableColumn<Enrollment, String> dateCol;
    @FXML private TableColumn<Enrollment, String> statusCol;
    @FXML private TableColumn<Enrollment, Double> gradeCol;

    @FXML private ComboBox<Student> studentCombo;
    @FXML private ComboBox<Course>  courseCombo;
    @FXML private ComboBox<String>  statusFilterCombo;
    @FXML private Label statusLabel;
    @FXML private Label totalLabel;
    @FXML private Label selectedInfoLabel;
    @FXML private Button sortMenuBtn;
    @FXML private javafx.scene.layout.VBox  tableContainer;
    @FXML private javafx.scene.layout.HBox  paginationContainer;

    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final StudentDAO    studentDAO    = new StudentDAO();
    private final CourseDAO     courseDAO     = new CourseDAO();
    private final UserDAO       userDAO       = new UserDAO();
    private DataGridHelper<Enrollment> grid;
    private String currentSortKey = null;
    private boolean sortAscending = true;

    @FXML
    public void initialize() {
        setupTableColumns();
        setupComboBoxes();

        // DataGrid: sorting + filtering + pagination
        grid = new DataGridHelper<>(enrollmentTable);

        // Sort options for admin enrollment table
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

        // ── Inject search bar above the table ─────────────────────────
        javafx.scene.layout.HBox searchBar = grid.buildFilterBarWithSort(
            "Search by student name or course...",
            val -> e -> (e.getStudentName() != null && e.getStudentName().toLowerCase().contains(val))
                     || (e.getCourseName()  != null && e.getCourseName().toLowerCase().contains(val))
                     || (e.getCourseCode()  != null && e.getCourseCode().toLowerCase().contains(val))
        );

        // ── Inject pagination bar below the table ─────────────────────
        javafx.scene.layout.HBox paginationBar = grid.buildPaginationBar();

        // Insert search bar at index 0 (above the table), replace placeholder HBox with real bar
        if (tableContainer != null) {
            tableContainer.getChildren().add(0, searchBar);
            // Replace the empty paginationContainer placeholder with the real pagination bar
            if (paginationContainer != null) {
                int idx = tableContainer.getChildren().indexOf(paginationContainer);
                if (idx >= 0) {
                    tableContainer.getChildren().set(idx, paginationBar);
                } else {
                    tableContainer.getChildren().add(paginationBar);
                }
            }
        }

        // Load all data fresh from DB — grid manages the table items
        loadEnrollments();

        // Wire master-detail
        enrollmentTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> updateDetailPanel(selected));
    }

    // ── MASTER-DETAIL PANEL ───────────────────────────────────────────
    private void updateDetailPanel(Enrollment e) {
        if (selectedInfoLabel == null) return;
        if (e == null) {
            selectedInfoLabel.setText("← Select an enrollment from the table.");
            return;
        }
        selectedInfoLabel.setText(
            "Student:  " + e.getStudentName() + "\n" +
            "Course:   " + e.getCourseName() + "\n" +
            "Status:   " + e.getStatus() + "\n" +
            "Grade:    " + (e.getGrade() != null
                ? String.format("%.1f  (%s)", e.getGrade(), e.getLetterGrade())
                : "Not graded — assigned by instructor") + "\n" +
            "Enrolled: " + (e.getEnrollmentDate() != null ? e.getEnrollmentDate() : "—")
        );
    }

    // ── TABLE COLUMNS ─────────────────────────────────────────────────
    private void setupTableColumns() {
        enrollmentTable.setPlaceholder(new Label("No enrollments found."));
        enrollmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        studentCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getStudentName()));
        courseCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getCourseName()));
        dateCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getEnrollmentDate() != null
                ? cd.getValue().getEnrollmentDate().toString() : ""));
        statusCol.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().getStatus()));
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
        gradeCol.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().getGrade()));
        // table items managed by DataGridHelper
    }

    // ── COMBO BOXES ───────────────────────────────────────────────────
    private void setupComboBoxes() {
        if (statusFilterCombo != null)
            statusFilterCombo.setItems(FXCollections.observableArrayList(
                "ALL", "ENROLLED", "COMPLETED", "DROPPED"));

        if (studentCombo != null) {
            studentCombo.setItems(FXCollections.observableArrayList(getAllStudentsForCombo()));
            studentCombo.setConverter(new StringConverter<>() {
                public String toString(Student s) {
                    if (s == null) return "";
                    String num = s.getStudentNumber() != null ? s.getStudentNumber() : "";
                    String name = s.getFullName() != null ? s.getFullName() : "Unknown";
                    return name + (num.isEmpty() ? "" : "  (" + num + ")");
                }
                public Student fromString(String str) { return null; }
            });
            studentCombo.setPromptText("Select a student...");
        }

        if (courseCombo != null) {
            courseCombo.setItems(FXCollections.observableArrayList(courseDAO.getActiveCourses()));
            courseCombo.setConverter(new StringConverter<>() {
                public String toString(Course c) {
                    return c == null ? "" : c.getCourseCode() + " — " + c.getCourseName();
                }
                public Course fromString(String str) { return null; }
            });
            courseCombo.setPromptText("Select a course...");
        }
    }

    private List<Student> getAllStudentsForCombo() {
        List<Student> students = studentDAO.getAllStudents();
        List<User> studentUsers = userDAO.getUsersByRole("STUDENT");
        java.util.Set<Integer> existingIds = students.stream()
            .map(Student::getUserId)
            .collect(java.util.stream.Collectors.toSet());

        for (User u : studentUsers) {
            if (!existingIds.contains(u.getUserId())) {
                int count = studentDAO.getTotalStudents();
                Student s = new Student(u.getUserId(),
                    String.format("STU%05d", count + 1), "Undeclared");
                s.setEnrollmentDate(java.time.LocalDate.now());
                s.setCurrentSemester(1);
                if (studentDAO.createStudent(s)) {
                    Student created = studentDAO.getStudentByUserId(u.getUserId());
                    if (created != null) students.add(created);
                } else {
                    Student synthetic = new Student();
                    synthetic.setUserId(u.getUserId());
                    synthetic.setFullName(u.getFullName());
                    synthetic.setEmail(u.getEmail());
                    synthetic.setStudentNumber("STU-" + u.getUserId());
                    synthetic.setMajor("Undeclared");
                    students.add(synthetic);
                }
            }
        }
        return students;
    }

    // ── LOAD ENROLLMENTS ──────────────────────────────────────────────
    private void loadEnrollments() {
        grid.setData(loadAllEnrollmentsBulletproof());
        if (totalLabel != null)
            totalLabel.setText("Total: " + grid.getTotalFiltered() + " enrollments");
    }

    private List<Enrollment> loadAllEnrollmentsBulletproof() {
        List<Enrollment> list = new ArrayList<>();
        String sql =
            "SELECT e.enrollment_id, e.student_id, e.course_id, " +
            "       e.enrollment_date, e.status, e.grade, e.completion_date, " +
            "       COALESCE(u1.full_name, u2.full_name, u3.full_name, " +
            "                'Student #' || e.student_id) AS student_name, " +
            "       COALESCE(c.course_code, '') AS course_code, " +
            "       COALESCE(c.course_name, 'Unknown Course') AS course_name " +
            "FROM enrollments e " +
            "LEFT JOIN students s1 ON e.student_id = s1.student_id " +
            "LEFT JOIN users    u1 ON s1.user_id   = u1.user_id " +
            "LEFT JOIN users    u2 ON e.student_id = u2.user_id " +
            "LEFT JOIN students s3 ON e.student_id = s3.user_id " +
            "LEFT JOIN users    u3 ON s3.user_id   = u3.user_id " +
            "LEFT JOIN courses  c  ON e.course_id  = c.course_id " +
            "ORDER BY e.enrollment_date DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Enrollment en = new Enrollment();
                en.setEnrollmentId(rs.getInt("enrollment_id"));
                en.setStudentId(rs.getInt("student_id"));
                en.setCourseId(rs.getInt("course_id"));
                en.setStudentName(rs.getString("student_name"));
                en.setCourseName(rs.getString("course_name"));
                en.setCourseCode(rs.getString("course_code"));
                en.setStatus(rs.getString("status"));
                // Use getString to avoid SQLite date parsing issues
                String enrollStr = rs.getString("enrollment_date");
                if (enrollStr != null && !enrollStr.isBlank()) {
                    try { en.setEnrollmentDate(java.time.LocalDate.parse(enrollStr.substring(0, 10))); }
                    catch (Exception ignored) {}
                }
                double grade = rs.getDouble("grade");
                if (!rs.wasNull()) en.setGrade(grade);
                String compStr = rs.getString("completion_date");
                if (compStr != null && !compStr.isBlank()) {
                    try { en.setCompletionDate(java.time.LocalDate.parse(compStr.substring(0, 10))); }
                    catch (Exception ignored) {}
                }
                list.add(en);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    // ── ACTIONS ───────────────────────────────────────────────────────
    @FXML
    private void handleEnroll() {
        Student student = studentCombo != null ? studentCombo.getValue() : null;
        Course  course  = courseCombo  != null ? courseCombo.getValue()  : null;

        if (student == null || course == null) {
            setStatus("⚠ Please select both a student and a course.");
            return;
        }

        int sid = student.getStudentId();
        if (sid <= 0) {
            Student fromDb = studentDAO.getStudentByUserId(student.getUserId());
            if (fromDb != null && fromDb.getStudentId() > 0) {
                sid = fromDb.getStudentId();
            } else {
                int count = studentDAO.getTotalStudents();
                Student newS = new Student(student.getUserId(),
                    String.format("STU%05d", count + 1), "Undeclared");
                newS.setEnrollmentDate(java.time.LocalDate.now());
                newS.setCurrentSemester(1);
                if (studentDAO.createStudent(newS)) {
                    Student created = studentDAO.getStudentByUserId(student.getUserId());
                    if (created != null) sid = created.getStudentId();
                }
                if (sid <= 0) {
                    setStatus("⚠ Cannot resolve student ID.");
                    return;
                }
            }
        }

        if (enrollmentDAO.isEnrolled(sid, course.getCourseId())) {
            setStatus("⚠ " + student.getFullName() + " is already enrolled.");
            return;
        }
        if (course.getAvailableSeats() <= 0) {
            setStatus("⚠ No seats available in " + course.getCourseName());
            return;
        }
        if (enrollmentDAO.enrollStudent(sid, course.getCourseId())) {
            loadEnrollments();
            setupComboBoxes();
            setStatus("✓ Enrolled " + student.getFullName() + " in " + course.getCourseName());
        } else {
            setStatus("⚠ Enrollment failed.");
        }
    }

    @FXML
    private void handleUpdateStatus() {
        Enrollment selected = enrollmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("⚠ Select an enrollment first."); return; }
        ChoiceDialog<String> dialog = new ChoiceDialog<>(selected.getStatus(),
            "ENROLLED", "COMPLETED", "DROPPED");
        dialog.setTitle("Update Status");
        dialog.setHeaderText("Change status for: " + selected.getStudentName());
        dialog.showAndWait().ifPresent(status -> {
            if (enrollmentDAO.updateEnrollmentStatus(selected.getEnrollmentId(), status)) {
                loadEnrollments();
                setStatus("✓ Status updated to " + status);
            } else {
                setStatus("⚠ Failed to update status.");
            }
        });
    }

    @FXML
    private void handleDrop() {
        Enrollment selected = enrollmentTable.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("⚠ Select an enrollment to drop."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Drop " + selected.getStudentName() + " from " + selected.getCourseName() + "?",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Drop");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (enrollmentDAO.dropEnrollment(selected.getEnrollmentId())) {
                    loadEnrollments();
                    setStatus("✓ Enrollment dropped.");
                } else {
                    setStatus("⚠ Failed to drop enrollment.");
                }
            }
        });
    }

    @FXML
    private void handleFilterByStatus() {
        if (statusFilterCombo == null) return;
        String filter = statusFilterCombo.getValue();
        if (filter == null || "ALL".equals(filter)) {
            grid.clearFilter();
        } else {
            // Reload fresh data first, then apply filter
            grid.setData(loadAllEnrollmentsBulletproof());
            grid.setFilter(e -> filter.equals(e.getStatus()));
        }
        if (totalLabel != null) totalLabel.setText("Total: " + grid.getTotalFiltered());
    }

    @FXML
    private void handleRefresh() {
        // Reset status filter combo to ALL
        if (statusFilterCombo != null) statusFilterCombo.setValue("ALL");
        // Clear any active filter in the grid
        grid.clearFilter();
        // Reload fresh data from DB
        setupComboBoxes();
        loadEnrollments();
        setStatus("✓ Refreshed — showing all enrollments.");
    }

    @FXML
    private void handleBack() {
        com.lms.analytics.utils.NavigationUtil.backToDashboard(enrollmentTable);
    }

    @FXML
    private void handleShowSortMenu() {
        if (sortMenuBtn == null) return;
        // Build and show the sort context menu anchored to the Sort button
        javafx.scene.control.ContextMenu menu = new javafx.scene.control.ContextMenu();
        menu.setStyle(
            "-fx-background-color:white; -fx-background-radius:10; " +
            "-fx-border-color:#e2e8f0; -fx-border-radius:10; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);");

        // Title (non-clickable)
        javafx.scene.control.MenuItem titleItem = new javafx.scene.control.MenuItem("Sort by");
        titleItem.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8; -fx-font-weight:bold; -fx-padding:6 16 4 16;");
        titleItem.setDisable(true);
        menu.getItems().add(titleItem);
        menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());

        // Sort options
        String[][] options = {
            {"Student Name", "studentName"},
            {"Course Name",  "courseName"},
            {"Date",         "date"},
            {"Status",       "status"},
            {"Grade",        "grade"}
        };

        for (String[] opt : options) {
            String key = opt[0];
            boolean isActive = key.equals(currentSortKey);
            String label = isActive
                ? "✓  " + key + "  " + (sortAscending ? "↑" : "↓")
                : "      " + key;

            javafx.scene.control.MenuItem item = new javafx.scene.control.MenuItem(label);
            item.setStyle("-fx-font-size:13px; -fx-padding:10 20; " +
                (isActive ? "-fx-text-fill:#0284c7; -fx-font-weight:bold;" : "-fx-text-fill:#1e293b;"));
            item.setOnAction(ev -> {
                if (key.equals(currentSortKey)) {
                    sortAscending = !sortAscending;
                } else {
                    currentSortKey = key;
                    sortAscending = true;
                }
                sortEnrollmentsBy(key);
                sortMenuBtn.setText("⇅  " + key + " " + (sortAscending ? "↑" : "↓"));
            });
            menu.getItems().add(item);
        }

        menu.getItems().add(new javafx.scene.control.SeparatorMenuItem());
        javafx.scene.control.MenuItem clearItem = new javafx.scene.control.MenuItem("✕  Clear Sort");
        clearItem.setStyle("-fx-font-size:12px; -fx-text-fill:#94a3b8; -fx-padding:8 20;");
        clearItem.setOnAction(ev -> {
            currentSortKey = null;
            sortAscending = true;
            loadEnrollments();
            sortMenuBtn.setText("⇅  Sort");
        });
        menu.getItems().add(clearItem);

        menu.show(sortMenuBtn, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private void sortEnrollmentsBy(String key) {
        java.util.Comparator<Enrollment> comp = switch (key) {
            case "Student Name" -> java.util.Comparator.comparing(
                e -> e.getStudentName() != null ? e.getStudentName() : "");
            case "Course Name"  -> java.util.Comparator.comparing(
                e -> e.getCourseName() != null ? e.getCourseName() : "");
            case "Date"         -> java.util.Comparator.comparing(
                e -> e.getEnrollmentDate() != null ? e.getEnrollmentDate().toString() : "");
            case "Status"       -> java.util.Comparator.comparing(
                e -> e.getStatus() != null ? e.getStatus() : "");
            case "Grade"        -> java.util.Comparator.comparingDouble(
                e -> e.getGrade() != null ? e.getGrade() : -1.0);
            default -> null;
        };
        if (comp != null) grid.applyExternalSort(comp);
    }

    private void updateTotalLabel() {
        if (totalLabel != null) totalLabel.setText("Total: " + grid.getTotalFiltered());
    }

    private void setStatus(String msg) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        if (msg.startsWith("⚠") || msg.startsWith("✗"))
            statusLabel.setStyle("-fx-text-fill:#dc2626; -fx-font-size:12px;");
        else if (msg.startsWith("✓"))
            statusLabel.setStyle("-fx-text-fill:#16a34a; -fx-font-size:12px;");
        else
            statusLabel.setStyle("-fx-text-fill:#38bdf8; -fx-font-size:12px;");
    }
}
