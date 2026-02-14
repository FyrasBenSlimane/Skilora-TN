package com.skilora.controller.formation;

import com.skilora.framework.components.*;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.model.entity.formation.Training;
import com.skilora.model.enums.TrainingCategory;
import com.skilora.service.formation.TrainingService;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.scene.shape.SVGPath;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Side;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * AdminTrainingController
 * 
 * Handles CRUD operations for trainings in the admin panel.
 * Follows MVC pattern - Controller uses Service layer.
 */
public class AdminTrainingController {

    private static final Logger logger = LoggerFactory.getLogger(AdminTrainingController.class);

    @FXML
    private Button addTrainingBtn;
    @FXML
    private TLTextField searchField;
    @FXML
    private Button filterBtn;
    @FXML
    private TableView<Training> trainingsTable;
    @FXML
    private Label rowsInfo;
    @FXML
    private Pagination pagination;

    private TrainingService trainingService;
    private java.util.function.Consumer<Training> onShowTrainingForm;
    private Runnable onRefreshView;
    private FilteredList<Training> filteredData;

    private static final int ROWS_PER_PAGE = 10;
    private ObservableList<Training> allData;
    private TrainingCategory selectedCategoryFilter = null;

    @FXML
    public void initialize() {
        // This is called automatically by FXML loader
        // But we need initializeContext to be called to set up services
        logger.debug("AdminTrainingController FXML initialize() called");
    }

    public void initializeContext(TrainingService trainingService,
            java.util.function.Consumer<Training> onShowTrainingForm,
            Runnable onRefreshView) {
        this.trainingService = trainingService;
        this.onShowTrainingForm = onShowTrainingForm;
        this.onRefreshView = onRefreshView;

        setupButtons();
        setupTable();

        // Attach search listener
        if (searchField != null && searchField.getControl() != null) {
            searchField.getControl().textProperty().addListener((observable, oldValue, newValue) -> {
                if (filteredData != null) {
                    applyFilters();
                    updatePagination();
                }
            });
        }

        loadTrainings();
    }

    private void setupButtons() {
        // Add Training Button with Icon and Text
        if (addTrainingBtn != null) {
            SVGPath plusIcon = new SVGPath();
            plusIcon.setContent("M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z");
            plusIcon.getStyleClass().add("svg-path");
            addTrainingBtn.setGraphic(plusIcon);
            addTrainingBtn.setText("Ajouter une Formation");
            addTrainingBtn.setOnAction(e -> {
                logger.info("Add training button clicked");
                if (onShowTrainingForm != null) {
                    logger.info("Calling onShowTrainingForm callback");
                    onShowTrainingForm.accept(null);
                } else {
                    logger.warn("onShowTrainingForm callback is null!");
                }
            });
        } else {
            logger.error("addTrainingBtn is null! Check FXML binding.");
        }

        // Search Field Setup
        if (searchField != null) {
            searchField.setPromptText("Rechercher par titre ou description...");
        }

        // Filter Button with Icon
        SVGPath filterIcon = new SVGPath();
        filterIcon.setContent("M10 18h4v-2h-4v2zM3 6v2h18V6H3zm3 7h12v-2H6v2z");
        filterIcon.getStyleClass().add("svg-path");
        filterIcon.setScaleX(0.8);
        filterIcon.setScaleY(0.8);
        filterBtn.setGraphic(filterIcon);
        filterBtn.setOnAction(e -> showFilterMenu());
    }

    private void setupTable() {
        trainingsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        trainingsTable.setFixedCellSize(50);

        // ID Column
        TableColumn<Training, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);
        idCol.setMaxWidth(80);
        idCol.setMinWidth(40);
        idCol.getStyleClass().add("id-column");
        idCol.setStyle("-fx-alignment: CENTER;");
        idCol.setCellFactory(col -> new TableCell<>() {
            {
                setAlignment(Pos.CENTER);
                setStyle("-fx-text-fill: -fx-foreground;");
            }

            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.valueOf(item));
                }
            }
        });

        // Title Column
        TableColumn<Training, String> titleCol = new TableColumn<>("Titre");
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        titleCol.setPrefWidth(200);

        // Category Column
        TableColumn<Training, TrainingCategory> categoryCol = new TableColumn<>("Catégorie");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setStyle("-fx-alignment: CENTER-LEFT;");
        categoryCol.setCellFactory(col -> new TableCell<>() {
            private final TLBadge badge = new TLBadge("", TLBadge.Variant.OUTLINE);
            private final HBox box = new HBox(badge);
            {
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(TrainingCategory category, boolean empty) {
                super.updateItem(category, empty);
                if (empty || category == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.setText(category.getDisplayName());
                    badge.setVariant(TLBadge.Variant.SECONDARY);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // Level Column
        TableColumn<Training, String> levelCol = new TableColumn<>("Niveau");
        levelCol.setCellValueFactory(cellData -> {
            Training t = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                t.getLevel() != null ? t.getLevel().getDisplayName() : "");
        });
        levelCol.setPrefWidth(100);

        // Cost Column
        TableColumn<Training, String> costCol = new TableColumn<>("Coût");
        costCol.setCellValueFactory(cellData -> {
            Training t = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(t.getFormattedCost());
        });
        costCol.setPrefWidth(100);
        costCol.setStyle("-fx-alignment: CENTER-RIGHT;");

        // Duration Column
        TableColumn<Training, Integer> durationCol = new TableColumn<>("Durée (h)");
        durationCol.setCellValueFactory(new PropertyValueFactory<>("duration"));
        durationCol.setPrefWidth(80);
        durationCol.setStyle("-fx-alignment: CENTER;");

        // Actions Column
        TableColumn<Training, Void> actionsCol = new TableColumn<>("");
        actionsCol.setSortable(false);
        actionsCol.setPrefWidth(50);
        actionsCol.setMaxWidth(50);
        actionsCol.setResizable(false);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final TLButton moreBtn = new TLButton("", ButtonVariant.GHOST);

            {
                SVGPath moreIcon = new SVGPath();
                moreIcon.setContent(
                        "M6 10c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm12 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm-6 0c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z");
                moreIcon.getStyleClass().add("svg-path");
                moreIcon.setScaleX(0.9);
                moreIcon.setScaleY(0.9);
                moreBtn.setGraphic(moreIcon);
                moreBtn.getStyleClass().add("btn-icon");

                TLDropdownMenu actionsMenu = new TLDropdownMenu();

                MenuItem editItem = actionsMenu.addItem("Modifier");
                editItem.setOnAction(e -> {
                    Training t = getTableView().getItems().get(getIndex());
                    if (onShowTrainingForm != null) {
                        onShowTrainingForm.accept(t);
                    }
                });

                MenuItem deleteItem = actionsMenu.addItem("Supprimer");
                deleteItem.getStyleClass().add("text-destructive");
                deleteItem.setOnAction(e -> {
                    Training t = getTableView().getItems().get(getIndex());
                    if (t == null) return;
                    
                    DialogUtils.showConfirmation(
                            "Confirmer la suppression",
                            "Êtes-vous sûr de vouloir supprimer la formation \"" + t.getTitle() + "\" ?\n\n" +
                            "Cette action supprimera également toutes les inscriptions et leçons associées.")
                            .ifPresent(response -> {
                                if (response == ButtonType.OK) {
                                    Thread deleteThread = new Thread(() -> {
                                        try {
                                            logger.info("Deleting training with ID: {}", t.getId());
                                            boolean deleted = trainingService.deleteTraining(t.getId());
                                            
                                            javafx.application.Platform.runLater(() -> {
                                                if (deleted) {
                                                    DialogUtils.showSuccess("Succès", 
                                                        "La formation \"" + t.getTitle() + "\" a été supprimée avec succès.");
                                                    
                                                    // Reload trainings to refresh the table
                                                    loadTrainings();
                                                    
                                                    // Also call refresh callback if provided
                                                    if (onRefreshView != null) {
                                                        onRefreshView.run();
                                                    }
                                                } else {
                                                    DialogUtils.showError("Erreur", 
                                                        "La formation n'a pas pu être supprimée. Elle n'existe peut-être plus.");
                                                    // Still reload to refresh the table
                                                    loadTrainings();
                                                }
                                            });
                                        } catch (Exception ex) {
                                            logger.error("Error deleting training", ex);
                                            javafx.application.Platform.runLater(() -> {
                                                DialogUtils.showError("Erreur", 
                                                    "Impossible de supprimer la formation: " + ex.getMessage());
                                            });
                                        }
                                    }, "DeleteTrainingThread");
                                    deleteThread.setDaemon(true);
                                    deleteThread.start();
                                }
                            });
                });

                moreBtn.setOnAction(e -> actionsMenu.showWithinWindow(moreBtn, Side.BOTTOM, 0));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(moreBtn);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        @SuppressWarnings("unchecked")
        TableColumn<Training, ?>[] columns = new TableColumn[] { 
            idCol, titleCol, categoryCol, levelCol, costCol, durationCol, actionsCol 
        };
        trainingsTable.getColumns().addAll(columns);
    }

    private void loadTrainings() {
        if (trainingService == null) {
            logger.error("TrainingService is null, cannot load trainings");
            return;
        }
        
        rowsInfo.setText("Chargement...");
        trainingsTable.setPlaceholder(new Label("Chargement des formations..."));

        Task<ObservableList<Training>> loadTask = new Task<>() {
            @Override
            protected ObservableList<Training> call() throws Exception {
                logger.debug("Loading trainings from service...");
                List<Training> trainings = trainingService.getAllTrainings();
                logger.debug("Loaded {} trainings", trainings.size());
                return FXCollections.observableArrayList(trainings);
            }
        };

        loadTask.setOnSucceeded(e -> {
            allData = loadTask.getValue();
            filteredData = new FilteredList<>(allData, p -> true);

            // Re-apply current filters
            if (searchField != null && searchField.getControl() != null) {
                applyFilters();
            } else {
                setupPagination();
                updateTableView(0);
            }
            
            trainingsTable.setPlaceholder(new Label("Aucune formation trouvée"));
            logger.debug("Trainings loaded successfully, table updated");
        });

        loadTask.setOnFailed(e -> {
            rowsInfo.setText("Erreur");
            trainingsTable.setPlaceholder(new Label("Erreur lors du chargement"));
            logger.error("Error loading trainings", loadTask.getException());
            DialogUtils.showError("Erreur", 
                "Impossible de charger les formations: " + loadTask.getException().getMessage());
        });

        new Thread(loadTask).start();
    }
    
    /**
     * Public method to refresh the trainings list
     * Can be called from outside to force a reload
     */
    public void refresh() {
        logger.debug("Refresh requested for AdminTrainingController");
        loadTrainings();
    }

    private void applyFilters() {
        filteredData.setPredicate(training -> {
            String searchText = searchField.getControl().getText();

            // Apply search filter
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                boolean matchesSearch = (training.getTitle() != null && 
                                       training.getTitle().toLowerCase().contains(lowerCaseFilter)) ||
                                      (training.getDescription() != null && 
                                       training.getDescription().toLowerCase().contains(lowerCaseFilter));
                if (!matchesSearch)
                    return false;
            }

            // Apply category filter
            if (selectedCategoryFilter != null && training.getCategory() != selectedCategoryFilter) {
                return false;
            }

            return true;
        });
        updatePagination();
    }

    private void setupPagination() {
        int pageCount = (int) Math.ceil((double) filteredData.size() / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);

        pagination.currentPageIndexProperty().addListener((obs, oldIndex, newIndex) -> {
            updateTableView(newIndex.intValue());
        });

        updateRowInfo();
    }

    private void updatePagination() {
        int pageCount = (int) Math.ceil((double) filteredData.size() / ROWS_PER_PAGE);
        pagination.setPageCount(pageCount > 0 ? pageCount : 1);
        pagination.setCurrentPageIndex(0);
        updateTableView(0);
        updateRowInfo();
    }

    private void updateTableView(int pageIndex) {
        int fromIndex = pageIndex * ROWS_PER_PAGE;
        int toIndex = Math.min(fromIndex + ROWS_PER_PAGE, filteredData.size());

        int minIndex = Math.min(toIndex, filteredData.size());
        ObservableList<Training> pageData = FXCollections.observableArrayList(
                filteredData.subList(Math.min(fromIndex, minIndex), minIndex));

        trainingsTable.setItems(pageData);
        updateRowInfo();
    }

    private void updateRowInfo() {
        int currentPage = pagination.getCurrentPageIndex() + 1;
        int totalPages = pagination.getPageCount();
        int totalItems = filteredData.size();
        int fromItem = totalItems > 0 ? (pagination.getCurrentPageIndex() * ROWS_PER_PAGE) + 1 : 0;
        int toItem = Math.min(fromItem + ROWS_PER_PAGE - 1, totalItems);

        if (totalItems > 0) {
            rowsInfo.setText(String.format("Affichage %d-%d sur %d (Page %d/%d)", 
                fromItem, toItem, totalItems, currentPage, totalPages));
        } else {
            rowsInfo.setText("Aucune formation");
        }
    }

    private void showFilterMenu() {
        TLDropdownMenu filterMenu = new TLDropdownMenu();

        MenuItem allCategoriesItem = filterMenu.addItem("Toutes les catégories");
        allCategoriesItem.setOnAction(e -> {
            selectedCategoryFilter = null;
            applyFilters();
        });

        filterMenu.getItems().add(new SeparatorMenuItem());

        for (TrainingCategory category : TrainingCategory.values()) {
            MenuItem categoryItem = filterMenu.addItem(category.getDisplayName());
            categoryItem.setOnAction(e -> {
                selectedCategoryFilter = category;
                applyFilters();
            });
        }

        filterMenu.showWithinWindow(filterBtn, Side.BOTTOM, 8);
    }
}
