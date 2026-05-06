package com.lms.analytics.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;

import com.lms.analytics.services.AuthService;
import com.lms.analytics.utils.SessionManager;
import com.lms.analytics.utils.SceneUtil;
import com.lms.analytics.utils.ThemeManager;
import com.lms.analytics.models.User;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class MainController {

    @FXML private StackPane mainRoot;

    private BorderPane rootLayout;
    private StackPane contentArea;
    private Label statusLabel;
    private Label clockLabel;
    private Timeline clockTimeline;
    private AuthService authService;
    private Button activeNavBtn = null;
    private String currentRole = "STUDENT";

    private final ThemeManager theme = ThemeManager.getInstance();

    @FXML
    public void initialize() {
        authService = new AuthService();
        User user = SessionManager.getInstance().getCurrentUser();
        if (user != null) currentRole = user.getRole();
        buildLayout();
        startClock();
        if (isAdmin())       showDashboard();
        else if (isInstructor()) showInstructorDashboard();
        else                 showBrowseCourses();
    }

    // ── LAYOUT ────────────────────────────────────────────────────────
    private void buildLayout() {
        rootLayout = new BorderPane();
        applyRootStyle();
        rootLayout.setLeft(buildSidebar());
        rootLayout.setTop(buildTopBar());
        contentArea = new StackPane();
        contentArea.setId("contentArea");
        applyContentStyle();
        rootLayout.setCenter(contentArea);
        rootLayout.setBottom(buildStatusBar());
        mainRoot.getChildren().add(rootLayout);
    }

    private void applyRootStyle() {
        rootLayout.setStyle("-fx-background-color:" + theme.bg() + ";");
    }

    private void applyContentStyle() {
        contentArea.setStyle("-fx-background-color:" + theme.contentBg() + ";");
    }

    // ── THEME TOGGLE ──────────────────────────────────────────────────
    private void toggleTheme() {
        theme.toggle();
        // Rebuild entire layout with new theme
        mainRoot.getChildren().clear();
        activeNavBtn = null;
        buildLayout();
        startClock();
        if (isAdmin())           showDashboard();
        else if (isInstructor()) showInstructorDashboard();
        else                     showBrowseCourses();
    }

    // ── SIDEBAR ───────────────────────────────────────────────────────
    private VBox buildSidebar() {
        VBox sidebar = new VBox(0);
        sidebar.setPrefWidth(210);
        sidebar.setStyle("-fx-background-color:" + theme.sidebar() + ";");

        VBox nav = new VBox(2);
        nav.setPadding(new Insets(8, 8, 12, 8));
        VBox.setVgrow(nav, Priority.ALWAYS);

        if (isAdmin()) {
            nav.getChildren().add(navSection("MAIN"));
            Button dashBtn = navBtn("🏠", "Dashboard", this::showDashboard);
            nav.getChildren().add(dashBtn);
            activeNavBtn = dashBtn; setActive(dashBtn);

            nav.getChildren().add(navSection("MANAGEMENT"));
            nav.getChildren().add(navBtn("📚", "Courses",         this::showCourses));
            nav.getChildren().add(navBtn("👥", "Students",        this::showStudents));
            nav.getChildren().add(navBtn("📝", "Enrollments",     this::showEnrollments));
            nav.getChildren().add(navBtn("👨‍🏫", "Instructors",   this::showInstructors));

            nav.getChildren().add(navSection("REPORTS"));
            nav.getChildren().add(navBtn("📊", "Analytics",       this::showPerformance));
            nav.getChildren().add(navBtn("📄", "Reports",         this::showReports));
            nav.getChildren().add(navBtn("📥", "Import / Export", this::showImport));
            nav.getChildren().add(navBtn("🔑", "Passwords",       this::showPasswordMgmt));
        }

        if (isInstructor()) {
            nav.getChildren().add(navSection("MAIN"));
            Button dashBtn = navBtn("🏠", "My Dashboard", this::showInstructorDashboard);
            nav.getChildren().add(dashBtn);
            activeNavBtn = dashBtn; setActive(dashBtn);

            nav.getChildren().add(navSection("MY COURSES"));
            nav.getChildren().add(navBtn("📚", "My Courses",      this::showMyCourses));
            nav.getChildren().add(navBtn("📝", "Enrollments",     this::showInstructorEnrollments));
            nav.getChildren().add(navBtn("🎓", "Grade Students",  this::showGradeStudents));

            nav.getChildren().add(navSection("REPORTS"));
            nav.getChildren().add(navBtn("📊", "Analytics",       this::showInstructorAnalytics));
            nav.getChildren().add(navBtn("📄", "Reports",         this::showReports));
        }

        if (isStudent()) {
            nav.getChildren().add(navSection("MY LEARNING"));
            Button browseBtn = navBtn("📚", "Browse Courses", this::showBrowseCourses);
            nav.getChildren().add(browseBtn);
            activeNavBtn = browseBtn; setActive(browseBtn);

            nav.getChildren().add(navBtn("📝", "My Enrollments", this::showMyEnrollments));
            nav.getChildren().add(navBtn("🎓", "My Grades",      this::showMyGrades));
            nav.getChildren().add(navBtn("👤", "My Profile",     this::showMyProfile));
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        nav.getChildren().add(spacer);

        Button logoutBtn = new Button("LOG OUT");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setStyle(logoutStyle(false));
        logoutBtn.setOnMouseEntered(e -> logoutBtn.setStyle(logoutStyle(true)));
        logoutBtn.setOnMouseExited(e  -> logoutBtn.setStyle(logoutStyle(false)));
        logoutBtn.setOnAction(e -> handleLogout());
        nav.getChildren().add(logoutBtn);

        Label version = new Label("OCES v2025");
        version.setStyle("-fx-font-size:10px; -fx-text-fill:" + theme.navSection() +
                "; -fx-padding:6 0 0 4;");
        nav.getChildren().add(version);

        sidebar.getChildren().add(nav);
        return sidebar;
    }

    // ── TOP BAR ───────────────────────────────────────────────────────
    private HBox buildTopBar() {
        HBox bar = new HBox(14);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(0, 16, 0, 0));
        bar.setPrefHeight(52); bar.setMinHeight(52); bar.setMaxHeight(52);
        // Top bar always dark — white text is always visible
        bar.setStyle("-fx-background-color:#1e293b; -fx-border-color:#334155; -fx-border-width:0 0 1 0;");

        // Logo block (same width as sidebar) — always dark background, always white text
        HBox logoBlock = new HBox(10);
        logoBlock.setAlignment(Pos.CENTER_LEFT);
        logoBlock.setPrefWidth(210); logoBlock.setMinWidth(210); logoBlock.setMaxWidth(210);
        logoBlock.setPadding(new Insets(0, 12, 0, 14));
        logoBlock.setStyle("-fx-background-color:#1e293b;"); // always dark, never changes

        Label logoIcon = new Label("🎓");
        logoIcon.setStyle("-fx-font-size:22px; -fx-text-fill:white;");

        VBox logoText = new VBox(1);
        Label appName = new Label("OCES");
        appName.setStyle("-fx-font-size:16px; -fx-font-weight:bold; -fx-text-fill:white;");
        Label logoSub = new Label("Online Course Enrollment");
        logoSub.setStyle("-fx-font-size:9px; -fx-text-fill:rgba(255,255,255,0.6);");
        logoText.getChildren().addAll(appName, logoSub);
        logoBlock.getChildren().addAll(logoIcon, logoText);

        // Center title
        VBox titleBox = new VBox(1);
        titleBox.setAlignment(Pos.CENTER);
        Label titleLbl = new Label("Online Course Enrollment System");
        titleLbl.setStyle("-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:white;");
        Label subLbl = new Label("OCES Analytics Platform · 2025");
        subLbl.setStyle("-fx-font-size:10px; -fx-text-fill:rgba(255,255,255,0.55);");
        titleBox.getChildren().addAll(titleLbl, subLbl);

        Region sl = new Region(); HBox.setHgrow(sl, Priority.ALWAYS);
        Region sr = new Region(); HBox.setHgrow(sr, Priority.ALWAYS);

        clockLabel = new Label();
        clockLabel.setStyle("-fx-font-size:11px; -fx-text-fill:rgba(255,255,255,0.65);");

        Label onlineDot = new Label("● Online");
        onlineDot.setStyle("-fx-font-size:11px; -fx-text-fill:" + theme.onlineColor() + ";");

        // User info
        User user = SessionManager.getInstance().getCurrentUser();
        String name     = user != null ? user.getFullName() : "User";
        String role     = user != null ? user.getRole() : "";
        String initials = name.length() >= 1 ? String.valueOf(name.charAt(0)).toUpperCase() : "U";

        VBox userInfo = new VBox(1);
        userInfo.setAlignment(Pos.CENTER_RIGHT);
        Label nameLbl = new Label(name);
        nameLbl.setStyle("-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:white;");
        Label roleLbl = new Label("● " + role);
        roleLbl.setStyle("-fx-font-size:10px; -fx-text-fill:rgba(255,255,255,0.6);");
        userInfo.getChildren().addAll(nameLbl, roleLbl);

        // Avatar
        StackPane avatar = new StackPane();
        Circle circle = new Circle(16);
        String avatarColor = isAdmin() ? "#da3633" : isInstructor() ? "#1f6feb" : "#238636";
        circle.setStyle("-fx-fill:" + avatarColor + ";");
        Label initLbl = new Label(initials);
        initLbl.setStyle("-fx-font-size:13px; -fx-font-weight:bold; -fx-text-fill:white;");
        avatar.getChildren().addAll(circle, initLbl);

        // ── Clickable profile area (userInfo + avatar) ─────────────
        HBox profileBtn = new HBox(8, userInfo, avatar);
        profileBtn.setAlignment(Pos.CENTER_RIGHT);
        profileBtn.setPadding(new Insets(4, 8, 4, 8));
        profileBtn.setStyle("-fx-cursor:hand; -fx-background-radius:8;");
        profileBtn.setOnMouseEntered(e -> profileBtn.setStyle(
            "-fx-cursor:hand; -fx-background-radius:8; " +
            "-fx-background-color:rgba(56,189,248,0.12);"));
        profileBtn.setOnMouseExited(e -> profileBtn.setStyle(
            "-fx-cursor:hand; -fx-background-radius:8;"));
        Tooltip.install(profileBtn, new Tooltip("View My Profile"));
        profileBtn.setOnMouseClicked(e -> {
            if (isStudent())    showMyProfile();
            else if (isAdmin()) showAdminProfile();
            else                showInstructorProfile();
        });

        // ── Dark / Light toggle button ─────────────────────────────
        Button themeToggle = new Button(theme.isDark() ? "☀" : "🌙");
        themeToggle.setStyle(theme.toggleBtnStyle());
        themeToggle.setTooltip(new Tooltip(theme.isDark() ? "Switch to Light Mode" : "Switch to Dark Mode"));
        themeToggle.setOnAction(e -> toggleTheme());
        themeToggle.setOnMouseEntered(e -> themeToggle.setStyle(
            theme.toggleBtnStyle().replace("-fx-background-color:#21262d",
                "-fx-background-color:#30363d")
            .replace("-fx-background-color:#f1f5f9",
                "-fx-background-color:#e2e8f0")));
        themeToggle.setOnMouseExited(e -> themeToggle.setStyle(theme.toggleBtnStyle()));

        bar.getChildren().addAll(
            logoBlock, sl, titleBox, sr,
            clockLabel, onlineDot, themeToggle, profileBtn
        );
        return bar;
    }

    // ── STATUS BAR ────────────────────────────────────────────────────
    private HBox buildStatusBar() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(6, 20, 6, 20));
        bar.setStyle("-fx-background-color:" + theme.statusBar() +
                "; -fx-border-color:" + theme.topBorder() + "; -fx-border-width:1 0 0 0;");

        statusLabel = new Label("Ready");
        statusLabel.setStyle("-fx-font-size:11px; -fx-text-fill:" + theme.statusText() + ";");

        Region sl = new Region(); HBox.setHgrow(sl, Priority.ALWAYS);

        Label devLabel = new Label(
            "✦  Developed by Mohammednur Seid  ✦");
        devLabel.setStyle(
            "-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:white; " +
            "-fx-background-color:" + theme.devBoxBg() + "; " +
            "-fx-border-color:" + theme.devBoxBorder() + "; " +
            "-fx-border-radius:6; -fx-background-radius:6; -fx-padding:4 18;");

        Region sr = new Region(); HBox.setHgrow(sr, Priority.ALWAYS);
        bar.getChildren().addAll(statusLabel, sl, devLabel, sr);
        return bar;
    }

    // ── CLOCK ─────────────────────────────────────────────────────────
    private void startClock() {
        if (clockTimeline != null) clockTimeline.stop();
        clockTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (clockLabel != null)
                clockLabel.setText(LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd  HH:mm:ss")));
        }));
        clockTimeline.setCycleCount(Timeline.INDEFINITE);
        clockTimeline.play();
    }

    // ── VIEW LOADING ──────────────────────────────────────────────────
    private void loadView(String fxmlPath, String viewName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            contentArea.getChildren().setAll(root);
            if (statusLabel != null) statusLabel.setText(viewName + " loaded");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load " + viewName + ": " + e.getMessage());
        }
    }

    // ── NAV ACTIONS ───────────────────────────────────────────────────
    private void showDashboard()          { loadView("/fxml/DashboardView.fxml",              "Dashboard"); }
    private void showCourses()            { loadView("/fxml/CourseManagementView.fxml",        "Courses"); }
    private void showStudents()           { loadView("/fxml/StudentManagementView.fxml",       "Students"); }
    private void showEnrollments()        { loadView("/fxml/EnrollmentView.fxml",              "Enrollments"); }
    private void showInstructors()        { loadView("/fxml/InstructorManagementView.fxml",    "Instructors"); }
    private void showReports()            { loadView("/fxml/ReportsView.fxml",                 "Reports"); }
    private void showImport()             { loadView("/fxml/ImportExportView.fxml",            "Import/Export"); }
    private void showPasswordMgmt()       { loadView("/fxml/PasswordManagementView.fxml",      "Password Management"); }
    private void showPerformance()        { loadView("/fxml/DashboardView.fxml",               "Analytics"); }
    private void showInstructorDashboard(){ loadView("/fxml/InstructorDashboardView.fxml",     "My Dashboard"); }
    private void showMyCourses()          { loadView("/fxml/InstructorCoursesView.fxml",       "My Courses"); }
    private void showGradeStudents()      { loadView("/fxml/GradeStudentsView.fxml",           "Grade Students"); }
    // Instructor-specific pages (different from admin's)
    private void showInstructorEnrollments() { loadView("/fxml/InstructorEnrollmentsView.fxml", "My Enrollments"); }
    private void showInstructorAnalytics()   { loadView("/fxml/InstructorAnalyticsView.fxml",   "My Analytics"); }
    private void showBrowseCourses()      { loadView("/fxml/BrowseCoursesView.fxml",           "Browse Courses"); }
    private void showMyEnrollments()      { loadView("/fxml/MyEnrollmentsView.fxml",           "My Enrollments"); }
    private void showMyGrades()           { loadView("/fxml/MyGradesView.fxml",                "My Grades"); }
    private void showMyProfile()          { loadView("/fxml/MyProfileView.fxml",               "My Profile"); }

    private void showAdminProfile() {
        // Admin sees a simple profile page
        loadView("/fxml/MyProfileView.fxml", "My Profile");
    }

    private void showInstructorProfile() {
        loadView("/fxml/MyProfileView.fxml", "My Profile");
    }

    // ── LOGOUT ────────────────────────────────────────────────────────
    private void handleLogout() {
        Alert dlg = new Alert(Alert.AlertType.CONFIRMATION,
            "Are you sure you want to logout?", ButtonType.YES, ButtonType.NO);
        dlg.setTitle("Logout"); dlg.setHeaderText("Confirm Logout");
        dlg.showAndWait().ifPresent(r -> {
            if (r == ButtonType.YES) {
                authService.logout();
                SessionManager.getInstance().endSession();
                if (clockTimeline != null) clockTimeline.stop();
                try {
                    Stage stage = (Stage) mainRoot.getScene().getWindow();
                    FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/fxml/LandingView.fxml"));
                    Parent root = loader.load();
                    Scene scene = SceneUtil.create(root,
                        javafx.scene.paint.Color.web("#0f172a"), getClass());
                    stage.setScene(scene);
                    stage.setMaximized(false);
                    stage.setWidth(1100); stage.setHeight(720);
                    stage.setResizable(true); stage.centerOnScreen();
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }

    // ── HELPERS ───────────────────────────────────────────────────────
    private boolean isAdmin()      { return "ADMIN".equals(currentRole); }
    private boolean isInstructor() { return "INSTRUCTOR".equals(currentRole); }
    private boolean isStudent()    { return "STUDENT".equals(currentRole); }

    private Label navSection(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:10px; -fx-font-weight:bold; " +
            "-fx-text-fill:" + theme.navSection() + "; -fx-padding:12 8 4 8;");
        return lbl;
    }

    private Button navBtn(String icon, String label, Runnable action) {
        Button btn = new Button(icon + "  " + label);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setStyle(navBtnStyle(false));
        btn.setOnMouseEntered(e -> { if (btn != activeNavBtn) btn.setStyle(navBtnHoverStyle()); });
        btn.setOnMouseExited(e  -> { if (btn != activeNavBtn) btn.setStyle(navBtnStyle(false)); });
        btn.setOnAction(e -> {
            if (activeNavBtn != null) activeNavBtn.setStyle(navBtnStyle(false));
            activeNavBtn = btn; setActive(btn); action.run();
        });
        return btn;
    }

    private void setActive(Button btn) { btn.setStyle(navBtnStyle(true)); }

    private String navBtnStyle(boolean active) {
        return active
            ? "-fx-background-color:#1f6feb; -fx-text-fill:white; " +
              "-fx-font-size:13px; -fx-background-radius:8; " +
              "-fx-padding:10 12; -fx-cursor:hand; -fx-alignment:CENTER_LEFT;"
            : "-fx-background-color:transparent; -fx-text-fill:" + theme.navInactive() + "; " +
              "-fx-font-size:13px; -fx-background-radius:8; " +
              "-fx-padding:10 12; -fx-cursor:hand; -fx-alignment:CENTER_LEFT;";
    }

    private String navBtnHoverStyle() {
        return "-fx-background-color:" + theme.navHover() + "; " +
               "-fx-text-fill:" + theme.textPrimary() + "; " +
               "-fx-font-size:13px; -fx-background-radius:8; " +
               "-fx-padding:10 12; -fx-cursor:hand; -fx-alignment:CENTER_LEFT;";
    }

    private String logoutStyle(boolean hover) {
        return (hover ? "-fx-background-color:#b91c1c;" : "-fx-background-color:#da3633;") +
            "-fx-text-fill:white; -fx-font-weight:bold; " +
            "-fx-font-size:12px; -fx-background-radius:8; -fx-padding:10 0; -fx-cursor:hand;";
    }

    private void showError(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }
}
