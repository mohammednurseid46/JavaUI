package com.lms.analytics.utils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Reusable DataGrid helper — adds sorting, filtering, pagination,
 * page-size selection to any TableView.
 */
public class DataGridHelper<T> {

    private final TableView<T> table;
    private final ObservableList<T> masterData;
    private FilteredList<T> filteredData;
    private SortedList<T> sortedData;

    private int currentPage = 0;
    private int pageSize    = 10;

    private Label pageInfoLabel;
    private Button prevBtn, nextBtn;
    private ComboBox<Integer> pageSizeCombo;

    // Sort state
    private String activeSortKey = null;
    private boolean sortAscending = true;
    private final Map<String, Comparator<T>> sortOptions = new LinkedHashMap<>();

    public DataGridHelper(TableView<T> table) {
        this.table      = table;
        this.masterData = FXCollections.observableArrayList();
        this.filteredData = new FilteredList<>(masterData, p -> true);
        this.sortedData   = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(table.comparatorProperty());
    }

    /** Load / reload all data */
    public void setData(List<T> data) {
        masterData.setAll(data);
        currentPage = 0;
        refreshPage();
    }

    /** Apply a text filter predicate */
    public void setFilter(Predicate<T> predicate) {
        filteredData.setPredicate(predicate);
        currentPage = 0;
        refreshPage();
    }

    /** Clear filter */
    public void clearFilter() {
        filteredData.setPredicate(p -> true);
        currentPage = 0;
        refreshPage();
    }

    /**
     * Register a named sort option.
     * Call this before buildSortButton() to add sort choices.
     * Example: grid.addSortOption("Name", Comparator.comparing(Student::getFullName));
     */
    public void addSortOption(String label, Comparator<T> comparator) {
        sortOptions.put(label, comparator);
    }

    /**
     * Build a "Sort ▾" button that shows a popup menu with all registered sort options.
     * Each option shows a ✓ checkmark when active, and clicking again toggles asc/desc.
     */
    public Button buildSortButton() {
        Button sortBtn = new Button("⇅  Sort");
        sortBtn.setStyle(
            "-fx-background-color:white; -fx-text-fill:#334155; " +
            "-fx-border-color:#cbd5e1; -fx-border-width:1.5; " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:6 14; -fx-font-size:12px; -fx-font-weight:bold; " +
            "-fx-cursor:hand;");
        sortBtn.setOnMouseEntered(e -> sortBtn.setStyle(
            "-fx-background-color:#f0f9ff; -fx-text-fill:#0284c7; " +
            "-fx-border-color:#38bdf8; -fx-border-width:1.5; " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:6 14; -fx-font-size:12px; -fx-font-weight:bold; " +
            "-fx-cursor:hand;"));
        sortBtn.setOnMouseExited(e -> sortBtn.setStyle(
            "-fx-background-color:white; -fx-text-fill:#334155; " +
            "-fx-border-color:#cbd5e1; -fx-border-width:1.5; " +
            "-fx-border-radius:8; -fx-background-radius:8; " +
            "-fx-padding:6 14; -fx-font-size:12px; -fx-font-weight:bold; " +
            "-fx-cursor:hand;"));

        sortBtn.setOnAction(e -> showSortPopup(sortBtn));
        return sortBtn;
    }

    private void showSortPopup(Button anchor) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle(
            "-fx-background-color:white; " +
            "-fx-background-radius:10; " +
            "-fx-border-color:#e2e8f0; -fx-border-radius:10; " +
            "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.15),12,0,0,4);");

        // Title item (non-clickable)
        MenuItem titleItem = new MenuItem("Sort by");
        titleItem.setStyle(
            "-fx-font-size:11px; -fx-text-fill:#94a3b8; " +
            "-fx-font-weight:bold; -fx-padding:6 16 4 16;");
        titleItem.setDisable(true);
        menu.getItems().add(titleItem);
        menu.getItems().add(new SeparatorMenuItem());

        for (Map.Entry<String, Comparator<T>> entry : sortOptions.entrySet()) {
            String key = entry.getKey();
            Comparator<T> comp = entry.getValue();

            // Build label: active item gets checkmark + direction arrow
            String label;
            if (key.equals(activeSortKey)) {
                label = "✓  " + key + "  " + (sortAscending ? "↑" : "↓");
            } else {
                label = "      " + key;
            }

            MenuItem item = new MenuItem(label);
            item.setStyle(
                "-fx-font-size:13px; -fx-padding:10 20; " +
                (key.equals(activeSortKey)
                    ? "-fx-text-fill:#0284c7; -fx-font-weight:bold;"
                    : "-fx-text-fill:#1e293b;"));

            item.setOnAction(ev -> {
                if (key.equals(activeSortKey)) {
                    // Toggle direction
                    sortAscending = !sortAscending;
                } else {
                    activeSortKey = key;
                    sortAscending = true;
                }
                applySort(comp);
                // Update button label to show active sort
                anchor.setText("⇅  " + key + " " + (sortAscending ? "↑" : "↓"));
            });

            menu.getItems().add(item);
        }

        // Clear sort option
        menu.getItems().add(new SeparatorMenuItem());
        MenuItem clearItem = new MenuItem("✕  Clear Sort");
        clearItem.setStyle("-fx-font-size:12px; -fx-text-fill:#94a3b8; -fx-padding:8 20;");
        clearItem.setOnAction(ev -> {
            activeSortKey = null;
            sortAscending = true;
            sortedData.comparatorProperty().bind(table.comparatorProperty());
            anchor.setText("⇅  Sort");
            refreshPage();
        });
        menu.getItems().add(clearItem);

        menu.show(anchor, javafx.geometry.Side.BOTTOM, 0, 4);
    }

    private void applySort(Comparator<T> comp) {
        Comparator<T> finalComp = sortAscending ? comp : comp.reversed();
        // Unbind from table comparator and apply our custom sort
        sortedData.comparatorProperty().unbind();
        sortedData.setComparator(finalComp);
        currentPage = 0;
        refreshPage();
    }

    /**
     * Build a filter bar that includes the sort button on the right.
     * Use this instead of buildFilterBar() when you want sort + search together.
     */
    public HBox buildFilterBarWithSort(String promptText,
            java.util.function.Function<String, Predicate<T>> predicateFactory) {

        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        TextField searchField = new TextField();
        searchField.setPromptText("🔍  " + promptText);
        searchField.setPrefWidth(280);
        searchField.setStyle("-fx-background-radius:20; -fx-border-radius:20; -fx-padding:6 14;");
        searchField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) clearFilter();
            else setFilter(predicateFactory.apply(val.toLowerCase()));
        });

        Button clearBtn = new Button("✕");
        clearBtn.setStyle(
            "-fx-background-color:transparent; -fx-text-fill:#94a3b8; " +
            "-fx-border-color:transparent; -fx-padding:5 8; " +
            "-fx-cursor:hand; -fx-font-size:12px;");
        clearBtn.setOnAction(e -> { searchField.clear(); clearFilter(); });

        Label countLbl = new Label();
        countLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        filteredData.addListener((javafx.collections.ListChangeListener<T>) c ->
            countLbl.setText(filteredData.size() + " records"));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        Button sortBtn = buildSortButton();

        bar.getChildren().addAll(searchField, clearBtn, sp, countLbl, sortBtn);
        return bar;
    }

    /** Build the bottom pagination bar — call once and add to your layout */
    public HBox buildPaginationBar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle("-fx-background-color:white; -fx-border-color:#e2e8f0; -fx-border-width:1 0 0 0;");

        // Page size selector
        Label pageSizeLbl = new Label("Rows per page:");
        pageSizeLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");

        pageSizeCombo = new ComboBox<>();
        ObservableList<Integer> sizes = FXCollections.observableArrayList(5, 10, 20, 50, 100);
        pageSizeCombo.setItems(sizes);
        pageSizeCombo.getSelectionModel().select(Integer.valueOf(pageSize));
        pageSizeCombo.setPrefWidth(75);
        pageSizeCombo.setStyle(
            "-fx-font-size:12px; -fx-background-color:white; " +
            "-fx-border-color:#cbd5e1; -fx-border-radius:6; -fx-background-radius:6;");
        pageSizeCombo.setOnAction(e -> {
            Integer selected = pageSizeCombo.getValue();
            if (selected != null) {
                pageSize    = selected;
                currentPage = 0;
                refreshPage();
            }
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        prevBtn = new Button("◀  Prev");
        prevBtn.getStyleClass().addAll("button", "btn-primary");
        prevBtn.setStyle("-fx-padding:5 14; -fx-font-size:12px;");
        prevBtn.setOnAction(e -> { if (currentPage > 0) { currentPage--; refreshPage(); } });

        pageInfoLabel = new Label();
        pageInfoLabel.setStyle(
            "-fx-font-size:12px; -fx-font-weight:bold; -fx-text-fill:#334155;");

        nextBtn = new Button("Next  ▶");
        nextBtn.getStyleClass().addAll("button", "btn-primary");
        nextBtn.setStyle("-fx-padding:5 14; -fx-font-size:12px;");
        nextBtn.setOnAction(e -> {
            if ((currentPage + 1) * pageSize < filteredData.size()) {
                currentPage++;
                refreshPage();
            }
        });

        bar.getChildren().addAll(pageSizeLbl, pageSizeCombo, spacer,
                                  prevBtn, pageInfoLabel, nextBtn);
        return bar;
    }

    /** Build a search/filter bar with a text field (no sort button) */
    public HBox buildFilterBar(String promptText, java.util.function.Function<String, Predicate<T>> predicateFactory) {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(8, 16, 8, 16));
        bar.setStyle("-fx-background-color:#f8fafc; -fx-border-color:#e2e8f0; -fx-border-width:0 0 1 0;");

        TextField searchField = new TextField();
        searchField.setPromptText("🔍  " + promptText);
        searchField.setPrefWidth(300);
        searchField.setStyle("-fx-background-radius:20; -fx-border-radius:20; -fx-padding:6 14;");
        searchField.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) clearFilter();
            else setFilter(predicateFactory.apply(val.toLowerCase()));
        });

        Button clearBtn = new Button("✕ Clear");
        clearBtn.setStyle("-fx-background-color:transparent; -fx-text-fill:#38bdf8; " +
            "-fx-border-color:#38bdf8; -fx-border-radius:6; -fx-background-radius:6; " +
            "-fx-padding:5 10; -fx-cursor:hand; -fx-font-size:12px;");
        clearBtn.setOnAction(e -> { searchField.clear(); clearFilter(); });

        Label countLbl = new Label();
        countLbl.setStyle("-fx-font-size:12px; -fx-text-fill:#64748b;");
        filteredData.addListener((javafx.collections.ListChangeListener<T>) c ->
            countLbl.setText(filteredData.size() + " records"));

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        bar.getChildren().addAll(searchField, clearBtn, sp, countLbl);
        return bar;
    }

    private void refreshPage() {
        int total = filteredData.size();
        int from  = currentPage * pageSize;
        int to    = Math.min(from + pageSize, total);

        ObservableList<T> page = FXCollections.observableArrayList(
            sortedData.subList(Math.min(from, total), to));
        table.setItems(page);

        int totalPages = total == 0 ? 1 : (int) Math.ceil((double) total / pageSize);
        if (pageInfoLabel != null)
            pageInfoLabel.setText("Page " + (currentPage + 1) + " of " + totalPages
                + "  (" + total + " total)");
        if (prevBtn != null) prevBtn.setDisable(currentPage == 0);
        if (nextBtn != null) nextBtn.setDisable(to >= total);
    }

    /** Apply an external comparator directly (for FXML-based controllers) */
    public void applyExternalSort(java.util.Comparator<T> comp) {
        sortedData.comparatorProperty().unbind();
        sortedData.setComparator(comp);
        currentPage = 0;
        refreshPage();
    }

    public ObservableList<T> getMasterData() { return masterData; }
    public int getTotalFiltered() { return filteredData.size(); }
}
