package com.skilora.user.controller;

import com.skilora.framework.components.*;
import com.skilora.framework.components.TLButton.ButtonVariant;
import com.skilora.user.entity.User;
import com.skilora.user.entity.RoleUpgradeRequest;
import com.skilora.user.enums.Role;
import com.skilora.user.service.RoleUpgradeService;
import com.skilora.user.service.UserService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.shape.SVGPath;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Side;
import java.util.List;

public class UsersController {

    private static final Logger logger = LoggerFactory.getLogger(UsersController.class);

    @FXML
    private TLButton addUserBtn;
    @FXML
    private TLButton roleRequestsBtn;
    @FXML
    private TLTextField searchField;
    @FXML
    private TLButton filterBtn;
    @FXML
    private TLButton columnsBtn;
    @FXML
    private TableView<User> usersTable;
    @FXML
    private Label rowsInfo;
    @FXML
    private Pagination pagination;
    @FXML
    private Label titleLabel;
    @FXML
    private Label subtitleLabel;

    private UserService userService;
    private User currentAdmin;
    private java.util.function.Consumer<User> onShowUserForm;
    private Runnable onRefreshView;
    private java.util.function.Function<User, String> initialsGenerator;
    private FilteredList<User> filteredData;
    private Role selectedRoleFilter = null;

    private static final int ROWS_PER_PAGE = 10;
    private ObservableList<User> allData;

    public void initializeContext(UserService userService,
            User currentAdmin,
            java.util.function.Consumer<User> onShowUserForm,
            Runnable onRefreshView,
            java.util.function.Function<User, String> initialsGenerator) {
        this.userService = userService;
        this.currentAdmin = currentAdmin;
        this.onShowUserForm = onShowUserForm;
        this.onRefreshView = onRefreshView;
        this.initialsGenerator = initialsGenerator;

        setupButtons();
        setupTable();

        // Attach search listener once (not inside loadTask callback to prevent stacking)
        searchField.getControl().textProperty().addListener((observable, oldValue, newValue) -> {
            if (filteredData != null) {
                applyFilters();
                updatePagination();
            }
        });

        loadUsers();
    }

    private void setupButtons() {
        // I18n overrides for header labels
        if (titleLabel != null) {
            titleLabel.setText(I18n.get("users.title"));
        }
        if (subtitleLabel != null) {
            subtitleLabel.setText(I18n.get("users.subtitle"));
        }

        if (roleRequestsBtn != null) {
            SVGPath shield = new SVGPath();
            shield.setContent(SvgIcons.SHIELD_CHECK);
            shield.getStyleClass().add("svg-path");
            shield.setScaleX(0.8);
            shield.setScaleY(0.8);
            roleRequestsBtn.setGraphic(shield);
            roleRequestsBtn.setText(I18n.get("users.role_requests"));
            roleRequestsBtn.setOnAction(e -> showRoleRequestsDialog());
        }

        // Add User Button with Icon
        SVGPath plusIcon = new SVGPath();
        plusIcon.setContent(SvgIcons.PLUS);
        plusIcon.getStyleClass().add("svg-path");
        addUserBtn.setGraphic(plusIcon);
        addUserBtn.setText(I18n.get("users.add"));
        addUserBtn.setOnAction(e -> {
            if (onShowUserForm != null) {
                onShowUserForm.accept(null);
            }
        });

        // Search Field Setup
        searchField.setPromptText(I18n.get("users.search"));

        // Filter Button with Icon
        SVGPath filterIcon = new SVGPath();
        filterIcon.setContent(SvgIcons.FILTER);
        filterIcon.getStyleClass().add("svg-path");
        filterIcon.setScaleX(0.8);
        filterIcon.setScaleY(0.8);
        filterBtn.setGraphic(filterIcon);
        filterBtn.setText(I18n.get("users.filters"));
        filterBtn.setOnAction(e -> showFilterMenu());

        // Columns Button with Icon
        SVGPath colsIcon = new SVGPath();
        colsIcon.setContent(SvgIcons.COLUMNS_3);
        colsIcon.getStyleClass().add("svg-path");
        colsIcon.setScaleX(0.8);
        colsIcon.setScaleY(0.8);
        columnsBtn.setGraphic(colsIcon);
        columnsBtn.setText(I18n.get("users.columns"));
    }

    private void showRoleRequestsDialog() {
        if (currentAdmin == null || currentAdmin.getRole() != Role.ADMIN) {
            return;
        }
        TLDialog<ButtonType> dialog = new TLDialog<>();
        if (usersTable != null && usersTable.getScene() != null) {
            dialog.initOwner(usersTable.getScene().getWindow());
        }
        dialog.setDialogTitle(I18n.get("users.role_requests"));
        dialog.setDescription(I18n.get("users.role_requests.desc"));

        VBox body = new VBox(12);
        body.setPadding(new Insets(4));
        TLLoadingState loading = new TLLoadingState(I18n.get("common.loading"));
        body.getChildren().add(loading);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(420);
        scroll.setStyle("-fx-background-color: transparent;");

        dialog.setContent(scroll);
        dialog.addButton(ButtonType.CLOSE);

        Task<List<RoleUpgradeRequest>> task = new Task<>() {
            @Override
            protected List<RoleUpgradeRequest> call() {
                return RoleUpgradeService.getInstance().findPendingRequests();
            }
        };
        task.setOnSucceeded(e -> {
            List<RoleUpgradeRequest> list = task.getValue();
            javafx.application.Platform.runLater(() -> {
                body.getChildren().clear();
                if (list == null || list.isEmpty()) {
                    body.getChildren().add(new TLEmptyState(SvgIcons.SHIELD, I18n.get("users.role_requests.empty"),
                            I18n.get("users.role_requests.empty.desc")));
                    return;
                }
                for (RoleUpgradeRequest r : list) {
                    body.getChildren().add(buildRoleRequestCard(r, dialog));
                }
            });
        });
        task.setOnFailed(e -> javafx.application.Platform.runLater(() -> {
            body.getChildren().clear();
            body.getChildren().add(new TLAlert(TLAlert.Variant.DESTRUCTIVE, I18n.get("common.error"),
                    I18n.get("users.role_requests.load_failed")));
        }));
        AppThreadPool.execute(task);

        dialog.showAndWait();
    }

    private TLCard buildRoleRequestCard(RoleUpgradeRequest req, TLDialog<ButtonType> parentDialog) {
        TLCard card = new TLCard();
        VBox content = new VBox(10);

        String name = req.getFullName() != null ? req.getFullName() : ("#" + req.getUserId());
        Label title = new Label(name);
        title.getStyleClass().add("h4");

        Label meta = new Label(req.getCurrentRole() + " → " + req.getRequestedRole());
        meta.getStyleClass().add("text-muted");

        content.getChildren().addAll(title, meta);

        if (req.getJustification() != null && !req.getJustification().isBlank()) {
            Label just = new Label(req.getJustification());
            just.setWrapText(true);
            just.getStyleClass().add("text-sm");
            content.getChildren().add(just);
        }

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        TLTextField notes = new TLTextField("", "");
        notes.setLabel(I18n.get("users.role_requests.admin_notes"));
        notes.setPromptText(I18n.get("users.role_requests.notes"));
        notes.setPrefWidth(340);

        TLButton reject = new TLButton(I18n.get("common.reject"), ButtonVariant.GHOST);
        reject.getStyleClass().add("text-destructive");
        reject.setOnAction(e -> {
            // Block admin from rejecting their own role upgrade request
            if (currentAdmin != null && req.getUserId() == currentAdmin.getId()) {
                if (reject.getScene() != null) {
                    com.skilora.framework.components.TLToast.error(
                        reject.getScene(), I18n.get("common.error"), I18n.get("admin.self_action.reject_own"));
                }
                return;
            }
            boolean ok = RoleUpgradeService.getInstance().reject(req.getId(), currentAdmin.getId(), notes.getText());
            if (ok) {
                parentDialog.close();
                loadUsers();
            }
        });

        TLButton approve = new TLButton(I18n.get("common.approve"), ButtonVariant.PRIMARY);
        approve.setOnAction(e -> {
            // Block admin from approving their own role upgrade request
            if (currentAdmin != null && req.getUserId() == currentAdmin.getId()) {
                if (approve.getScene() != null) {
                    com.skilora.framework.components.TLToast.error(
                        approve.getScene(), I18n.get("common.error"), I18n.get("admin.self_action.approve_own"));
                }
                return;
            }
            boolean ok = RoleUpgradeService.getInstance().approve(req.getId(), currentAdmin.getId(), notes.getText());
            if (ok) {
                parentDialog.close();
                loadUsers();
            }
        });

        actions.getChildren().addAll(reject, approve);

        content.getChildren().addAll(notes, actions);
        card.setBody(content);
        return card;
    }

    private void setupTable() {
        usersTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        usersTable.setFixedCellSize(50);

        // 1. SELECT Column
        TableColumn<User, Boolean> selectCol = new TableColumn<>();
        selectCol.getStyleClass().add("check-column");

        CheckBox headerCb = new CheckBox();
        headerCb.getStyleClass().add("checkbox-input");
        StackPane headerCbPane = new StackPane(headerCb);
        headerCbPane.setAlignment(Pos.CENTER);
        selectCol.setGraphic(headerCbPane);

        selectCol.setSortable(false);
        selectCol.setPrefWidth(48);
        selectCol.setMaxWidth(48);
        selectCol.setResizable(false);
        selectCol.setStyle("-fx-alignment: CENTER;");
        selectCol.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();
            {
                checkBox.getStyleClass().add("checkbox-input");
                setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Boolean item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(checkBox);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        // 2. ID Column
        TableColumn<User, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(60);
        idCol.setMaxWidth(80);
        idCol.setMinWidth(40);
        idCol.getStyleClass().add("id-column");
        idCol.setStyle("-fx-alignment: CENTER;");
        idCol.setCellFactory(col -> new TableCell<>() {
            {
                setAlignment(Pos.CENTER);
                getStyleClass().add("text-primary");
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

        // 3. USERNAME Column (With Avatar)
        TableColumn<User, User> userCol = new TableColumn<>(I18n.get("users.col.user"));
        userCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleObjectProperty<>(cellData.getValue()));
        userCol.setCellFactory(col -> new TableCell<>() {
            private final HBox box = new HBox(8);
            private final TLAvatar avatar = new TLAvatar();
            private final Label nameLabel = new Label();

            {
                box.setAlignment(Pos.CENTER_LEFT);
                avatar.setPrefSize(32, 32);
                avatar.setMinSize(32, 32);
                avatar.setMaxSize(32, 32);
                nameLabel.getStyleClass().addAll("font-bold", "text-primary");
                box.getChildren().addAll(avatar, nameLabel);
            }

            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    nameLabel.setText(user.getUsername());

                    String initials = initialsGenerator != null
                            ? initialsGenerator.apply(user)
                            : user.getUsername().substring(0, Math.min(2, user.getUsername().length())).toUpperCase();
                    avatar.setFallback(initials);

                    javafx.scene.image.Image img = com.skilora.utils.ImageUtils.loadProfileImage(
                            user.getPhotoUrl(), 32, 32);
                    avatar.setImage(img);

                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // 4. FULL NAME Column
        TableColumn<User, String> nameCol = new TableColumn<>(I18n.get("users.col.full_name"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));

        // 5. ROLE Column
        TableColumn<User, Role> roleCol = new TableColumn<>(I18n.get("users.col.role"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        roleCol.setStyle("-fx-alignment: CENTER-LEFT;");
        roleCol.setCellFactory(col -> new TableCell<>() {
            private final TLBadge badge = new TLBadge("", TLBadge.Variant.OUTLINE);
            private final HBox box = new HBox(badge);
            {
                box.setAlignment(Pos.CENTER_LEFT);
            }

            @Override
            protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    TLBadge.Variant variant;
                    switch (role) {
                        case ADMIN:
                            variant = TLBadge.Variant.DESTRUCTIVE;
                            break;
                        case EMPLOYER:
                            variant = TLBadge.Variant.SECONDARY;
                            break;
                        default:
                            variant = TLBadge.Variant.OUTLINE;
                            break;
                    }
                    badge.setText(role.getDisplayName());
                    badge.setVariant(variant);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        // 6. ACTIONS Column
        TableColumn<User, Void> actionsCol = new TableColumn<>("");
        actionsCol.setSortable(false);
        actionsCol.setPrefWidth(50);
        actionsCol.setMaxWidth(50);
        actionsCol.setResizable(false);
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final TLButton moreBtn = new TLButton("", ButtonVariant.GHOST);

            {
                SVGPath moreIcon = new SVGPath();
                moreIcon.setContent(SvgIcons.ELLIPSIS);
                moreIcon.getStyleClass().add("svg-path");
                moreIcon.setScaleX(0.9);
                moreIcon.setScaleY(0.9);
                moreBtn.setGraphic(moreIcon);
                moreBtn.setTooltip(new Tooltip("More actions"));
                moreBtn.getStyleClass().add("btn-icon");

                TLDropdownMenu actionsMenu = new TLDropdownMenu();

                MenuItem copyIdItem = actionsMenu.addItem(I18n.get("users.copy_id"));
                copyIdItem.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                    content.putString(String.valueOf(u.getId()));
                    javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                });

                actionsMenu.getItems().add(new SeparatorMenuItem());

                MenuItem editItem = actionsMenu.addItem(I18n.get("users.edit"));
                editItem.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    // Block admin from editing themselves (role change risk)
                    if (currentAdmin != null && u.getId() == currentAdmin.getId()) {
                        com.skilora.framework.components.TLToast.error(
                            moreBtn.getScene(), I18n.get("common.error"), I18n.get("admin.self_action.edit"));
                        return;
                    }
                    if (onShowUserForm != null) {
                        onShowUserForm.accept(u);
                    }
                });

                MenuItem deleteItem = actionsMenu.addItem(I18n.get("users.delete"));
                deleteItem.getStyleClass().add("text-destructive");
                deleteItem.setOnAction(e -> {
                    User u = getTableView().getItems().get(getIndex());
                    // Block admin from deleting themselves
                    if (currentAdmin != null && u.getId() == currentAdmin.getId()) {
                        com.skilora.framework.components.TLToast.error(
                            moreBtn.getScene(), I18n.get("common.error"), I18n.get("admin.self_action.delete"));
                        return;
                    }
                    // Confirmation dialog via DialogUtils
                    DialogUtils.showConfirmation(
                            I18n.get("users.delete.confirm_title"),
                            I18n.get("users.delete.confirm_msg", u.getUsername())).ifPresent(response -> {
                                if (response == ButtonType.OK) {
                                    AppThreadPool.execute(() -> {
                                        try {
                                            userService.deleteUser(u.getId());
                                            javafx.application.Platform.runLater(() -> {
                                                if (onRefreshView != null) {
                                                    onRefreshView.run();
                                                }
                                            });
                                        } catch (Exception ex) {
                                            logger.error("Failed to delete user", ex);
                                            javafx.application.Platform.runLater(() -> {
                                                com.skilora.framework.components.TLToast.error(
                                                    moreBtn.getScene(), "Error", "Failed to delete user: " + ex.getMessage());
                                            });
                                        }
                                    });
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
        TableColumn<User, ?>[] columns = new TableColumn[] { selectCol, idCol, userCol, nameCol, roleCol, actionsCol };
        usersTable.getColumns().addAll(columns);

        // Dynamic Columns Menu
        TLDropdownMenu columnsMenu = new TLDropdownMenu();

        java.util.function.BiConsumer<String, TableColumn<User, ?>> addColItem = (name, col) -> {
            CheckMenuItem item = new CheckMenuItem(name);
            item.setSelected(true);
            item.selectedProperty().bindBidirectional(col.visibleProperty());
            columnsMenu.getItems().add(item);
        };

        addColItem.accept("ID", idCol);
        addColItem.accept(I18n.get("users.col.user"), userCol);
        addColItem.accept(I18n.get("users.col.full_name"), nameCol);
        addColItem.accept(I18n.get("users.col.role"), roleCol);

        columnsBtn.setOnAction(e -> columnsMenu.showWithinWindow(columnsBtn, Side.BOTTOM, 8));
    }

    private void loadUsers() {
        rowsInfo.setText(I18n.get("users.loading.title"));
        usersTable.setPlaceholder(new Label(I18n.get("users.loading.subtitle")));

        Task<ObservableList<User>> loadTask = new Task<>() {
            @Override
            protected ObservableList<User> call() throws Exception {
                return FXCollections.observableArrayList(userService.getAllUsers());
            }
        };

        loadTask.setOnSucceeded(e -> {
            allData = loadTask.getValue();
            filteredData = new FilteredList<>(allData, p -> true);

            setupPagination();
            updateTableView(0);
            usersTable.setPlaceholder(new Label(I18n.get("users.no_users")));
        });

        loadTask.setOnFailed(e -> {
            rowsInfo.setText(I18n.get("users.error"));
            usersTable.setPlaceholder(new Label(I18n.get("users.error.detail")));
        });

        AppThreadPool.execute(loadTask);
    }

    private void applyFilters() {
        filteredData.setPredicate(user -> {
            String searchText = searchField.getControl().getText();

            // Apply search filter
            if (searchText != null && !searchText.isEmpty()) {
                String lowerCaseFilter = searchText.toLowerCase();
                boolean matchesSearch = user.getUsername().toLowerCase().contains(lowerCaseFilter) ||
                        (user.getFullName() != null && user.getFullName().toLowerCase().contains(lowerCaseFilter));
                if (!matchesSearch)
                    return false;
            }

            // Apply role filter
            if (selectedRoleFilter != null && user.getRole() != selectedRoleFilter) {
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
        ObservableList<User> pageData = FXCollections.observableArrayList(
                filteredData.subList(Math.min(fromIndex, minIndex), minIndex));

        usersTable.setItems(pageData);
        updateRowInfo();
    }

    private void updateRowInfo() {
        int currentPage = pagination.getCurrentPageIndex() + 1;
        int totalPages = pagination.getPageCount();
        int totalItems = filteredData.size();
        int fromItem = totalItems > 0 ? (pagination.getCurrentPageIndex() * ROWS_PER_PAGE) + 1 : 0;
        int toItem = Math.min(fromItem + ROWS_PER_PAGE - 1, totalItems);

        if (totalItems > 0) {
            rowsInfo.setText(I18n.get("users.pagination", fromItem, toItem, totalItems, currentPage, totalPages));
        } else {
            rowsInfo.setText(I18n.get("users.no_users"));
        }
    }

    private void showFilterMenu() {
        TLDropdownMenu filterMenu = new TLDropdownMenu();

        MenuItem allRolesItem = filterMenu.addItem(I18n.get("users.all_roles"));
        allRolesItem.setOnAction(e -> {
            selectedRoleFilter = null;
            applyFilters();
        });

        filterMenu.getItems().add(new SeparatorMenuItem());

        for (Role role : Role.values()) {
            MenuItem roleItem = filterMenu.addItem(role.getDisplayName());
            roleItem.setOnAction(e -> {
                selectedRoleFilter = role;
                applyFilters();
            });
        }

        filterMenu.showWithinWindow(filterBtn, Side.BOTTOM, 8);
    }
}
