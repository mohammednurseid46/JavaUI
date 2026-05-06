package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import com.lms.analytics.dao.CourseDAO;
import com.lms.analytics.dao.EnrollmentDAO;
import com.lms.analytics.models.Course;
import com.lms.analytics.models.Enrollment;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.SessionManager;

import java.util.List;

public class InstructorAnalyticsController {

    @FXML private StackPane root;

    private final CourseDAO     courseDAO     = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    @FXML
    public void initialize() { buildUI(); }

    private void buildUI() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#f0f2f5; -fx-background:#f0f2f5;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox page = new VBox(20);
        page.setPadding(new Insets(20));
        page.setStyle("-fx-background-color:#f0f2f5;");

        User user = SessionManager.getInstance().getCurrentUser();
        if (user == null) { root.getChildren().add(new Label("Not logged in.")); return; }

        List<Course> myCourses = courseDAO.getCoursesByInstructor(user.getUserId());

        // Collect all enrollments across my courses
        List<Enrollment> allEnrollments = myCourses.stream()
            .flatMap(c -> enrollmentDAO.getEnrollmentsByCourse(c.getCourseId()).stream())
            .toList();

        long totalStudents = allEnrollments.stream()
            .map(Enrollment::getStudentId).distinct().count();
        long graded    = allEnrollments.stream().filter(e -> e.getGrade() != null).count();
        long completed = allEnrollments.stream().filter(e -> "COMPLETED".equals(e.getStatus())).count();
        long dropped   = allEnrollments.stream().filter(e -> "DROPPED".equals(e.getStatus())).count();
        long active    = allEnrollments.stream().filter(e -> "ENROLLED".equals(e.getStatus())).count();
        double avgGrade = allEnrollments.stream()
            .filter(e -> e.getGrade() != null)
            .mapToDouble(Enrollment::getGrade).average().orElse(0);

        // ── Header ────────────────────────────────────────────────────
        Label title = new Label("📊  My Course Analytics");
        title.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        Label sub = new Label("Analytics for your " + myCourses.size() + " assigned course(s) only.");
        sub.setStyle("-fx-font-size:13px; -fx-text-fill:#64748b;");

        // ── Stat cards — consistent light blue color scheme ──────────
        HBox cards = new HBox(14);
        String c1 = "#38bdf8", c2 = "#0ea5e9", c3 = "#0284c7", c4 = "#0369a1", c5 = "#075985";
        cards.getChildren().addAll(
            statCard("📚", "My Courses",     String.valueOf(myCourses.size()),
                avgGrade > 0 ? "Active" : "Assigned", c1),
            statCard("👥", "Total Students", String.valueOf(totalStudents),
                totalStudents + " unique", c2),
            statCard("📝", "Enrollments",    String.valueOf(allEnrollments.size()),
                active + " active", c3),
            statCard("✅", "Completed",      String.valueOf(completed),
                allEnrollments.isEmpty() ? "0%" :
                    String.format("%.0f%%", completed * 100.0 / allEnrollments.size()), c4),
            statCard("📈", "Avg Grade",
                avgGrade > 0 ? String.format("%.1f%%", avgGrade) : "N/A",
                graded + " graded", c5)
        );
        for (var n : cards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // ── Enrollment status breakdown (Pie chart) ───────────────────
        VBox statusCard = card("📊  Enrollment Status Breakdown");
        PieChart pieChart = new PieChart();
        pieChart.setAnimated(false);
        pieChart.setPrefHeight(220);
        if (active > 0)    pieChart.getData().add(new PieChart.Data("Active (" + active + ")", active));
        if (completed > 0) pieChart.getData().add(new PieChart.Data("Completed (" + completed + ")", completed));
        if (dropped > 0)   pieChart.getData().add(new PieChart.Data("Dropped (" + dropped + ")", dropped));
        if (pieChart.getData().isEmpty())
            pieChart.getData().add(new PieChart.Data("No enrollments", 1));
        statusCard.getChildren().add(pieChart);

        // ── Grade distribution (Bar chart) ───────────────────────────
        VBox gradeCard = card("🎓  Grade Distribution Across My Courses");
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of Students");
        BarChart<String, Number> gradeBar = new BarChart<>(xAxis, yAxis);
        gradeBar.setAnimated(false);
        gradeBar.setLegendVisible(false);
        gradeBar.setPrefHeight(220);

        long countA = allEnrollments.stream().filter(e -> e.getGrade() != null && e.getGrade() >= 90).count();
        long countB = allEnrollments.stream().filter(e -> e.getGrade() != null && e.getGrade() >= 80 && e.getGrade() < 90).count();
        long countC = allEnrollments.stream().filter(e -> e.getGrade() != null && e.getGrade() >= 70 && e.getGrade() < 80).count();
        long countD = allEnrollments.stream().filter(e -> e.getGrade() != null && e.getGrade() >= 60 && e.getGrade() < 70).count();
        long countF = allEnrollments.stream().filter(e -> e.getGrade() != null && e.getGrade() < 60).count();
        long notGraded = allEnrollments.stream().filter(e -> e.getGrade() == null).count();

        XYChart.Series<String, Number> gradeSeries = new XYChart.Series<>();
        gradeSeries.getData().addAll(
            new XYChart.Data<>("A (90-100)", countA),
            new XYChart.Data<>("B (80-89)",  countB),
            new XYChart.Data<>("C (70-79)",  countC),
            new XYChart.Data<>("D (60-69)",  countD),
            new XYChart.Data<>("F (<60)",    countF),
            new XYChart.Data<>("Not Graded", notGraded)
        );
        gradeBar.getData().add(gradeSeries);
        gradeCard.getChildren().add(gradeBar);

        // ── Per-course breakdown table ────────────────────────────────
        VBox courseCard = card("📚  Per-Course Breakdown");
        TableView<Course> courseTable = new TableView<>();
        courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        courseTable.setPrefHeight(200);
        courseTable.setPlaceholder(new Label("No courses assigned."));

        TableColumn<Course, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseCode()));

        TableColumn<Course, String> nameCol = new TableColumn<>("Course Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseName()));

        TableColumn<Course, String> enrolledCol = new TableColumn<>("Enrolled");
        enrolledCol.setCellValueFactory(cd -> {
            long count = enrollmentDAO.getEnrollmentsByCourse(cd.getValue().getCourseId())
                .stream().filter(e -> "ENROLLED".equals(e.getStatus())).count();
            return new SimpleStringProperty(String.valueOf(count));
        });

        TableColumn<Course, String> completedCol = new TableColumn<>("Completed");
        completedCol.setCellValueFactory(cd -> {
            long count = enrollmentDAO.getEnrollmentsByCourse(cd.getValue().getCourseId())
                .stream().filter(e -> "COMPLETED".equals(e.getStatus())).count();
            return new SimpleStringProperty(String.valueOf(count));
        });

        TableColumn<Course, String> avgCol = new TableColumn<>("Avg Grade");
        avgCol.setCellValueFactory(cd -> {
            double avg = enrollmentDAO.getEnrollmentsByCourse(cd.getValue().getCourseId())
                .stream().filter(e -> e.getGrade() != null)
                .mapToDouble(Enrollment::getGrade).average().orElse(0);
            return new SimpleStringProperty(avg > 0 ? String.format("%.1f%%", avg) : "N/A");
        });

        TableColumn<Course, String> fillCol = new TableColumn<>("Fill Rate");
        fillCol.setCellValueFactory(cd -> {
            Course c = cd.getValue();
            if (c.getCapacity() <= 0) return new SimpleStringProperty("—");
            double rate = (double) c.getEnrolledCount() / c.getCapacity() * 100;
            return new SimpleStringProperty(String.format("%.0f%%", rate));
        });

        courseTable.getColumns().addAll(codeCol, nameCol, enrolledCol, completedCol, avgCol, fillCol);
        courseTable.setItems(FXCollections.observableArrayList(myCourses));
        courseCard.getChildren().add(courseTable);

        // ── Grading progress ─────────────────────────────────────────
        VBox progressCard = card("✏️  Grading Progress");
        double gradingPct = allEnrollments.isEmpty() ? 0
            : (double) graded / allEnrollments.size() * 100;

        Label progressLbl = new Label(String.format(
            "Graded: %d / %d students  (%.1f%%)",
            graded, allEnrollments.size(), gradingPct));
        progressLbl.setStyle("-fx-font-size:13px; -fx-text-fill:#334155;");

        ProgressBar progressBar = new ProgressBar(gradingPct / 100);
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.setPrefHeight(16);
        progressBar.setStyle(gradingPct >= 80
            ? "-fx-accent:#16a34a;"
            : gradingPct >= 50 ? "-fx-accent:#eab308;" : "-fx-accent:#ef4444;");

        Label gradingNote = new Label(gradingPct >= 100
            ? "✅ All students graded!"
            : gradingPct >= 80 ? "Almost done — a few students left to grade."
            : "⚠ Many students still need grades.");
        gradingNote.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        progressCard.getChildren().addAll(progressLbl, progressBar, gradingNote);

        page.getChildren().addAll(
            title, sub, cards,
            statusCard, gradeCard,
            courseCard, progressCard
        );

        scroll.setContent(page);
        root.getChildren().add(scroll);
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private VBox statCard(String icon, String title, String value, String subtitle, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(14));
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:10; " +
            "-fx-border-color:" + color + "; -fx-border-width:0 0 0 4; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:11px; -fx-text-fill:#64748b;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:18px;");
        top.getChildren().addAll(titleLbl, sp, iconLbl);

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size:26px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label subLbl = new Label("▤ " + subtitle);
        subLbl.setStyle("-fx-font-size:11px; -fx-text-fill:" + color + ";");

        card.getChildren().addAll(top, valLbl, subLbl);
        return card;
    }

    private VBox card(String title) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:10; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        card.getChildren().addAll(lbl, new Separator());
        return card;
    }
}
