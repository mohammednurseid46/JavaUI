package com.lms.analytics.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.control.ScrollPane;
import javafx.util.Duration;

import com.lms.analytics.services.AnalyticsService;
import com.lms.analytics.utils.SessionManager;
import com.lms.analytics.utils.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class DashboardController {

    @FXML private StackPane dashboardRoot;

    private AnalyticsService analyticsService;
    private Timeline refreshTimeline;

    // Live stat labels
    private Label totalStudentsVal;
    private Label activeCoursesVal;
    private Label totalEnrollmentsVal;
    private Label completionRateVal;
    private Label avgGradeVal;
    private Label totalRevenueVal;
    private Label avgOccupancyLabel;
    private Label instructorsLabel;

    // Charts & table
    private BarChart<String, Number> trendChart;
    private VBox activityLog;
    private Label lastUpdatedLabel;

    @FXML
    public void initialize() {
        analyticsService = new AnalyticsService();
        buildUI();
        loadData();
        startAutoRefresh();
    }

    // ── BUILD UI ──────────────────────────────────────────────────────
    private void buildUI() {
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:#f0f2f5; -fx-background:#f0f2f5;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox page = new VBox(20);
        page.setPadding(new Insets(20, 24, 24, 24));
        page.setStyle("-fx-background-color:#f0f2f5;");
        scroll.setContent(page);

        // ── Search + status row ───────────────────────────────────────
        HBox topRow = buildTopRow();

        // ── Quick Actions ─────────────────────────────────────────────
        VBox quickActions = buildQuickActions();

        // ── System Overview ───────────────────────────────────────────
        VBox overview = buildSystemOverview();

        // ── Charts + Activity Log ─────────────────────────────────────
        HBox chartsRow = buildChartsRow();

        page.getChildren().addAll(topRow, quickActions, overview, chartsRow);
        dashboardRoot.getChildren().add(scroll);
    }

    // ── TOP ROW ───────────────────────────────────────────────────────
    private HBox buildTopRow() {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        // Search bar
        TextField search = new TextField();
        search.setPromptText("🔍  Global Search: Find students, courses, or enrollments...");
        search.setPrefWidth(420);
        search.setStyle(
            "-fx-background-color:white; -fx-border-color:#e2e8f0; " +
            "-fx-border-radius:20; -fx-background-radius:20; " +
            "-fx-padding:9 16; -fx-font-size:13px; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),6,0,0,2);");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Online badge
        Label online = new Label("● Server: Online");
        online.setStyle(
            "-fx-background-color:white; -fx-text-fill:#16a34a; " +
            "-fx-font-size:12px; -fx-font-weight:bold; " +
            "-fx-background-radius:20; -fx-border-color:#bbf7d0; " +
            "-fx-border-radius:20; -fx-padding:7 14;");

        lastUpdatedLabel = new Label();
        lastUpdatedLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#94a3b8;");

        row.getChildren().addAll(search, spacer, lastUpdatedLabel, online);
        return row;
    }

    // ── QUICK ACTIONS ─────────────────────────────────────────────────
    private VBox buildQuickActions() {
        VBox box = new VBox(12);

        Label title = sectionTitle("⚡ Quick Actions");

        HBox btns = new HBox(12);
        
        Button addCourseBtn = quickBtn("+ Add New Course");
        addCourseBtn.setOnAction(e -> navigateToCourses());
        
        Button registerStudentBtn = quickBtn("+ Register Student");
        registerStudentBtn.setOnAction(e -> navigateToStudents());
        
        Button manageEnrollBtn = quickBtn("⊘ Manage Enrollments");
        manageEnrollBtn.setOnAction(e -> navigateToEnrollments());
        
        Button viewReportsBtn = quickBtn("☑ View Reports");
        viewReportsBtn.setOnAction(e -> navigateToReports());

        btns.getChildren().addAll(addCourseBtn, registerStudentBtn, manageEnrollBtn, viewReportsBtn);

        box.getChildren().addAll(title, btns);
        return box;
    }

    private void navigateToCourses() {
        com.lms.analytics.utils.NavigationUtil.navigateTo(dashboardRoot, "/fxml/CourseManagementView.fxml");
    }

    private void navigateToStudents() {
        com.lms.analytics.utils.NavigationUtil.navigateTo(dashboardRoot, "/fxml/StudentManagementView.fxml");
    }

    private void navigateToEnrollments() {
        com.lms.analytics.utils.NavigationUtil.navigateTo(dashboardRoot, "/fxml/EnrollmentView.fxml");
    }

    private void navigateToReports() {
        com.lms.analytics.utils.NavigationUtil.navigateTo(dashboardRoot, "/fxml/ReportsView.fxml");
    }

    private Button quickBtn(String text) {
        final String normal =
            "-fx-background-color:#38bdf8; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:9 18; -fx-cursor:hand; " +
            "-fx-effect:dropshadow(gaussian,rgba(56,189,248,0.35),8,0,0,2);";
        final String hover =
            "-fx-background-color:#0ea5e9; -fx-text-fill:white; " +
            "-fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:9 18; -fx-cursor:hand; " +
            "-fx-effect:dropshadow(gaussian,rgba(14,165,233,0.45),10,0,0,3);";
        Button btn = new Button(text);
        btn.setStyle(normal);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(normal));
        return btn;
    }

    // ── SYSTEM OVERVIEW ───────────────────────────────────────────────
    private VBox buildSystemOverview() {
        VBox box = new VBox(12);

        Label title = sectionTitle("▦ System Overview");

        // All stat value labels — will be populated by loadData()
        totalStudentsVal    = bigStatVal("—");
        activeCoursesVal    = bigStatVal("—");
        totalEnrollmentsVal = bigStatVal("—");
        completionRateVal   = bigStatVal("—");
        avgGradeVal         = bigStatVal("—");
        totalRevenueVal     = bigStatVal("—");
        Label avgOccupancyVal  = bigStatVal("—");
        Label instructorsVal   = bigStatVal("—");

        // Store extra labels for loadData()
        avgOccupancyLabel = avgOccupancyVal;
        instructorsLabel  = instructorsVal;

        // Consistent light blue accent color for all cards
        String c1 = "#38bdf8", c2 = "#0ea5e9", c3 = "#0284c7", c4 = "#0369a1";

        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            overviewCard("Total Students",    totalStudentsVal,    "👥", c1, "Registered students"),
            overviewCard("Active Courses",    activeCoursesVal,    "📚", c2, "Active courses"),
            overviewCard("Total Enrollments", totalEnrollmentsVal, "📝", c3, "All enrollments"),
            overviewCard("Completion Rate",   completionRateVal,   "✅", c4, "Completion %")
        );
        for (javafx.scene.Node n : cards.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        HBox cards2 = new HBox(16);
        cards2.getChildren().addAll(
            overviewCard("Average Grade",  avgGradeVal,      "📊", c1, "Grade average"),
            overviewCard("Total Revenue",  totalRevenueVal,  "💰", c2, "Payments received"),
            overviewCard("Avg Occupancy",  avgOccupancyVal,  "📈", c3, "Course fill rate"),
            overviewCard("Instructors",    instructorsVal,   "👨‍🏫", c4, "Active instructors")
        );
        for (javafx.scene.Node n : cards2.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        box.getChildren().addAll(title, cards, cards2);
        return box;
    }

    private VBox overviewCard(String title, Label valLabel, String icon,
                               String accentColor, String subText) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(16, 16, 14, 16));
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:10; " +
            "-fx-border-color:" + accentColor + "; -fx-border-width:0 0 0 4; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:18px;");
        topRow.getChildren().addAll(titleLbl, sp, iconLbl);

        valLabel.setStyle("-fx-font-size:32px; -fx-font-weight:bold; " +
                "-fx-text-fill:#0f172a;");

        Label sub = new Label("▤ " + subText);
        sub.setStyle("-fx-font-size:11px; -fx-text-fill:" + accentColor + ";");

        card.getChildren().addAll(topRow, valLabel, sub);
        return card;
    }

    private Label bigStatVal(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:32px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        return l;
    }

    // ── CHARTS ROW ────────────────────────────────────────────────────
    private HBox buildChartsRow() {
        HBox row = new HBox(16);

        // Monthly trend bar chart
        VBox chartCard = buildTrendChartCard();
        HBox.setHgrow(chartCard, Priority.ALWAYS);

        // Live activity log
        VBox logCard = buildActivityLogCard();
        logCard.setPrefWidth(340);

        row.getChildren().addAll(chartCard, logCard);
        return row;
    }

    private VBox buildTrendChartCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:10; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

        Label title = new Label("▦ Monthly Enrollment Trends");
        title.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");

        CategoryAxis x = new CategoryAxis();
        x.setLabel("Month");
        NumberAxis y = new NumberAxis();
        y.setLabel("Enrollments");
        y.setAutoRanging(true);        // auto-scale to actual data
        y.setForceZeroInRange(true);   // always start from 0

        trendChart = new BarChart<>(x, y);
        trendChart.setAnimated(false);
        trendChart.setLegendVisible(false);
        trendChart.setPrefHeight(260);
        trendChart.setBarGap(4);
        trendChart.setCategoryGap(16);
        trendChart.setStyle("-fx-background-color:transparent;");

        card.getChildren().addAll(title, trendChart);
        VBox.setVgrow(trendChart, Priority.ALWAYS);
        return card;
    }

    private VBox buildActivityLogCard() {
        VBox card = new VBox(12);
        card.setPadding(new Insets(16));
        card.setStyle(
            "-fx-background-color:white; -fx-background-radius:10; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.07),8,0,0,2);");

        HBox titleRow = new HBox(8);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("▦ Live Activity Log");
        title.setStyle("-fx-font-size:14px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        Label liveDot = new Label("●");
        liveDot.setStyle("-fx-text-fill:#16a34a; -fx-font-size:10px;");
        titleRow.getChildren().addAll(liveDot, title);

        activityLog = new VBox(8);

        ScrollPane logScroll = new ScrollPane(activityLog);
        logScroll.setFitToWidth(true);
        logScroll.setPrefHeight(280);
        logScroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");

        card.getChildren().addAll(titleRow, logScroll);
        return card;
    }

    // ── DATA LOADING ──────────────────────────────────────────────────
    private void loadData() {
        Map<String, Object> stats = analyticsService.getDashboardStats();

        totalStudentsVal.setText(String.valueOf(stats.getOrDefault("totalStudents", 0)));
        activeCoursesVal.setText(String.valueOf(stats.getOrDefault("activeCourses", 0)));
        totalEnrollmentsVal.setText(String.valueOf(stats.getOrDefault("totalEnrollments", 0)));

        double compRate = ((Number) stats.getOrDefault("completionRate", 0.0)).doubleValue();
        completionRateVal.setText(String.format("%.1f%%", compRate));

        double avgGrade = ((Number) stats.getOrDefault("averageGrade", 0.0)).doubleValue();
        avgGradeVal.setText(avgGrade > 0 ? String.format("%.1f%%", avgGrade) : "N/A");

        double revenue = ((Number) stats.getOrDefault("totalRevenue", 0.0)).doubleValue();
        totalRevenueVal.setText(String.format("$%,.0f", revenue));

        // Live: average occupancy rate across all active courses
        if (avgOccupancyLabel != null) {
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT AVG(CAST(enrolled_count AS REAL)/NULLIF(capacity,0)*100) " +
                     "FROM courses WHERE status='ACTIVE' AND capacity > 0")) {
                if (rs.next()) {
                    double occ = rs.getDouble(1);
                    avgOccupancyLabel.setText(rs.wasNull() ? "N/A" : String.format("%.0f%%", occ));
                }
            } catch (SQLException e) { avgOccupancyLabel.setText("N/A"); }
        }

        // Live: active instructor count
        if (instructorsLabel != null) {
            try (Connection conn = DatabaseConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                     "SELECT COUNT(*) FROM users WHERE role='INSTRUCTOR' AND is_active=1")) {
                if (rs.next()) instructorsLabel.setText(String.valueOf(rs.getInt(1)));
            } catch (SQLException e) { instructorsLabel.setText("N/A"); }
        }

        loadTrendChart();
        loadActivityLog();

        if (lastUpdatedLabel != null)
            lastUpdatedLabel.setText("Updated: " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }

    private void loadTrendChart() {
        trendChart.getData().clear();

        java.util.Map<String, Integer> trends = analyticsService.getMonthlyEnrollmentTrends();

        if (trends.isEmpty()) {
            // No data — show a placeholder message
            trendChart.setTitle("No enrollment data yet");
            return;
        }

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Enrollments");
        trends.forEach((month, count) ->
            series.getData().add(new XYChart.Data<>(month, count)));
        trendChart.getData().add(series);

        // Style bars with light blue after chart renders
        javafx.application.Platform.runLater(() -> {
            trendChart.lookupAll(".chart-bar").forEach(node ->
                node.setStyle(
                    "-fx-bar-fill:#38bdf8; " +
                    "-fx-background-radius:4 4 0 0;"));
        });
    }

    private void loadActivityLog() {
        if (activityLog == null) return;
        activityLog.getChildren().clear();

        String sql = """
            SELECT u.full_name, c.course_name, e.enrollment_date, e.status
            FROM enrollments e
            JOIN students s ON e.student_id = s.student_id
            JOIN users u ON s.user_id = u.user_id
            JOIN courses c ON e.course_id = c.course_id
            ORDER BY e.enrollment_date DESC LIMIT 12
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String name   = rs.getString("full_name");
                String course = rs.getString("course_name");
                String date   = rs.getString("enrollment_date");
                String status = rs.getString("status");
                activityLog.getChildren().add(activityItem(name, course, date, status));
            }

            if (activityLog.getChildren().isEmpty()) {
                Label empty = new Label("No recent activity.");
                empty.setStyle("-fx-text-fill:#94a3b8; -fx-font-size:12px;");
                activityLog.getChildren().add(empty);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            Label err = new Label("Could not load activity.");
            err.setStyle("-fx-text-fill:#ef4444; -fx-font-size:12px;");
            activityLog.getChildren().add(err);
        }
    }

    private HBox activityItem(String name, String course, String date, String status) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(8, 10, 8, 10));
        row.setStyle(
            "-fx-background-color:#f8fafc; -fx-background-radius:8; " +
            "-fx-border-color:#e2e8f0; -fx-border-radius:8; -fx-border-width:1;");

        // Dot
        Label dot = new Label("●");
        String dotColor = "COMPLETED".equals(status) ? "#16a34a"
                        : "DROPPED".equals(status)   ? "#dc2626"
                        : "#f97316";
        dot.setStyle("-fx-text-fill:" + dotColor + "; -fx-font-size:10px;");

        VBox info = new VBox(2);
        Label nameLbl = new Label(name + " enrolled in '" + course + "'");
        nameLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#1e293b; -fx-font-weight:bold;");
        nameLbl.setWrapText(true);
        Label dateLbl = new Label(date != null ? date : "");
        dateLbl.setStyle("-fx-font-size:10px; -fx-text-fill:#94a3b8;");
        info.getChildren().addAll(nameLbl, dateLbl);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Status badge
        Label badge = new Label(status);
        String badgeBg = "COMPLETED".equals(status) ? "#dcfce7"
                       : "DROPPED".equals(status)   ? "#fee2e2"
                       : "#fff7ed";
        String badgeFg = "COMPLETED".equals(status) ? "#16a34a"
                       : "DROPPED".equals(status)   ? "#dc2626"
                       : "#ea580c";
        badge.setStyle(
            "-fx-background-color:" + badgeBg + "; -fx-text-fill:" + badgeFg + "; " +
            "-fx-font-size:10px; -fx-font-weight:bold; " +
            "-fx-background-radius:10; -fx-padding:2 8;");

        row.getChildren().addAll(dot, info, badge);
        return row;
    }

    private void startAutoRefresh() {
        refreshTimeline = new Timeline(
                new KeyFrame(Duration.minutes(3), e -> loadData()));
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.play();
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private Label sectionTitle(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:#0f172a;");
        return l;
    }

    public void stopRefresh() {
        if (refreshTimeline != null) refreshTimeline.stop();
    }
}
