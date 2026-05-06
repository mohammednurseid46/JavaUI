package com.lms.analytics.controllers;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Objects;

import com.lms.analytics.utils.SceneUtil;

public class LandingController {

    @FXML private StackPane rootPane;

    @FXML
    public void initialize() {
        buildUI();
    }

    private void buildUI() {
        // ── Consistent dark background — no image ─────────────────────
        final String BG      = "#0f172a";
        final String BG2     = "#1e293b";
        final String ACCENT  = "#667eea";

        rootPane.setStyle("-fx-background-color:" + BG + ";");

        // ── Corner BGI.png logos — 4 corners, circular, 7% opacity ──────
        StackPane decorLayer = new StackPane();
        decorLayer.setMouseTransparent(true);

        // Load BGI.png
        javafx.scene.image.Image bgiImage = null;
        try {
            bgiImage = new javafx.scene.image.Image(
                java.util.Objects.requireNonNull(
                    getClass().getResourceAsStream("/images/BGI.png")));
        } catch (Exception ignored) {}

        if (bgiImage != null) {
            // Create 4 corner circular logo images — small, 7% opacity
            decorLayer.getChildren().addAll(
                cornerLogo(bgiImage, Pos.TOP_LEFT,     -40, -40),
                cornerLogo(bgiImage, Pos.TOP_RIGHT,     40, -40),
                cornerLogo(bgiImage, Pos.BOTTOM_LEFT,  -40,  40),
                cornerLogo(bgiImage, Pos.BOTTOM_RIGHT,  40,  40)
            );
        } else {
            // Fallback: subtle colored circles if image not found
            Circle c1 = circle(340, "rgba(102,126,234,0.07)");
            StackPane.setAlignment(c1, Pos.TOP_LEFT);
            c1.setTranslateX(-100); c1.setTranslateY(-100);
            Circle c2 = circle(280, "rgba(118,75,162,0.07)");
            StackPane.setAlignment(c2, Pos.BOTTOM_RIGHT);
            c2.setTranslateX(80); c2.setTranslateY(80);
            Circle c3 = circle(200, "rgba(72,187,120,0.07)");
            StackPane.setAlignment(c3, Pos.TOP_RIGHT);
            c3.setTranslateX(-60); c3.setTranslateY(140);
            Circle c4 = circle(220, "rgba(102,126,234,0.07)");
            StackPane.setAlignment(c4, Pos.BOTTOM_LEFT);
            c4.setTranslateX(-60); c4.setTranslateY(60);
            decorLayer.getChildren().addAll(c1, c2, c3, c4);
        }

        // Particle layer
        StackPane particleLayer = buildParticleLayer();
        particleLayer.setMouseTransparent(true);

        // ── Main scroll content ───────────────────────────────────────
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent; -fx-background:transparent;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        VBox page = new VBox(0);
        page.setStyle("-fx-background-color:transparent;");
        scroll.setContent(page);

        HBox nav      = buildNavBar();
        VBox hero     = buildHeroSection();
        VBox features = buildFeaturesSection();
        HBox stats    = buildStatsSection();
        VBox cta      = buildCtaSection();
        HBox footer   = buildFooter();

        page.getChildren().addAll(nav, hero, features, stats, cta, footer);
        rootPane.getChildren().addAll(decorLayer, particleLayer, scroll);

        animateEntrance(nav, hero, features);
    }

    // ── NAV BAR ───────────────────────────────────────────────────────
    private HBox buildNavBar() {
        HBox nav = new HBox();
        nav.setPadding(new Insets(16, 48, 16, 48));
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setStyle("-fx-background-color:rgba(15,23,42,0.95); " +
            "-fx-border-color:rgba(255,255,255,0.06); -fx-border-width:0 0 1 0;");

        // Logo — attractive with icon + text
        HBox logoBox = new HBox(10);
        logoBox.setAlignment(Pos.CENTER_LEFT);

        // Logo icon circle
        StackPane logoIcon = new StackPane();
        Circle logoBg = new Circle(18);
        logoBg.setStyle("-fx-fill:linear-gradient(to bottom right,#667eea,#764ba2);");
        Label logoEmoji = new Label("🎓");
        logoEmoji.setStyle("-fx-font-size:14px; -fx-text-fill:white;");
        logoIcon.getChildren().addAll(logoBg, logoEmoji);

        VBox logoText = new VBox(1);
        Label logoName = new Label("OCES");
        logoName.setStyle("-fx-font-size:18px; -fx-font-weight:bold; -fx-text-fill:white;");
        Label logoSub = new Label("Online Course Enrollment");
        logoSub.setStyle("-fx-font-size:9px; -fx-text-fill:rgba(255,255,255,0.5);");
        logoText.getChildren().addAll(logoName, logoSub);

        logoBox.getChildren().addAll(logoIcon, logoText);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button loginBtn = navButton("Sign In →", true);
        loginBtn.setOnAction(e -> navigateToLogin());

        nav.getChildren().addAll(logoBox, spacer, loginBtn);
        return nav;
    }

    // ── HERO ──────────────────────────────────────────────────────────
    private VBox buildHeroSection() {
        VBox hero = new VBox(24);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(80, 60, 80, 60));
        hero.setStyle("-fx-background-color:#0f172a;");

        // Badge
        Label badge = new Label("✦  Online Course Enrollment System  ✦");
        badge.setStyle(
            "-fx-background-color:rgba(102,126,234,0.35); " +
            "-fx-border-color:rgba(165,180,252,0.8); -fx-border-radius:20; " +
            "-fx-background-radius:20; -fx-padding:6 18; " +
            "-fx-text-fill:#e0e7ff; -fx-font-size:13px; -fx-font-weight:bold;");

        // Main headline
        Label headline = new Label("Manage. Analyze.\nGrow.");
        headline.setStyle(
            "-fx-font-size:64px; -fx-font-weight:bold; -fx-text-fill:white; " +
            "-fx-font-family:'Segoe UI'; -fx-text-alignment:center; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.9),12,0,0,3);");
        headline.setTextAlignment(TextAlignment.CENTER);
        headline.setWrapText(true);

        // Gradient text effect via colored label
        Label gradientWord = new Label("Smarter.");
        gradientWord.setStyle(
            "-fx-font-size:64px; -fx-font-weight:bold; " +
            "-fx-text-fill:#a5b4fc; " +
            "-fx-font-family:'Segoe UI'; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.9),12,0,0,3);");

        // Sub-headline
        Label sub = new Label(
            "A powerful analytics platform for online course enrollment.\n" +
            "Track students, manage courses, generate reports — all in one place.\n" +
            "OCES gives your institution the tools to succeed.");
        sub.setStyle(
            "-fx-font-size:17px; -fx-text-fill:rgba(255,255,255,0.92); " +
            "-fx-text-alignment:center; -fx-font-family:'Segoe UI'; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.8),6,0,0,1);");
        sub.setTextAlignment(TextAlignment.CENTER);
        sub.setWrapText(true);
        sub.setMaxWidth(620);

        // CTA buttons
        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);

        Button getStarted = ctaButton("Get Started →", true);
        getStarted.setOnAction(e -> navigateToLogin());

        Button learnMore = ctaButton("Learn More", false);
        learnMore.setOnAction(e -> {
            // Smooth scroll to features — just animate opacity pulse
            ScaleTransition pulse = new ScaleTransition(Duration.millis(200), learnMore);
            pulse.setToX(0.95); pulse.setToY(0.95);
            pulse.setAutoReverse(true); pulse.setCycleCount(2);
            pulse.play();
        });

        buttons.getChildren().addAll(getStarted, learnMore);

        // Trust line
        Label trust = new Label("✓ Free to use   ✓ No setup required   ✓ SQLite powered");
        trust.setStyle("-fx-font-size:13px; -fx-text-fill:rgba(255,255,255,0.75); " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.8),4,0,0,1);");

        hero.getChildren().addAll(badge, headline, gradientWord, sub, buttons, trust);
        return hero;
    }

    // ── FEATURES ──────────────────────────────────────────────────────
    private VBox buildFeaturesSection() {
        VBox section = new VBox(36);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(70, 48, 70, 48));
        section.setStyle("-fx-background-color:#1e293b;");
        section.setMaxWidth(Double.MAX_VALUE);

        // Section header
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);
        Label badge = new Label("✦  FEATURES  ✦");
        badge.setStyle(
            "-fx-background-color:rgba(102,126,234,0.2); " +
            "-fx-border-color:rgba(165,180,252,0.4); -fx-border-radius:20; " +
            "-fx-background-radius:20; -fx-padding:4 16; " +
            "-fx-text-fill:#a5b4fc; -fx-font-size:11px; -fx-font-weight:bold;");

        Label title = new Label("Everything you need");
        title.setStyle("-fx-font-size:34px; -fx-font-weight:bold; -fx-text-fill:white;");

        Label subtitle = new Label("OCES — Built for educators, administrators, and analysts.");
        subtitle.setStyle("-fx-font-size:14px; -fx-text-fill:rgba(255,255,255,0.65);");

        header.getChildren().addAll(badge, title, subtitle);

        // Feature cards — 3 per row using HBox rows
        String[][] features = {
            {"📊", "Analytics Dashboard",
             "Real-time stats on enrollments, completion rates, revenue, and grades at a glance."},
            {"📚", "Course Management",
             "Create, update, and manage courses with instructor assignments and capacity control."},
            {"👥", "Student Tracking",
             "Monitor student progress, enrollment history, and academic performance over time."},
            {"📄", "Report Generation",
             "Export PDF, Excel, and Word reports — accreditation, financial, and progress reports."},
            {"📥", "CSV Import/Export",
             "Bulk import students and courses from CSV files. Export data in multiple formats."},
            {"🔒", "Secure Auth",
             "BCrypt password hashing, role-based access (Admin, Instructor, Student), session management."}
        };

        // Row 1
        HBox row1 = new HBox(20);
        row1.setAlignment(Pos.CENTER);
        row1.setMaxWidth(Double.MAX_VALUE);
        // Row 2
        HBox row2 = new HBox(20);
        row2.setAlignment(Pos.CENTER);
        row2.setMaxWidth(Double.MAX_VALUE);

        for (int i = 0; i < 6; i++) {
            VBox card = featureCard(features[i][0], features[i][1], features[i][2]);
            HBox.setHgrow(card, Priority.ALWAYS);
            if (i < 3) row1.getChildren().add(card);
            else       row2.getChildren().add(card);
        }

        section.getChildren().addAll(header, row1, row2);
        return section;
    }

    // ── STATS ─────────────────────────────────────────────────────────
    private HBox buildStatsSection() {
        HBox stats = new HBox(0);
        stats.setAlignment(Pos.CENTER);
        stats.setPadding(new Insets(50, 60, 50, 60));
        stats.setStyle(
            "-fx-background-color:linear-gradient(to right, #667eea, #764ba2);");

        stats.getChildren().addAll(
            statItem("7+", "Core Modules"),
            statDivider(),
            statItem("100%", "SQLite Powered"),
            statDivider(),
            statItem("PDF/Excel", "Report Formats"),
            statDivider(),
            statItem("3 Roles", "Admin · Instructor · Student")
        );
        return stats;
    }

    // ── CTA SECTION ───────────────────────────────────────────────────
    private VBox buildCtaSection() {
        VBox cta = new VBox(20);
        cta.setAlignment(Pos.CENTER);
        cta.setPadding(new Insets(70, 60, 70, 60));
        cta.setStyle("-fx-background-color:#1e293b;");

        Label title = new Label("Ready to get started?");
        title.setStyle("-fx-font-size:38px; -fx-font-weight:bold; -fx-text-fill:white; " +
                "-fx-font-family:'Segoe UI'; " +
                "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.9),10,0,0,2);");

        Label sub = new Label("Sign in with your credentials and start managing your institution today.");
        sub.setStyle("-fx-font-size:15px; -fx-text-fill:rgba(255,255,255,0.90); " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.8),4,0,0,1);");
        sub.setWrapText(true);
        sub.setTextAlignment(TextAlignment.CENTER);

        Button btn = ctaButton("Open Login →", true);
        btn.setPrefWidth(200);
        btn.setOnAction(e -> navigateToLogin());

        cta.getChildren().addAll(title, sub, btn);
        return cta;
    }

    // ── FOOTER ────────────────────────────────────────────────────────
    private HBox buildFooter() {
        HBox footer = new HBox();
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(20, 40, 20, 40));
        footer.setStyle("-fx-background-color:#0f172a;");

        Label copy = new Label("© 2024 OCES — Online Course Enrollment System  ·  Built with JavaFX & SQLite");
        copy.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(255,255,255,0.70); " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.8),4,0,0,1);");

        footer.getChildren().add(copy);
        return footer;
    }

    // ── HELPERS ───────────────────────────────────────────────────────

    private VBox featureCard(String icon, String title, String desc) {
        VBox card = new VBox(14);
        card.setPadding(new Insets(24));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setStyle(
            "-fx-background-color:rgba(255,255,255,0.05); " +
            "-fx-background-radius:14; " +
            "-fx-border-color:rgba(102,126,234,0.25); " +
            "-fx-border-radius:14; " +
            "-fx-border-width:1;");

        // Icon in a colored circle — white emoji on purple background
        StackPane iconPane = new StackPane();
        Circle iconBg = new Circle(24);
        iconBg.setStyle("-fx-fill:rgba(102,126,234,0.35);");
        Label iconLbl = new Label(icon);
        iconLbl.setStyle("-fx-font-size:22px; -fx-text-fill:white;");
        iconPane.getChildren().addAll(iconBg, iconLbl);
        iconPane.setMaxWidth(48);
        iconPane.setMaxHeight(48);

        Label titleLbl = new Label(title);
        titleLbl.setStyle(
            "-fx-font-size:15px; -fx-font-weight:bold; -fx-text-fill:white;");
        titleLbl.setWrapText(true);

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size:12px; -fx-text-fill:rgba(255,255,255,0.70);");
        descLbl.setWrapText(true);

        card.getChildren().addAll(iconPane, titleLbl, descLbl);

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(
            "-fx-background-color:rgba(102,126,234,0.12); " +
            "-fx-background-radius:14; " +
            "-fx-border-color:rgba(102,126,234,0.6); " +
            "-fx-border-radius:14; -fx-border-width:1; " +
            "-fx-effect:dropshadow(gaussian,rgba(102,126,234,0.25),16,0,0,4); " +
            "-fx-cursor:hand;"));
        card.setOnMouseExited(e -> card.setStyle(
            "-fx-background-color:rgba(255,255,255,0.05); " +
            "-fx-background-radius:14; " +
            "-fx-border-color:rgba(102,126,234,0.25); " +
            "-fx-border-radius:14; -fx-border-width:1;"));

        return card;
    }

    private VBox statItem(String value, String label) {
        VBox item = new VBox(4);
        item.setAlignment(Pos.CENTER);
        item.setPadding(new Insets(0, 40, 0, 40));

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size:32px; -fx-font-weight:bold; -fx-text-fill:white; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.6),4,0,0,1);");

        Label lblLbl = new Label(label);
        lblLbl.setStyle("-fx-font-size:13px; -fx-text-fill:rgba(255,255,255,0.90);");

        item.getChildren().addAll(valLbl, lblLbl);
        return item;
    }

    private Region statDivider() {
        Region div = new Region();
        div.setPrefWidth(1);
        div.setPrefHeight(50);
        div.setStyle("-fx-background-color:rgba(255,255,255,0.25);");
        return div;
    }

    private Button ctaButton(String text, boolean primary) {
        Button btn = new Button(text);
        if (primary) {
            btn.setStyle(
                "-fx-background-color:linear-gradient(to right,#667eea,#764ba2); " +
                "-fx-text-fill:white; -fx-font-size:15px; -fx-font-weight:bold; " +
                "-fx-background-radius:30; -fx-padding:13 32; -fx-cursor:hand; " +
                "-fx-effect:dropshadow(gaussian,rgba(102,126,234,0.5),15,0,0,4);");
            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:linear-gradient(to right,#7c8ef0,#8a5cb8); " +
                "-fx-text-fill:white; -fx-font-size:15px; -fx-font-weight:bold; " +
                "-fx-background-radius:30; -fx-padding:13 32; -fx-cursor:hand; " +
                "-fx-effect:dropshadow(gaussian,rgba(102,126,234,0.7),20,0,0,6);"));
            btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:linear-gradient(to right,#667eea,#764ba2); " +
                "-fx-text-fill:white; -fx-font-size:15px; -fx-font-weight:bold; " +
                "-fx-background-radius:30; -fx-padding:13 32; -fx-cursor:hand; " +
                "-fx-effect:dropshadow(gaussian,rgba(102,126,234,0.5),15,0,0,4);"));
        } else {
            btn.setStyle(
                "-fx-background-color:transparent; " +
                "-fx-border-color:rgba(255,255,255,0.4); -fx-border-radius:30; " +
                "-fx-text-fill:white; -fx-font-size:15px; -fx-font-weight:bold; " +
                "-fx-padding:13 32; -fx-cursor:hand;");
            btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color:rgba(255,255,255,0.08); " +
                "-fx-border-color:rgba(255,255,255,0.7); -fx-border-radius:30; " +
                "-fx-text-fill:white; -fx-font-size:15px; -fx-font-weight:bold; " +
                "-fx-padding:13 32; -fx-cursor:hand;"));
            btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color:transparent; " +
                "-fx-border-color:rgba(255,255,255,0.4); -fx-border-radius:30; " +
                "-fx-text-fill:white; -fx-font-size:15px; -fx-font-weight:bold; " +
                "-fx-padding:13 32; -fx-cursor:hand;"));
        }
        return btn;
    }

    private Button navButton(String text, boolean primary) {
        Button btn = new Button(text);
        btn.setStyle(
            "-fx-background-color:linear-gradient(to right,#667eea,#764ba2); " +
            "-fx-text-fill:white; -fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-background-radius:20; -fx-padding:8 22; -fx-cursor:hand;");
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color:linear-gradient(to right,#7c8ef0,#8a5cb8); " +
            "-fx-text-fill:white; -fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-background-radius:20; -fx-padding:8 22; -fx-cursor:hand;"));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color:linear-gradient(to right,#667eea,#764ba2); " +
            "-fx-text-fill:white; -fx-font-size:13px; -fx-font-weight:bold; " +
            "-fx-background-radius:20; -fx-padding:8 22; -fx-cursor:hand;"));
        return btn;
    }

    private Circle circle(double radius, String color) {
        Circle c = new Circle(radius);
        c.setStyle("-fx-fill:" + color + ";");
        c.setEffect(new javafx.scene.effect.GaussianBlur(radius * 0.6));
        return c;
    }

    /**
     * Creates a circular clipped image of BGI.png for a corner,
     * with 7% opacity and a subtle glow.
     */
    private javafx.scene.Node cornerLogo(javafx.scene.image.Image img,
                                          Pos corner, double tx, double ty) {
        double radius = 60; // small circle — 120×120px total

        javafx.scene.image.ImageView iv = new javafx.scene.image.ImageView(img);
        iv.setFitWidth(radius * 2);
        iv.setFitHeight(radius * 2);
        iv.setPreserveRatio(false);
        iv.setSmooth(true);

        // Clip to circle
        Circle clip = new Circle(radius, radius, radius);
        iv.setClip(clip);

        // 7% opacity
        iv.setOpacity(0.07);

        StackPane.setAlignment(iv, corner);
        iv.setTranslateX(tx);
        iv.setTranslateY(ty);
        return iv;
    }

    // ── PARTICLE BACKGROUND ───────────────────────────────────────────
    private StackPane buildParticleLayer() {
        StackPane layer = new StackPane();
        layer.setStyle("-fx-background-color:transparent;");

        // Create 18 floating dots with random positions and animations
        String[] colors = {"rgba(102,126,234,0.4)", "rgba(118,75,162,0.35)",
                           "rgba(72,187,120,0.3)", "rgba(255,255,255,0.15)"};
        double[] sizes  = {4, 6, 3, 5, 4, 7, 3, 5, 6, 4, 3, 5, 4, 6, 3, 5, 4, 6};
        double[] xPos   = {50,150,300,500,700,900,100,400,600,800,200,350,550,750,120,450,650,850};
        double[] yPos   = {80,200,150,300,100,250,400,350,200,150,300,450,100,350,500,200,400,300};

        for (int i = 0; i < sizes.length; i++) {
            Circle dot = new Circle(sizes[i]);
            dot.setStyle("-fx-fill:" + colors[i % colors.length] + ";");
            dot.setTranslateX(xPos[i] - 500);
            dot.setTranslateY(yPos[i] - 300);

            // Float animation
            TranslateTransition tt = new TranslateTransition(
                Duration.seconds(4 + (i % 4)), dot);
            tt.setByY(-20 - (i % 15));
            tt.setAutoReverse(true);
            tt.setCycleCount(Animation.INDEFINITE);
            tt.setDelay(Duration.millis(i * 200));
            tt.play();

            // Fade animation
            FadeTransition ft = new FadeTransition(Duration.seconds(3 + (i % 3)), dot);
            ft.setFromValue(0.3); ft.setToValue(0.9);
            ft.setAutoReverse(true); ft.setCycleCount(Animation.INDEFINITE);
            ft.setDelay(Duration.millis(i * 150));
            ft.play();

            layer.getChildren().add(dot);
        }
        return layer;
    }

    // ── ANIMATIONS ────────────────────────────────────────────────────
    private void animateEntrance(HBox nav, VBox hero, VBox features) {
        // Nav slide down
        nav.setTranslateY(-60);
        nav.setOpacity(0);
        TranslateTransition navTT = new TranslateTransition(Duration.millis(500), nav);
        navTT.setToY(0);
        FadeTransition navFT = new FadeTransition(Duration.millis(500), nav);
        navFT.setToValue(1);
        navTT.play(); navFT.play();

        // Hero fade + rise
        hero.setOpacity(0);
        hero.setTranslateY(50);
        FadeTransition heroFT = new FadeTransition(Duration.millis(700), hero);
        heroFT.setDelay(Duration.millis(200));
        heroFT.setToValue(1);
        TranslateTransition heroTT = new TranslateTransition(Duration.millis(700), hero);
        heroTT.setDelay(Duration.millis(200));
        heroTT.setToY(0);
        heroFT.play(); heroTT.play();

        // Features fade in
        features.setOpacity(0);
        FadeTransition featFT = new FadeTransition(Duration.millis(600), features);
        featFT.setDelay(Duration.millis(600));
        featFT.setToValue(1);
        featFT.play();
    }

    // ── NAVIGATION ────────────────────────────────────────────────────
    private void navigateToLogin() {
        try {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            Parent root = loader.load();
            Scene scene = SceneUtil.light(root, getClass());

            FadeTransition fadeOut = new FadeTransition(Duration.millis(250), rootPane);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(e -> {
                stage.setScene(scene);
                stage.setWidth(1000);
                stage.setHeight(660);
                stage.setResizable(true);
                stage.setMinWidth(800);
                stage.setMinHeight(560);
                stage.centerOnScreen();
                root.setOpacity(0);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(400), root);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
