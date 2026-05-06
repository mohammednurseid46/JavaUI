package com.lms.analytics.controllers;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.lms.analytics.dao.CourseDAO;
import com.lms.analytics.dao.InstructorDAO;
import com.lms.analytics.dao.StudentDAO;
import com.lms.analytics.models.Instructor;
import com.lms.analytics.services.ReportService;
import com.lms.analytics.utils.PDFExporter;
import com.lms.analytics.utils.ExcelExporter;
import com.lms.analytics.utils.WordExporter;

import java.io.File;
import java.util.List;

public class ReportsController {

    @FXML private StackPane reportCardContainer;

    // Resolved IDs
    private int resolvedStudentId    = 0;
    private int resolvedInstructorId = 0;
    private int resolvedCourseId     = 0;

    // Flag to prevent text-change listener from overwriting a just-selected item
    private boolean selectingFromDropdown = false;

    // UI controls
    private ComboBox<String> reportTypeCombo;
    private ComboBox<String> studentCombo;
    private ComboBox<String> instructorCombo;
    private ComboBox<String> courseCombo;
    private Label            statusLabel;
    private ProgressBar      progressBar;

    // DAOs & Services
    private final StudentDAO    studentDAO    = new StudentDAO();
    private final CourseDAO     courseDAO     = new CourseDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final com.lms.analytics.dao.UserDAO userDAO = new com.lms.analytics.dao.UserDAO();
    private ReportService  reportService;
    private PDFExporter    pdfExporter;
    private ExcelExporter  excelExporter;
    private WordExporter   wordExporter;

    @FXML
    public void initialize() {
        reportService = new ReportService();
        pdfExporter   = new PDFExporter();
        excelExporter = new ExcelExporter();
        wordExporter  = new WordExporter();
        buildReportCard();
    }

    private void buildReportCard() {
        VBox card = new VBox(14);
        card.setPadding(new Insets(20));
        card.setMaxWidth(Double.MAX_VALUE);

        Label cardTitle = new Label("Generate Report");
        cardTitle.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        // Report Type combo
        reportTypeCombo = new ComboBox<>();
        reportTypeCombo.setPrefWidth(400);
        reportTypeCombo.setMaxWidth(400);
        reportTypeCombo.setPromptText("Select a report type...");
        reportTypeCombo.getItems().addAll(
            "Accreditation Report (PDF)",
            "Financial Summary (PDF)",
            "Enrollment Report (PDF)",
            "Academic Progress (PDF)",
            "Instructor Evaluation (PDF)",
            "Grade Book (Excel)",
            "Student Report (Excel)",
            "Course Report (Excel)",
            "Student Certificate (Word)"
        );

        // Student combo
        studentCombo = buildSearchCombo("Search student name or number...");
        populateOptions(studentCombo, "student", "");
        wireCombo(studentCombo, "student");

        // Instructor combo
        instructorCombo = buildSearchCombo("Search instructor name...");
        populateOptions(instructorCombo, "instructor", "");
        wireCombo(instructorCombo, "instructor");

        // If the logged-in user is an INSTRUCTOR, pre-fill their own ID
        com.lms.analytics.models.User currentUser =
            com.lms.analytics.utils.SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && "INSTRUCTOR".equals(currentUser.getRole())) {
            // Find this instructor's entry in the combo list and pre-select it
            String match = instructorCombo.getItems().stream()
                .filter(item -> item.contains("[ID:" + currentUser.getUserId() + "]"))
                .findFirst().orElse(null);
            if (match != null) {
                instructorCombo.getEditor().setText(match);
                extractId(match, "instructor");
            } else {
                // Fallback: set display text and resolve ID directly from user_id
                String display = currentUser.getFullName() + " [ID:" + currentUser.getUserId() + "]";
                instructorCombo.getEditor().setText(display);
                resolvedInstructorId = currentUser.getUserId();
                setStatus("Instructor ID: " + currentUser.getUserId() + " selected");
            }
        }

        // Course combo
        courseCombo = buildSearchCombo("Search course name or code...");
        populateOptions(courseCombo, "course", "");
        wireCombo(courseCombo, "course");

        // Buttons
        Button generateBtn = new Button("Generate Report");
        generateBtn.getStyleClass().addAll("button", "btn-primary");
        generateBtn.setOnAction(e -> handleGenerateReport());

        Button exportBtn = new Button("Export All Enrollments (Excel)");
        exportBtn.getStyleClass().addAll("button", "btn-primary");
        exportBtn.setOnAction(e -> handleExportAllToExcel());

        HBox btnRow = new HBox(10, generateBtn, exportBtn);
        btnRow.setAlignment(Pos.CENTER_LEFT);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);

        statusLabel = new Label();
        statusLabel.setStyle("-fx-text-fill:#38bdf8; -fx-font-weight:bold; -fx-font-size:12px;");

        // Two-column grid
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(12);
        grid.setMaxWidth(Double.MAX_VALUE);

        // Label column — fixed 140px
        ColumnConstraints labelCol = new ColumnConstraints(140, 140, 140);

        // Control column — fixed preferred width of 400px, does NOT grow beyond that
        ColumnConstraints controlCol = new ColumnConstraints(300, 400, 400);
        controlCol.setHgrow(Priority.NEVER);
        controlCol.setFillWidth(false);
        grid.getColumnConstraints().addAll(labelCol, controlCol);

        // Remove setMaxWidth(MAX) from combos — let the column constraint control width
        reportTypeCombo.setMaxWidth(400);
        reportTypeCombo.setPrefWidth(400);
        studentCombo.setMaxWidth(400);
        studentCombo.setPrefWidth(400);
        instructorCombo.setMaxWidth(400);
        instructorCombo.setPrefWidth(400);
        courseCombo.setMaxWidth(400);
        courseCombo.setPrefWidth(400);

        grid.add(fieldLabel("Report Type:"),   0, 0);
        grid.add(reportTypeCombo,              1, 0);

        grid.add(fieldLabel("Student ID:"),    0, 1);
        grid.add(studentCombo,                 1, 1);

        grid.add(fieldLabel("Instructor ID:"), 0, 2);
        grid.add(instructorCombo,              1, 2);

        grid.add(fieldLabel("Course ID:"),     0, 3);
        grid.add(courseCombo,                  1, 3);

        card.getChildren().addAll(cardTitle, new Separator(), grid, btnRow, progressBar, statusLabel);
        reportCardContainer.getChildren().add(card);
    }

    // Build an editable searchable combo box
    private ComboBox<String> buildSearchCombo(String prompt) {
        ComboBox<String> combo = new ComboBox<>();
        combo.setEditable(true);
        combo.setPrefWidth(400);
        combo.setMaxWidth(400);
        combo.setPromptText(prompt);
        combo.setVisibleRowCount(8);
        return combo;
    }

    // Wire a combo box: live search on typing, ID extraction on selection
    private void wireCombo(ComboBox<String> combo, String type) {

        // When user picks an item from the dropdown list
        combo.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> {
            if (selected != null && selected.contains("[ID:")) {
                selectingFromDropdown = true;
                // Show the full selected text in the editor
                combo.getEditor().setText(selected);
                combo.getEditor().positionCaret(selected.length());
                extractId(selected, type);
                selectingFromDropdown = false;
            }
        });

        // Live search as user types — but skip if we just set the text from a selection
        combo.getEditor().textProperty().addListener((obs, old, val) -> {
            if (selectingFromDropdown) return;
            if (val == null) return;
            // If the current text already has [ID:] it means a selection was made — don't reload
            if (val.contains("[ID:")) {
                extractId(val, type);
                return;
            }
            // Otherwise filter the list
            populateOptions(combo, type, val.toLowerCase());
            if (!val.isEmpty() && !combo.isShowing()) {
                combo.show();
            }
        });

        // Also extract ID when focus leaves the field
        combo.getEditor().focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                String text = combo.getEditor().getText();
                if (text != null && text.contains("[ID:")) {
                    extractId(text, type);
                }
            }
        });
    }

    // Populate combo items based on type and filter string
    private void populateOptions(ComboBox<String> combo, String type, String filter) {
        List<String> items = switch (type) {
            case "student" -> studentDAO.getAllStudents().stream()
                .filter(s -> filter.isEmpty()
                    || (s.getFullName() != null && s.getFullName().toLowerCase().contains(filter))
                    || (s.getStudentNumber() != null && s.getStudentNumber().toLowerCase().contains(filter)))
                .map(s -> s.getStudentNumber() + " - " + s.getFullName() + " [ID:" + s.getStudentId() + "]")
                .toList();
            case "instructor" -> {
                List<Instructor> list = filter.isEmpty()
                    ? instructorDAO.getAllInstructors()
                    : instructorDAO.searchInstructors(filter);

                // If instructors table is empty, fall back to users table (role=INSTRUCTOR)
                // This covers instructors registered via admin panel who have no instructors row
                if (list.isEmpty()) {
                    yield userDAO.getUsersByRole("INSTRUCTOR").stream()
                        .filter(u -> filter.isEmpty()
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(filter))
                            || (u.getUsername() != null && u.getUsername().toLowerCase().contains(filter)))
                        .map(u -> u.getFullName() + " (@" + u.getUsername() + ") [ID:" + u.getUserId() + "]")
                        .toList();
                }
                yield list.stream()
                    .map(i -> i.getFullName() + " (" + i.getEmployeeNumber() + ") [ID:" + i.getInstructorId() + "]")
                    .toList();
            }
            case "course" -> courseDAO.getAllCourses().stream()
                .filter(c -> filter.isEmpty()
                    || (c.getCourseName() != null && c.getCourseName().toLowerCase().contains(filter))
                    || (c.getCourseCode() != null && c.getCourseCode().toLowerCase().contains(filter)))
                .map(c -> c.getCourseCode() + " - " + c.getCourseName() + " [ID:" + c.getCourseId() + "]")
                .toList();
            default -> List.of();
        };
        // Save current editor text so we can restore it after setItems()
        String currentText = combo.getEditor().getText();
        combo.setItems(FXCollections.observableArrayList(items));
        // Only restore if the text doesn't already contain a selection
        if (!currentText.contains("[ID:")) {
            combo.getEditor().setText(currentText);
        }
    }

    // Extract [ID:X] from a combo item string and store it
    private void extractId(String text, String type) {
        if (text == null || !text.contains("[ID:")) return;
        try {
            int start = text.lastIndexOf("ID:") + 3;
            int end   = text.indexOf("]", start);
            if (start > 2 && end > start) {
                int id = Integer.parseInt(text.substring(start, end).trim());
                switch (type) {
                    case "student"    -> { resolvedStudentId    = id; setStatus("Student ID: "    + id + " selected"); }
                    case "instructor" -> { resolvedInstructorId = id; setStatus("Instructor ID: " + id + " selected"); }
                    case "course"     -> { resolvedCourseId     = id; setStatus("Course ID: "     + id + " selected"); }
                }
            }
        } catch (NumberFormatException ignored) {}
    }

    // Re-extract all IDs before generating a report
    private void refreshResolvedIds() {
        if (studentCombo    != null) extractId(getText(studentCombo),    "student");
        if (instructorCombo != null) extractId(getText(instructorCombo), "instructor");
        if (courseCombo     != null) extractId(getText(courseCombo),     "course");
    }

    private String getText(ComboBox<String> combo) {
        String val = combo.getValue();
        return (val != null && val.contains("[ID:")) ? val : combo.getEditor().getText();
    }

    @FXML
    private void handleGenerateReport() {
        if (reportTypeCombo == null || reportTypeCombo.getValue() == null) {
            setStatus("Please select a report type."); return;
        }
        String reportType = reportTypeCombo.getValue();
        refreshResolvedIds();

        if (reportType.equals("Academic Progress (PDF)") && resolvedStudentId == 0) {
            setStatus("Please select a student."); return;
        }
        if (reportType.equals("Instructor Evaluation (PDF)") && resolvedInstructorId == 0) {
            setStatus("Please select an instructor."); return;
        }
        if (reportType.equals("Grade Book (Excel)") && resolvedCourseId == 0) {
            setStatus("Please select a course."); return;
        }
        if (reportType.equals("Student Certificate (Word)")) {
            if (resolvedStudentId == 0) { setStatus("Please select a student."); return; }
            if (resolvedCourseId  == 0) { setStatus("Please select a course.");  return; }
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report");
        if (reportType.contains("PDF"))
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        else if (reportType.contains("Excel"))
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        else if (reportType.contains("Word"))
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Word Files", "*.docx"));

        Stage stage = (Stage) reportCardContainer.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        String path = file.getAbsolutePath();
        if (progressBar != null) progressBar.setProgress(-1);

        try {
            boolean success = switch (reportType) {
                case "Accreditation Report (PDF)"  -> pdfExporter.exportAccreditationReport(path);
                case "Financial Summary (PDF)"     -> pdfExporter.exportFinancialReport(path);
                case "Enrollment Report (PDF)"     -> pdfExporter.exportEnrollmentReport(path);
                case "Academic Progress (PDF)"     -> reportService.generateAcademicProgressReport(path, resolvedStudentId);
                case "Instructor Evaluation (PDF)" -> reportService.generateInstructorEvaluationReport(path, resolvedInstructorId);
                case "Grade Book (Excel)"          -> excelExporter.exportGradeBook(path, resolvedCourseId);
                case "Student Report (Excel)"      -> excelExporter.exportStudents(path);
                case "Course Report (Excel)"       -> excelExporter.exportCourses(path);
                case "Student Certificate (Word)"  -> wordExporter.generateCertificate(path, resolvedStudentId, resolvedCourseId, resolvedInstructorId);
                default -> false;
            };
            if (progressBar != null) progressBar.setProgress(success ? 1 : 0);
            setStatus(success ? "Report saved: " + file.getName() : "Failed to generate report.");
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error: " + e.getMessage());
            if (progressBar != null) progressBar.setProgress(0);
        }
    }

    @FXML
    private void handleExportAllToExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Excel Report");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        Stage stage = (Stage) reportCardContainer.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        boolean success = excelExporter.exportEnrollments(file.getAbsolutePath());
        setStatus(success ? "Exported: " + file.getName() : "Export failed.");
    }

    @FXML private void handleQuickAccreditation() { quickExport("Accreditation Report (PDF)", "*.pdf"); }
    @FXML private void handleQuickFinancial()     { quickExport("Financial Summary (PDF)",    "*.pdf"); }
    @FXML private void handleQuickEnrollment()    { quickExport("Enrollment Report (PDF)",    "*.pdf"); }

    private void quickExport(String reportType, String ext) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save " + reportType);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            ext.equals("*.pdf") ? "PDF Files" : "Excel Files", ext));
        Stage stage = (Stage) reportCardContainer.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;
        String path = file.getAbsolutePath();
        if (progressBar != null) progressBar.setProgress(-1);
        try {
            boolean success = switch (reportType) {
                case "Accreditation Report (PDF)" -> pdfExporter.exportAccreditationReport(path);
                case "Financial Summary (PDF)"    -> pdfExporter.exportFinancialReport(path);
                case "Enrollment Report (PDF)"    -> pdfExporter.exportEnrollmentReport(path);
                default -> false;
            };
            if (progressBar != null) progressBar.setProgress(success ? 1 : 0);
            setStatus(success ? "Saved: " + file.getName() : "Export failed.");
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        com.lms.analytics.utils.NavigationUtil.backToDashboard(reportCardContainer);
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:#334155;");
        return l;
    }

    private void setStatus(String msg) {
        if (statusLabel == null) return;
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-weight:bold; -fx-font-size:12px; -fx-text-fill:"
            + (msg.startsWith("Report saved") || msg.startsWith("Exported") || msg.startsWith("Saved") || msg.contains("selected")
               ? "#16a34a;" : msg.startsWith("Please") || msg.startsWith("Failed") || msg.startsWith("Error")
               ? "#dc2626;" : "#38bdf8;"));
    }
}
