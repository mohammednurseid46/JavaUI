package com.lms.analytics.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;

import com.lms.analytics.dao.CourseDAO;
import com.lms.analytics.dao.EnrollmentDAO;
import com.lms.analytics.models.Course;
import com.lms.analytics.models.Enrollment;
import com.lms.analytics.models.User;
import com.lms.analytics.utils.SessionManager;

import java.util.List;

public class InstructorDashboardController {

    @FXML private StackPane root;

    private final CourseDAO courseDAO = new CourseDAO();
    private final EnrollmentDAO enrollmentDAO = new EnrollmentDAO();

    @FXML
    public void initialize() { buildUI(); }

    private void buildUI() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#f0f2f5; -fx-background:#f0f2f5;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox page = new VBox(20);
        page.setPadding(new Insets(24));
        page.setStyle("-fx-background-color:#f0f2f5;");
        scroll.setContent(page);

        User user = SessionManager.getInstance().getCurrentUser();
        String name = user != null ? user.getFullName() : "Instructor";

        // Header
        Label welcome = new Label("👋  Welcome, " + name);
        welcome.setStyle("-fx-font-size:22px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        Label sub = new Label("Here's an overview of your courses and students.");
        sub.setStyle("-fx-font-size:14px; -fx-text-fill:#64748b;");

        // ── Live stat values ──────────────────────────────────────────
        List<Course> myCourses = user != null
            ? courseDAO.getCoursesByInstructor(user.getUserId())
            : List.of();

        List<Enrollment> allEnrollments = myCourses.stream()
            .flatMap(c -> enrollmentDAO.getEnrollmentsByCourse(c.getCourseId()).stream())
            .toList();

        long totalStudents = allEnrollments.stream()
            .map(Enrollment::getStudentId).distinct().count();
        long completed = allEnrollments.stream()
            .filter(e -> "COMPLETED".equals(e.getStatus())).count();
        long active = allEnrollments.stream()
            .filter(e -> "ENROLLED".equals(e.getStatus())).count();
        long activeCourses = myCourses.stream()
            .filter(c -> "ACTIVE".equals(c.getStatus())).count();
        double avgGrade = allEnrollments.stream()
            .filter(e -> e.getGrade() != null)
            .mapToDouble(Enrollment::getGrade).average().orElse(0);

        // ── Stat cards — consistent light blue ────────────────────────
        String c1 = "#38bdf8", c2 = "#0ea5e9", c3 = "#0284c7", c4 = "#0369a1";

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("📚", "My Courses",     String.valueOf(myCourses.size()),
                activeCourses + " active", c1),
            statCard("👥", "Total Students", String.valueOf(totalStudents),
                active + " enrolled", c2),
            statCard("✅", "Completed",      String.valueOf(completed),
                allEnrollments.isEmpty() ? "0%" :
                    String.format("%.0f%%", completed * 100.0 / allEnrollments.size()), c3),
            statCard("📊", "Avg Grade",
                avgGrade > 0 ? String.format("%.1f%%", avgGrade) : "N/A",
                "across all courses", c4)
        );
        for (var n : cards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // My courses table
        VBox coursesCard = card("📚  My Courses");
        TableView<Course> courseTable = new TableView<>();
        courseTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        courseTable.setPrefHeight(200);

        TableColumn<Course, String> codeCol = new TableColumn<>("Code");
        codeCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseCode()));
        TableColumn<Course, String> nameCol = new TableColumn<>("Course Name");
        nameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseName()));
        TableColumn<Course, String> enrolledCol = new TableColumn<>("Enrolled");
        enrolledCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getEnrolledCount() + "/" + cd.getValue().getCapacity()));
        TableColumn<Course, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));

        courseTable.getColumns().addAll(codeCol, nameCol, enrolledCol, statusCol);
        courseTable.setItems(FXCollections.observableArrayList(myCourses));
        coursesCard.getChildren().add(courseTable);

        // Recent enrollments
        VBox recentCard = card("📝  Recent Student Enrollments");
        TableView<Enrollment> recentTable = new TableView<>();
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        recentTable.setPrefHeight(200);

        TableColumn<Enrollment, String> sNameCol = new TableColumn<>("Student");
        sNameCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStudentName()));
        TableColumn<Enrollment, String> sCourseCol = new TableColumn<>("Course");
        sCourseCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getCourseName()));
        TableColumn<Enrollment, String> sDateCol = new TableColumn<>("Date");
        sDateCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getEnrollmentDate() != null ? cd.getValue().getEnrollmentDate().toString() : ""));
        TableColumn<Enrollment, String> sStatusCol = new TableColumn<>("Status");
        sStatusCol.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().getStatus()));
        TableColumn<Enrollment, String> sGradeCol = new TableColumn<>("Grade");
        sGradeCol.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().getGrade() != null ? String.format("%.1f", cd.getValue().getGrade()) : "—"));

        recentTable.getColumns().addAll(sNameCol, sCourseCol, sDateCol, sStatusCol, sGradeCol);

        // Collect all enrollments across my courses (already computed above)
        recentTable.setItems(FXCollections.observableArrayList(allEnrollments));
        recentCard.getChildren().add(recentTable);

        page.getChildren().addAll(welcome, sub, cards, coursesCard, recentCard);
        root.getChildren().add(scroll);
    }

    private VBox statCard(String icon, String title, String value, String subtitle, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:10; " +
            "-fx-border-color:" + color + "; -fx-border-width:0 0 0 4; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:18px;");
        top.getChildren().addAll(titleLbl, sp, iconLbl);

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size:30px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        Label subLbl = new Label("▤ " + subtitle);
        subLbl.setStyle("-fx-font-size:11px; -fx-text-fill:" + color + ";");

        card.getChildren().addAll(top, valLbl, subLbl);
        return card;
    }

    private VBox card(String title) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color:white; -fx-background-radius:10; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        card.getChildren().addAll(lbl, new Separator());
        return card;
    }
}
