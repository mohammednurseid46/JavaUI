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

import com.lms.analytics.dao.CourseDAO;
import com.lms.analytics.dao.UserDAO;
import com.lms.analytics.models.Course;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.DataGridHelper;

import java.math.BigDecimal;
import java.util.List;

public class CourseController {

    @FXML private StackPane root;

    // Master (table)
    private TableView<Course> table;
    private DataGridHelper<Course> grid;
    private TextField searchField;
    private Label statusLabel;

    // Detail (form fields)
    private TextField courseCodeField, courseNameField, priceField;
    private TextArea  descriptionArea;
    private Spinner<Integer> creditsSpinner, capacitySpinner;
    private ComboBox<User>   instructorCombo;
    private ComboBox<String> statusCombo;
    private Label detailInfoLabel;

    private final CourseDAO courseDAO = new CourseDAO();
    private final UserDAO   userDAO   = new UserDAO();

    @FXML
    public void initialize() { buildUI(); loadCourses(); }

    // ── BUILD MASTER-DETAIL LAYOUT ────────────────────────────────────
    private void buildUI() {
        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color:#f0f2f5;");

        // ── Top bar ───────────────────────────────────────────────────
        VBox top = new VBox(8);
        top.setPadding(new Insets(16, 20, 12, 20));
        top.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        Button backBtn = new Button("← Back");
        backBtn.getStyleClass().add("back-btn");
        backBtn.setOnAction(e -> com.lms.analytics.utils.NavigationUtil.backToDashboard(table));

        Label title = new Label("📚  Course Management");
        title.setStyle("-fx-font-size:20px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        HBox titleRow = new HBox(12, backBtn, title);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label subtitle = new Label("Manage all courses. Select a course from the table to view or edit its details.");
        subtitle.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        searchField = new TextField();
        searchField.setPromptText("🔍  Search by code, name or instructor...");
        searchField.setPrefWidth(340);
        searchField.setStyle("-fx-background-radius:20; -fx-border-radius:20; -fx-padding:7 14;");
        searchField.textProperty().addListener((o, old, v) -> filterCourses(v));

        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#38bdf8;");

        Button refreshBtn = new Button("↻ Refresh");
        refreshBtn.getStyleClass().addAll("button", "btn-primary");
        refreshBtn.setStyle("-fx-padding:6 14;");
        refreshBtn.setOnAction(e -> loadCourses());

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        HBox searchRow = new HBox(10, searchField, sp, statusLabel, refreshBtn);
        searchRow.setAlignment(Pos.CENTER_LEFT);

        top.getChildren().addAll(titleRow, subtitle, searchRow);
        layout.setTop(top);

        // ── MASTER: Course table ──────────────────────────────────────
        table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        TableColumn<Course, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(new PropertyValueFactory<>("courseCode"));

        TableColumn<Course, String> nameCol = new TableColumn<>("Course Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("courseName"));

        TableColumn<Course, Integer> creditsCol = new TableColumn<>("Credits");
        creditsCol.setCellValueFactory(new PropertyValueFactory<>("credits"));

        TableColumn<Course, String> instructorCol = new TableColumn<>("Instructor");
        instructorCol.setCellValueFactory(new PropertyValueFactory<>("instructorName"));

        TableColumn<Course, Integer> capacityCol = new TableColumn<>("Capacity");
        capacityCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));

        TableColumn<Course, Integer> enrolledCol = new TableColumn<>("Enrolled");
        enrolledCol.setCellValueFactory(new PropertyValueFactory<>("enrolledCount"));

        TableColumn<Course, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setStyle(""); return; }
                setText(s);
                setStyle("ACTIVE".equals(s)    ? "-fx-text-fill:#16a34a; -fx-font-weight:bold;"
                       : "COMPLETED".equals(s) ? "-fx-text-fill:#1d4ed8; -fx-font-weight:bold;"
                       : "-fx-text-fill:#dc2626; -fx-font-weight:bold;");
            }
        });

        table.getColumns().addAll(codeCol, nameCol, creditsCol, instructorCol,
                                  capacityCol, enrolledCol, statusCol);
        // Wire master → detail
        table.getSelectionModel().selectedItemProperty().addListener(
            (obs, old, selected) -> populateDetail(selected));

        // DataGrid: sorting + filtering + pagination
        grid = new DataGridHelper<>(table);

        // Register sort options
        grid.addSortOption("Name",       java.util.Comparator.comparing(
            c -> c.getCourseName() != null ? c.getCourseName() : ""));
        grid.addSortOption("Code",       java.util.Comparator.comparing(
            c -> c.getCourseCode() != null ? c.getCourseCode() : ""));
        grid.addSortOption("Instructor", java.util.Comparator.comparing(
            c -> c.getInstructorName() != null ? c.getInstructorName() : ""));
        grid.addSortOption("Enrolled",   java.util.Comparator.comparingInt(
            com.lms.analytics.models.Course::getEnrolledCount));
        grid.addSortOption("Capacity",   java.util.Comparator.comparingInt(
            com.lms.analytics.models.Course::getCapacity));
        grid.addSortOption("Status",     java.util.Comparator.comparing(
            c -> c.getStatus() != null ? c.getStatus() : ""));

        HBox searchBar = grid.buildFilterBarWithSort(
            "Search by code, name or instructor...",
            val -> c -> (c.getCourseCode() != null && c.getCourseCode().toLowerCase().contains(val))
                     || (c.getCourseName() != null && c.getCourseName().toLowerCase().contains(val))
                     || (c.getInstructorName() != null && c.getInstructorName().toLowerCase().contains(val))
        );
        HBox paging = grid.buildPaginationBar();

        VBox centerBox = new VBox(0, searchBar, table, paging);
        VBox.setVgrow(table, Priority.ALWAYS);
        layout.setCenter(centerBox);

        // ── DETAIL: Right panel ───────────────────────────────────────
        ScrollPane rightScroll = new ScrollPane();
        rightScroll.setFitToWidth(true);
        rightScroll.setPrefWidth(320);
        rightScroll.setStyle("-fx-background-color:white; -fx-background:white; " +
            "-fx-border-color:#e2e8f0; -fx-border-width:0 0 0 1;");

        VBox detail = new VBox(10);
        detail.setPadding(new Insets(16));
        detail.setStyle("-fx-background-color:white;");

        Label detailTitle = new Label("Course Details");
        detailTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        detailInfoLabel = new Label("← Select a course from the table to view details.");
        detailInfoLabel.setWrapText(true);
        detailInfoLabel.setStyle(
            "-fx-font-size:12px; -fx-text-fill:#334155; " +
            "-fx-background-color:#f0f9ff; -fx-background-radius:8; " +
            "-fx-border-color:#bae6fd; -fx-border-radius:8; -fx-padding:10;");

        courseCodeField  = field("Course Code *");
        courseNameField  = field("Course Name *");
        descriptionArea  = new TextArea();
        descriptionArea.setPromptText("Course description...");
        descriptionArea.setPrefRowCount(3);
        descriptionArea.setMaxWidth(Double.MAX_VALUE);
        descriptionArea.getStyleClass().add("text-area");

        creditsSpinner  = new Spinner<>(1, 10, 3);
        creditsSpinner.setEditable(true);
        creditsSpinner.setMaxWidth(Double.MAX_VALUE);

        capacitySpinner = new Spinner<>(1, 500, 30);
        capacitySpinner.setEditable(true);
        capacitySpinner.setMaxWidth(Double.MAX_VALUE);

        priceField = field("Price (e.g. 0.00)");

        instructorCombo = new ComboBox<>();
        instructorCombo.setMaxWidth(Double.MAX_VALUE);
        instructorCombo.setPromptText("Assign instructor...");
        List<User> instructors = userDAO.getUsersByRole("INSTRUCTOR");
        instructorCombo.setItems(FXCollections.observableArrayList(instructors));
        instructorCombo.setConverter(new javafx.util.StringConverter<>() {
            public String toString(User u) { return u == null ? "" : u.getFullName(); }
            public User fromString(String s) { return null; }
        });

        statusCombo = new ComboBox<>();
        statusCombo.setMaxWidth(Double.MAX_VALUE);
        statusCombo.setItems(FXCollections.observableArrayList("ACTIVE", "COMPLETED", "CANCELLED"));
        statusCombo.setValue("ACTIVE");

        // Action buttons
        Button addBtn = new Button("➕  Add New Course");
        addBtn.setMaxWidth(Double.MAX_VALUE);
        addBtn.getStyleClass().addAll("button", "btn-primary");
        addBtn.setOnAction(e -> handleAdd());

        Button updateBtn = new Button("💾  Save Changes");
        updateBtn.setMaxWidth(Double.MAX_VALUE);
        updateBtn.getStyleClass().addAll("button", "btn-primary");
        updateBtn.setOnAction(e -> handleUpdate());

        Button deleteBtn = new Button("🗑  Delete Course");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        deleteBtn.getStyleClass().addAll("button", "btn-danger");
        deleteBtn.setOnAction(e -> handleDelete());

        Button clearBtn = new Button("✕  Clear Form");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.getStyleClass().addAll("button", "btn-secondary");
        clearBtn.setOnAction(e -> clearForm());

        detail.getChildren().addAll(
            detailTitle, new Separator(), detailInfoLabel,
            lbl("Course Code"), courseCodeField,
            lbl("Course Name"), courseNameField,
            lbl("Description"), descriptionArea,
            lbl("Credits"), creditsSpinner,
            lbl("Capacity"), capacitySpinner,
            lbl("Price"), priceField,
            lbl("Instructor"), instructorCombo,
            lbl("Status"), statusCombo,
            new Separator(),
            addBtn, updateBtn, deleteBtn, clearBtn
        );

        rightScroll.setContent(detail);
        layout.setRight(rightScroll);
        root.getChildren().add(layout);
    }

    // ── MASTER-DETAIL: populate detail panel from selected course ─────
    private void populateDetail(Course c) {
        if (c == null) {
            detailInfoLabel.setText("← Select a course from the table to view details.");
            clearForm();
            return;
        }
        detailInfoLabel.setText(
            "Code:     " + c.getCourseCode() + "\n" +
            "Name:     " + c.getCourseName() + "\n" +
            "Enrolled: " + c.getEnrolledCount() + " / " + c.getCapacity() + "\n" +
            "Status:   " + c.getStatus()
        );
        courseCodeField.setText(c.getCourseCode() != null ? c.getCourseCode() : "");
        courseNameField.setText(c.getCourseName() != null ? c.getCourseName() : "");
        descriptionArea.setText(c.getDescription() != null ? c.getDescription() : "");
        creditsSpinner.getValueFactory().setValue(c.getCredits() > 0 ? c.getCredits() : 3);
        capacitySpinner.getValueFactory().setValue(c.getCapacity() > 0 ? c.getCapacity() : 30);
        if (c.getPrice() != null) priceField.setText(c.getPrice().toPlainString());
        else priceField.clear();
        statusCombo.setValue(c.getStatus() != null ? c.getStatus() : "ACTIVE");

        // Match instructor by ID
        instructorCombo.getItems().stream()
            .filter(u -> u.getUserId() == c.getInstructorId())
            .findFirst()
            .ifPresent(instructorCombo::setValue);
    }

    // ── DATA ──────────────────────────────────────────────────────────
    private void loadCourses() {
        grid.setData(courseDAO.getAllCourses());
        statusLabel.setText("Total: " + grid.getTotalFiltered() + " courses");
    }

    private void filterCourses(String kw) {
        if (kw == null || kw.isBlank()) { grid.clearFilter(); return; }
        String val = kw.toLowerCase();
        grid.setFilter(c ->
            (c.getCourseCode() != null && c.getCourseCode().toLowerCase().contains(val)) ||
            (c.getCourseName() != null && c.getCourseName().toLowerCase().contains(val)));
        statusLabel.setText("Found: " + grid.getTotalFiltered());
    }

    // ── ACTIONS ───────────────────────────────────────────────────────
    private void handleAdd() {
        if (!validateForm()) return;
        Course c = buildFromForm();
        if (courseDAO.createCourse(c)) {
            loadCourses();
            clearForm();
            setStatus("✓ Course added: " + c.getCourseName());
        } else {
            setStatus("⚠ Failed to add course.");
        }
    }

    private void handleUpdate() {
        Course selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("⚠ Select a course to update."); return; }
        if (!validateForm()) return;
        Course c = buildFromForm();
        c.setCourseId(selected.getCourseId());
        if (courseDAO.updateCourse(c)) {
            loadCourses();
            setStatus("✓ Updated: " + c.getCourseName());
        } else {
            setStatus("⚠ Update failed.");
        }
    }

    private void handleDelete() {
        Course selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) { setStatus("⚠ Select a course to delete."); return; }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Delete course: " + selected.getCourseName() + "?\nThis cannot be undone.",
            ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                if (courseDAO.deleteCourse(selected.getCourseId())) {
                    loadCourses();
                    clearForm();
                    setStatus("✓ Deleted: " + selected.getCourseName());
                } else {
                    setStatus("⚠ Delete failed.");
                }
            }
        });
    }

    private void clearForm() {
        courseCodeField.clear();
        courseNameField.clear();
        descriptionArea.clear();
        priceField.clear();
        creditsSpinner.getValueFactory().setValue(3);
        capacitySpinner.getValueFactory().setValue(30);
        statusCombo.setValue("ACTIVE");
        instructorCombo.setValue(null);
        if (detailInfoLabel != null)
            detailInfoLabel.setText("← Select a course from the table to view details.");
        table.getSelectionModel().clearSelection();
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private boolean validateForm() {
        if (courseCodeField.getText().trim().isEmpty()) {
            setStatus("⚠ Course code is required."); return false;
        }
        if (courseNameField.getText().trim().isEmpty()) {
            setStatus("⚠ Course name is required."); return false;
        }
        return true;
    }

    private Course buildFromForm() {
        Course c = new Course();
        c.setCourseCode(courseCodeField.getText().trim());
        c.setCourseName(courseNameField.getText().trim());
        c.setDescription(descriptionArea.getText().trim());
        c.setCredits(creditsSpinner.getValue());
        c.setCapacity(capacitySpinner.getValue());
        if (!priceField.getText().isEmpty()) {
            try { c.setPrice(new BigDecimal(priceField.getText().trim())); }
            catch (NumberFormatException ignored) {}
        }
        c.setStatus(statusCombo.getValue());
        if (instructorCombo.getValue() != null)
            c.setInstructorId(instructorCombo.getValue().getUserId());
        return c;
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setMaxWidth(Double.MAX_VALUE);
        tf.getStyleClass().add("text-field");
        return tf;
    }

    private Label lbl(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("form-label");
        return l;
    }

    private void setStatus(String msg) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        if (msg.startsWith("⚠"))
            statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#dc2626;");
        else if (msg.startsWith("✓"))
            statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#16a34a;");
        else
            statusLabel.setStyle("-fx-font-size:12px; -fx-text-fill:#38bdf8;");
    }
}
