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
import com.lms.analytics.dao.StudentDAO;
import com.lms.analytics.models.Course;
import com.lms.analytics.models.Enrollment;
import com.lms.analytics.models.Student;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.DataGridHelper;
import com.lms.analytics.utils.SessionManager;

import java.time.LocalDate;

public class BrowseCoursesController {

    @FXML private StackPane root;

    private TableView<Course> table;
    private DataGridHelper<Course> grid;
    private TextField searchField;
    private Label statusLabel;

    // Detail panel
    private Label detailLabel;
    private Label enrollStatusLabel;
    private Button enrollBtn;

    private final CourseDAO    courseDAO    = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();
    private final StudentDAO   studentDAO   = new StudentDAO();
    private final ObservableList<Course> courseList = FXCollections.observableArrayList();
    private Student currentStudent;

    @FXML
    public void initialize() {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) {
            currentStudent = studentDAO.getStudentByUserId(user.getUserId());

            // Auto-create student profile if missing (user is STUDENT but no students row)
            if (currentStudent == null && "STUDENT".equals(user.getRole())) {
                currentStudent = autoCreateStudentProfile(user);
            }
        }
        buildUI();
        loadCourses();
    }

    /**
     * Auto-creates a student profile for users who registered via admin
     * or whose student row was not created properly.
     */
    private Student autoCreateStudentProfile(User user) {
        try {
            int count = studentDAO.getTotalStudents();
            String studentNumber = String.format("STU%05d", count + 1);
            Student s = new Student(user.getUserId(), studentNumber, "Undeclared");
            s.setEnrollmentDate(java.time.LocalDate.now());
            s.setCurrentSemester(1);
            if (studentDAO.createStudent(s)) {
                return studentDAO.getStudentByUserId(user.getUserId());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");

        // ── Top bar ───────────────────────────────────────────────────
        VBox top = new VBox(10);
        top.setPadding(new Insets(16, 20, 12, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        Label title = new Label("📚  Available Courses");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label subtitle = new Label("Browse all active courses and enroll directly from this page.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        HBox searchRow = new HBox(10);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchField = new TextField();
        searchField.setPromptText("🔍  Search by course name, code or description...");
        searchField.setPrefWidth(380);
        searchField.textProperty().addListener((o, old, v) -> filterCourses(v));

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill:#38bdf8; -fx-font-size:12px; -fx-font-weight:bold;");

        Button refreshBtn = new Button("↻  Refresh");
        refreshBtn.getStyleClass().addAll("button", "btn-primary");
        refreshBtn.setOnAction(e -> loadCourses());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        searchRow.getChildren().addAll(searchField, sp, statusLabel, refreshBtn);
        top.getChildren().addAll(title, subtitle, searchRow);
        layout.setTop(top);

        // ── Course table ──────────────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Course, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseCode()));
        codeCol.setPrefWidth(90);

        TableColumn<Course, String> nameCol = new TableColumn<>("Course Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseName()));
        nameCol.setPrefWidth(200);

        TableColumn<Course, String> instrCol = new TableColumn<>("Instructor");
        instrCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getInstructorName() != null ? cd.getValue().getInstructorName() : "TBA"));
        instrCol.setPrefWidth(140);

        TableColumn<Course, String> creditsCol = new TableColumn<>("Credits");
        creditsCol.setCellValueFactory(cd ->
            new SimpleStringProperty(String.valueOf(cd.getValue().getCredits())));
        creditsCol.setPrefWidth(60);

        TableColumn<Course, String> seatsCol = new TableColumn<>("Seats");
        seatsCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getAvailableSeats() + " / " + cd.getValue().getCapacity()));
        seatsCol.setPrefWidth(80);
        // Color seats red when full
        seatsCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(s.startsWith("0") ? "-fx-text-fill:#dc2626; -fx-font-weight:bold;"
                                           : "-fx-text-fill:#16a34a; -fx-font-weight:bold;");
            }
        });

        TableColumn<Course, String> priceCol = new TableColumn<>("Price");
        priceCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getPrice() != null ? "$" + cd.getValue().getPrice().toPlainString() : "Free"));
        priceCol.setPrefWidth(70);

        // Enrollment status column — uses same dual-ID check as the detail panel
        TableColumn<Course, String> myStatusCol = new TableColumn<>("My Status");
        myStatusCol.setCellValueFactory(cd -> {
            boolean enrolled = isStudentEnrolledForColumn(cd.getValue().getCourseId());
            return new SimpleStringProperty(enrolled ? "✓ Enrolled" : "Not Enrolled");
        });
        myStatusCol.setPrefWidth(100);
        myStatusCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle(s.startsWith("✓")
                    ? "-fx-text-fill:#16a34a; -fx-font-weight:bold;"
                    : "-fx-text-fill:#94a3b8;");
            }
        });

        table.getColumns().addAll(codeCol, nameCol, instrCol, creditsCol, seatsCol, priceCol, myStatusCol);
        table.getSelectionModel().selectedItemProperty().addListener(
            (o, old, c) -> { if (c != null) onCourseSelected(c); });

        // Double-click to enroll directly
        table.setRowFactory(tv -> {
            TableRow<Course> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) {
                    handleEnroll(row.getItem());
                }
            });
            return row;
        });

        // DataGrid: sorting + filtering + pagination
        grid = new DataGridHelper<>(table);

        // Sort options for Browse Courses
        grid.addSortOption("Name",       java.util.Comparator.comparing(
            c -> c.getCourseName() != null ? c.getCourseName() : ""));
        grid.addSortOption("Code",       java.util.Comparator.comparing(
            c -> c.getCourseCode() != null ? c.getCourseCode() : ""));
        grid.addSortOption("Instructor", java.util.Comparator.comparing(
            c -> c.getInstructorName() != null ? c.getInstructorName() : ""));
        grid.addSortOption("Seats",      java.util.Comparator.comparingInt(
            com.lms.analytics.models.Course::getAvailableSeats));
        grid.addSortOption("Credits",    java.util.Comparator.comparingInt(
            com.lms.analytics.models.Course::getCredits));

        HBox searchBar = grid.buildFilterBarWithSort(
            "Search by course name, code or instructor...",
            val -> c -> (c.getCourseCode() != null && c.getCourseCode().toLowerCase().contains(val))
                     || (c.getCourseName() != null && c.getCourseName().toLowerCase().contains(val))
                     || (c.getInstructorName() != null && c.getInstructorName().toLowerCase().contains(val))
        );
        HBox paging = grid.buildPaginationBar();

        VBox tableBox = new VBox(0, searchBar, table, paging);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setCenter(tableBox);

        // ── Right detail panel ────────────────────────────────────────
        VBox panel = new VBox(12);
        panel.setPadding(new Insets(16));
        panel.setPrefWidth(280);
        panel.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 0 1;");

        Label panelTitle = new Label("Course Details");
        panelTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        detailLabel = new Label("← Select a course from the table.");
        detailLabel.setWrapText(true);
        detailLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b; " +
            "-fx-background-color:#f8fafc; -fx-background-radius:8; -fx-padding:10;");

        enrollStatusLabel = new Label();
        enrollStatusLabel.setWrapText(true);
        enrollStatusLabel.setStyle("-fx-font-size:12px;");

        enrollBtn = new Button("✅  Enroll in This Course");
        enrollBtn.setMaxWidth(Double.MAX_VALUE);
        enrollBtn.getStyleClass().addAll("button", "btn-primary");
        enrollBtn.setDisable(true);
        enrollBtn.setOnAction(e -> {
            Course c = table.getSelectionModel().getSelectedItem();
            if (c != null) handleEnroll(c);
        });

        Label hint = new Label("💡 Double-click a row to enroll instantly.");
        hint.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");

        panel.getChildren().addAll(
            panelTitle, new Separator(),
            detailLabel, enrollStatusLabel,
            new Separator(),
            enrollBtn, hint
        );
        layout.setRight(panel);
        root.getChildren().add(layout);
    }

    // ── DATA ──────────────────────────────────────────────────────────
    private void loadCourses() {
        grid.setData(courseDAO.getActiveCourses());
        statusLabel.setText("Available: " + grid.getTotalFiltered() + " courses");
    }

    private void filterCourses(String kw) {
        if (kw == null || kw.isBlank()) { grid.clearFilter(); return; }
        String val = kw.toLowerCase();
        grid.setFilter(c ->
            (c.getCourseCode() != null && c.getCourseCode().toLowerCase().contains(val)) ||
            (c.getCourseName() != null && c.getCourseName().toLowerCase().contains(val)) ||
            (c.getInstructorName() != null && c.getInstructorName().toLowerCase().contains(val)));
        statusLabel.setText("Found: " + grid.getTotalFiltered());
    }

    // ── COURSE SELECTED ───────────────────────────────────────────────
    private void onCourseSelected(Course c) {
        // Ensure student has a valid ID before checking enrollment
        ensureStudentProfile();

        boolean alreadyEnrolled = isStudentEnrolled(c.getCourseId());
        boolean seatsFull = c.getAvailableSeats() <= 0;

        detailLabel.setText(
            "📚  " + c.getCourseCode() + " — " + c.getCourseName() + "\n\n" +
            "Instructor:   " + (c.getInstructorName() != null ? c.getInstructorName() : "TBA") + "\n" +
            "Credits:      " + c.getCredits() + "\n" +
            "Enrolled:     " + c.getEnrolledCount() + " / " + c.getCapacity() + "\n" +
            "Seats Left:   " + c.getAvailableSeats() + "\n" +
            "Price:        " + (c.getPrice() != null ? "$" + c.getPrice() : "Free") + "\n\n" +
            (c.getDescription() != null && !c.getDescription().isBlank()
                ? c.getDescription() : "No description available.")
        );

        if (alreadyEnrolled) {
            enrollStatusLabel.setText("✓  You are enrolled in this course.");
            enrollStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#16a34a; -fx-font-weight:bold;");
            enrollBtn.setDisable(true);
        } else if (seatsFull) {
            enrollStatusLabel.setText("✗  No seats available — course is full.");
            enrollStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#dc2626; -fx-font-weight:bold;");
            enrollBtn.setDisable(true);
        } else {
            enrollStatusLabel.setText("○  You are not enrolled. Click Enroll to join.");
            enrollStatusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
            enrollBtn.setDisable(false);
        }
    }

    /**
     * Checks enrollment using both student_id and user_id to handle data inconsistencies.
     * Used by the detail panel (called after ensureStudentProfile).
     */
    private boolean isStudentEnrolled(int courseId) {
        if (currentStudent == null) return false;
        User user = SessionManager.getInstance().getCurrentUser();
        // Check by student_id
        if (currentStudent.getStudentId() > 0 &&
            enrollmentDAO.isEnrolled(currentStudent.getStudentId(), courseId)) return true;
        // Check by user_id (fallback for corrupted data)
        if (user != null && enrollmentDAO.isEnrolled(user.getUserId(), courseId)) return true;
        return false;
    }

    /**
     * Same dual-ID enrollment check used by the My Status table column.
     * Does not require ensureStudentProfile() to have been called first.
     */
    private boolean isStudentEnrolledForColumn(int courseId) {
        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return false;
        // Check by student_id if profile is loaded
        if (currentStudent != null && currentStudent.getStudentId() > 0 &&
            enrollmentDAO.isEnrolled(currentStudent.getStudentId(), courseId)) return true;
        // Check by user_id directly (covers cases where enrollment was stored with user_id)
        if (enrollmentDAO.isEnrolled(user.getUserId(), courseId)) return true;
        return false;
    }

    /**
     * Ensures currentStudent has a valid student_id > 0.
     * Retries DB lookup and auto-create if needed.
     */
    private void ensureStudentProfile() {
        if (currentStudent != null && currentStudent.getStudentId() > 0) return;

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) return;

        // Retry DB lookup
        currentStudent = studentDAO.getStudentByUserId(user.getUserId());
        if (currentStudent != null && currentStudent.getStudentId() > 0) return;

        // Auto-create
        currentStudent = autoCreateStudentProfile(user);
    }

    // ── ENROLL ────────────────────────────────────────────────────────
    private void handleEnroll(Course c) {
        ensureStudentProfile();

        if (currentStudent == null || currentStudent.getStudentId() <= 0) {
            statusLabel.setText("⚠ Cannot enroll: student profile missing. Please contact admin.");
            return;
        }

        int sid = currentStudent.getStudentId();

        if (enrollmentDAO.isEnrolled(sid, c.getCourseId())) {
            statusLabel.setText("You are already enrolled in " + c.getCourseName());
            return;
        }
        if (c.getAvailableSeats() <= 0) {
            statusLabel.setText("⚠ No seats available in " + c.getCourseName());
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Enroll in:\n" + c.getCourseCode() + " — " + c.getCourseName() +
            "\n\nCredits: " + c.getCredits() +
            "\nPrice: " + (c.getPrice() != null ? "$" + c.getPrice() : "Free"),
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Enrollment");
        confirm.setHeaderText("Enroll in Course?");

        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (enrollmentDAO.enrollStudent(sid, c.getCourseId())) {
                    loadCourses();
                    Course refreshed = courseDAO.getCourseById(c.getCourseId());
                    if (refreshed != null) onCourseSelected(refreshed);
                    statusLabel.setText("✓  Successfully enrolled in " + c.getCourseName());
                } else {
                    statusLabel.setText("⚠ Enrollment failed. Please try again.");
                }
            }
        });
    }

}
