package com.lms.analytics.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.lms.analytics.services.ImportExportService;
import com.lms.analytics.services.ImportExportService.ImportResult;

import java.io.File;

public class ImportExportController {

    @FXML private ComboBox<String> importTypeCombo;
    @FXML private ComboBox<String> exportTypeCombo;
    @FXML private TextField selectedFileField;
    @FXML private TextArea logArea;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;

    private ImportExportService importExportService;

    @FXML
    public void initialize() {
        importExportService = new ImportExportService();

        if (importTypeCombo != null)
            importTypeCombo.getItems().addAll("Students (CSV)", "Courses (CSV)", "Enrollments (CSV)");

        if (exportTypeCombo != null)
            exportTypeCombo.getItems().addAll(
                    "Students (Excel)", "Courses (Excel)", "Enrollments (Excel)", "Students (CSV)");
    }

    @FXML
    private void handleBrowseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select CSV File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        Stage stage = (Stage) statusLabel.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file != null && selectedFileField != null) {
            selectedFileField.setText(file.getAbsolutePath());
        }
    }

    @FXML
    private void handleImport() {
        if (importTypeCombo == null || importTypeCombo.getValue() == null) {
            setStatus("Select an import type.");
            return;
        }
        if (selectedFileField == null || selectedFileField.getText().isEmpty()) {
            setStatus("Select a file to import.");
            return;
        }

        String filePath = selectedFileField.getText();
        String type = importTypeCombo.getValue();
        ImportResult result;

        if (progressBar != null) progressBar.setProgress(-1);

        switch (type) {
            case "Students (CSV)":
                result = importExportService.importStudentsFromCSV(filePath);
                break;
            case "Courses (CSV)":
                result = importExportService.importCoursesFromCSV(filePath);
                break;
            case "Enrollments (CSV)":
                result = importExportService.importEnrollmentsFromCSV(filePath);
                break;
            default:
                setStatus("Unknown import type.");
                return;
        }

        if (progressBar != null) progressBar.setProgress(1);
        setStatus(result.toString());
        appendLog("Import completed: " + result);
        if (result.hasErrors()) {
            result.getErrors().forEach(e -> appendLog("  ERROR: " + e));
        }
    }

    @FXML
    private void handleExport() {
        if (exportTypeCombo == null || exportTypeCombo.getValue() == null) {
            setStatus("Select an export type.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Export File");
        String type = exportTypeCombo.getValue();

        if (type.contains("Excel")) {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        } else {
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        }

        Stage stage = (Stage) statusLabel.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;

        if (progressBar != null) progressBar.setProgress(-1);
        boolean success = false;

        switch (type) {
            case "Students (Excel)":
                success = importExportService.exportStudentsToExcel(file.getAbsolutePath());
                break;
            case "Courses (Excel)":
                success = importExportService.exportCoursesToExcel(file.getAbsolutePath());
                break;
            case "Enrollments (Excel)":
                success = importExportService.exportEnrollmentsToExcel(file.getAbsolutePath());
                break;
            case "Students (CSV)":
                success = importExportService.exportStudentsToCSV(file.getAbsolutePath());
                break;
        }

        if (progressBar != null) progressBar.setProgress(success ? 1 : 0);
        String msg = success ? "Exported to: " + file.getName() : "Export failed.";
        setStatus(msg);
        appendLog(msg);
    }

    @FXML
    private void handleClearLog() {
        if (logArea != null) logArea.clear();
    }

    private void setStatus(String msg) {
        if (statusLabel != null) {
            statusLabel.setText(msg);
            statusLabel.setStyle("-fx-font-weight:bold; -fx-font-size:12px; -fx-text-fill:"
                + (msg.startsWith("✓") || msg.contains("success") || msg.contains("Imported") || msg.contains("Exported")
                    ? "#16a34a;" : msg.startsWith("⚠") || msg.contains("failed") || msg.contains("Failed")
                    ? "#dc2626;" : "#38bdf8;"));
        }
    }

    private void appendLog(String msg) {
        if (logArea != null) {
            logArea.appendText("[" + java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + msg + "\n");
        }
    }

    @FXML
    private void handleBack() {
        javafx.scene.Node node = statusLabel != null ? statusLabel
            : progressBar != null ? progressBar : logArea;
        if (node != null)
            com.lms.analytics.utils.NavigationUtil.backToDashboard(node);
    }
}
