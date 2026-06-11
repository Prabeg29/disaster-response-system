package com.coit20258.drs.controller;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import com.coit20258.drs.model.DisasterReport;
import com.coit20258.drs.model.Resource;
import com.coit20258.drs.service.AppService;

public class ResourceController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(ResourceController.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML — header/banner ───────────────────────────────────────────────
    @FXML private Label bannerLabel;

    // ── FXML — filter + table ──────────────────────────────────────────────
    @FXML private ComboBox<String>               filterCombo;
    @FXML private TableView<Resource>            resourcesTable;
    @FXML private TableColumn<Resource, String>  colName;
    @FXML private TableColumn<Resource, String>  colType;
    @FXML private TableColumn<Resource, Integer> colTotal;
    @FXML private TableColumn<Resource, Integer> colAvailable;
    @FXML private TableColumn<Resource, String>  colStatus;
    @FXML private TableColumn<Resource, Integer> colReportId;
    @FXML private TableColumn<Resource, String>  colUpdatedAt;
    @FXML private TableColumn<Resource, Void>    colAction;

    // ── FXML — mode toggles ────────────────────────────────────────────────
    @FXML private Label      formTitleLabel;
    @FXML private Button     btnModeCreate;
    @FXML private Button     btnModeEdit;
    @FXML private ScrollPane createScrollPane;
    @FXML private ScrollPane editScrollPane;

    // ── FXML — create form ─────────────────────────────────────────────────
    @FXML private VBox                     createFormPane;
    @FXML private TextField                nameField;
    @FXML private ComboBox<String>         typeCombo;
    @FXML private TextField                totalField;
    @FXML private TextField                availableField;
    @FXML private ComboBox<String>         statusCombo;
    @FXML private ComboBox<DisasterReport> reportCombo;
    @FXML private TextArea                 notesArea;
    @FXML private Label                    createValidationLabel;
    @FXML private Button                   createBtn;

    // ── FXML — edit form ───────────────────────────────────────────────────
    @FXML private VBox                     editFormPane;
    @FXML private Label                    editResourceLabel;
    @FXML private TextField                editNameField;
    @FXML private ComboBox<String>         editTypeCombo;
    @FXML private TextField                editTotalField;
    @FXML private TextField                editAvailableField;
    @FXML private ComboBox<String>         editStatusCombo;
    @FXML private ComboBox<DisasterReport> editReportCombo;
    @FXML private TextArea                 editNotesArea;
    @FXML private Label                    editValidationLabel;
    @FXML private Button                   editBtn;

    // ── State ──────────────────────────────────────────────────────────────
    private final AppService service = AppService.getInstance();
    private final ObservableList<Resource>       resourceData = FXCollections.observableArrayList();
    private final ObservableList<DisasterReport> reportData   = FXCollections.observableArrayList();
    private List<Resource>  allResources = List.of();
    private Resource        editingResource;

    private static final String FILTER_ALL = "All Types";

    // ── Initialise ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupTypeCombos();
        setupStatusCombos();
        setupReportCombos();
        setupFilterCombo();
        loadData();
        showCreateForm();
    }

    // ── FXML handlers — mode ───────────────────────────────────────────────
    @FXML private void handleModeCreate() { showCreateForm(); }
    @FXML private void handleModeEdit()   { showEditForm(editingResource); }
    @FXML private void handleRefresh()    { loadData(); }

    // ── FXML handlers — create ─────────────────────────────────────────────
    @FXML
    private void handleCreate() {
        hideCreateValidation();

        String name      = nameField.getText().trim();
        String type      = typeCombo.getValue();
        String totText   = totalField.getText().trim();
        String availText = availableField.getText().trim();
        String status    = statusCombo.getValue();

        if (name.isEmpty())  { showCreateValidation("Name is required."); return; }
        if (type == null)    { showCreateValidation("Resource type is required."); return; }
        if (totText.isEmpty()){ showCreateValidation("Total quantity is required."); return; }
        if (availText.isEmpty()){ showCreateValidation("Available quantity is required."); return; }
        if (status == null)  { showCreateValidation("Status is required."); return; }

        int total, available;
        try {
            total = Integer.parseInt(totText);
            if (total < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showCreateValidation("Total quantity must be a non-negative whole number."); return;
        }
        try {
            available = Integer.parseInt(availText);
            if (available < 0 || available > total) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showCreateValidation("Available must be 0 – " + total + "."); return;
        }

        DisasterReport report = reportCombo.getValue();
        int reportId = (report != null) ? report.getId() : 0;
        String notes = notesArea.getText().trim();

        createBtn.setDisable(true);
        Resource resource = new Resource(name, type, total, available, status,
                reportId, notes.isEmpty() ? null : notes);

        new Thread(() -> {
            try {
                service.saveResource(resource);
                Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    clearCreateForm();
                    showBanner("Resource added successfully.", true);
                    loadData();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to create resource", ex);
                Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    showCreateValidation("Failed: " + ex.getMessage());
                });
            }
        }, "create-resource-thread").start();
    }

    // ── FXML handlers — edit ───────────────────────────────────────────────
    @FXML
    private void handleEditSave() {
        hideEditValidation();

        if (editingResource == null) {
            showEditValidation("No resource selected. Click Edit in the table first."); return;
        }

        String name      = editNameField.getText().trim();
        String type      = editTypeCombo.getValue();
        String totText   = editTotalField.getText().trim();
        String availText = editAvailableField.getText().trim();
        String status    = editStatusCombo.getValue();

        if (name.isEmpty())  { showEditValidation("Name is required."); return; }
        if (type == null)    { showEditValidation("Resource type is required."); return; }
        if (totText.isEmpty()){ showEditValidation("Total quantity is required."); return; }
        if (availText.isEmpty()){ showEditValidation("Available quantity is required."); return; }
        if (status == null)  { showEditValidation("Status is required."); return; }

        int total, available;
        try {
            total = Integer.parseInt(totText);
            if (total < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showEditValidation("Total quantity must be a non-negative whole number."); return;
        }
        try {
            available = Integer.parseInt(availText);
            if (available < 0 || available > total) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showEditValidation("Available must be 0 – " + total + "."); return;
        }

        DisasterReport report = editReportCombo.getValue();
        String notes = editNotesArea.getText().trim();

        editingResource.setName(name);
        editingResource.setResourceType(type);
        editingResource.setTotalQuantity(total);
        editingResource.setAvailableQuantity(available);
        editingResource.setStatus(status);
        editingResource.setAssignedReportId(report != null ? report.getId() : 0);
        editingResource.setNotes(notes.isEmpty() ? null : notes);

        editBtn.setDisable(true);
        Resource toUpdate = editingResource;

        new Thread(() -> {
            try {
                service.updateResource(toUpdate);
                Platform.runLater(() -> {
                    editBtn.setDisable(false);
                    editingResource = null;
                    showBanner("Resource updated successfully.", true);
                    showCreateForm();
                    loadData();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to update resource", ex);
                Platform.runLater(() -> {
                    editBtn.setDisable(false);
                    showEditValidation("Failed: " + ex.getMessage());
                });
            }
        }, "edit-resource-thread").start();
    }

    // ── Private helpers ────────────────────────────────────────────────────
    private void loadData() {
        new Thread(() -> {
            try {
                List<Resource>       resources = service.findAllResources();
                List<DisasterReport> reports   = service.findAllReports();
                Platform.runLater(() -> {
                    allResources = resources;
                    reportData.setAll(reports);
                    applyFilter(filterCombo.getValue());
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load resource data", ex);
                Platform.runLater(() -> showBanner("Could not load data: " + ex.getMessage(), false));
            }
        }, "load-resources-thread").start();
    }

    private void applyFilter(String filter) {
        if (filter == null || filter.equals(FILTER_ALL)) {
            resourceData.setAll(allResources);
        } else {
            resourceData.setAll(allResources.stream()
                    .filter(r -> filter.equals(r.getResourceType()))
                    .toList());
        }
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colType.setCellValueFactory(new PropertyValueFactory<>("resourceType"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("totalQuantity"));
        colAvailable.setCellValueFactory(new PropertyValueFactory<>("availableQuantity"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReportId.setCellValueFactory(new PropertyValueFactory<>("assignedReportId"));

        colUpdatedAt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                Resource r = (Resource) getTableRow().getItem();
                setText(r.getUpdatedAt() != null ? r.getUpdatedAt().format(DATE_FMT) : "—");
            }
        });

        colAction.setCellFactory(col -> buildActionCell());
        resourcesTable.setItems(resourceData);
    }

    private void setupFilterCombo() {
        filterCombo.setItems(FXCollections.observableArrayList(
                FILTER_ALL,
                Resource.TYPE_VEHICLE, Resource.TYPE_EQUIPMENT,
                Resource.TYPE_PERSONNEL, Resource.TYPE_MEDICAL, Resource.TYPE_SUPPLIES));
        filterCombo.setValue(FILTER_ALL);
        filterCombo.valueProperty().addListener((obs, old, val) -> applyFilter(val));
    }

    private void setupTypeCombos() {
        ObservableList<String> types = FXCollections.observableArrayList(
                Resource.TYPE_VEHICLE, Resource.TYPE_EQUIPMENT,
                Resource.TYPE_PERSONNEL, Resource.TYPE_MEDICAL, Resource.TYPE_SUPPLIES);
        typeCombo.setItems(types);
        editTypeCombo.setItems(types);
    }

    private void setupStatusCombos() {
        ObservableList<String> statuses = FXCollections.observableArrayList(
                Resource.STATUS_AVAILABLE, Resource.STATUS_DEPLOYED, Resource.STATUS_MAINTENANCE);
        statusCombo.setItems(statuses);
        editStatusCombo.setItems(statuses);
    }

    private void setupReportCombos() {
        reportCombo.setItems(reportData);
        editReportCombo.setItems(reportData);
        reportCombo.setCellFactory(lv -> makeReportCell());
        reportCombo.setButtonCell(makeReportCell());
        editReportCombo.setCellFactory(lv -> makeReportCell());
        editReportCombo.setButtonCell(makeReportCell());
    }

    private ListCell<DisasterReport> makeReportCell() {
        return new ListCell<>() {
            @Override protected void updateItem(DisasterReport r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null
                        : "#" + r.getId() + " — " + r.getDisasterType() + " at " + r.getLocation());
            }
        };
    }

    private TableCell<Resource, Void> buildActionCell() {
        return new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(4, editBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-action");
                deleteBtn.getStyleClass().add("btn-danger");

                editBtn.setOnAction(e -> {
                    Resource res = getTableView().getItems().get(getIndex());
                    editingResource = res;
                    showEditForm(res);
                });

                deleteBtn.setOnAction(e -> {
                    Resource res = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Resource");
                    confirm.setHeaderText("Delete \"" + res.getName() + "\"?");
                    confirm.setContentText("This cannot be undone.");
                    confirm.showAndWait().ifPresent(result -> {
                        if (result == ButtonType.OK) deleteResource(res.getId());
                    });
                });
            }

            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private void deleteResource(int id) {
        new Thread(() -> {
            try {
                service.deleteResource(id);
                Platform.runLater(() -> {
                    if (editingResource != null && editingResource.getId() == id) editingResource = null;
                    showBanner("Resource deleted.", true);
                    loadData();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to delete resource", ex);
                Platform.runLater(() -> showBanner("Delete failed: " + ex.getMessage(), false));
            }
        }, "delete-resource-thread").start();
    }

    // ── Form visibility helpers ────────────────────────────────────────────
    private void showCreateForm() {
        formTitleLabel.setText("Add Resource");
        createScrollPane.setManaged(true);
        createScrollPane.setVisible(true);
        editScrollPane.setManaged(false);
        editScrollPane.setVisible(false);
        setActiveMode(btnModeCreate);
    }

    private void showEditForm(Resource res) {
        formTitleLabel.setText("Edit Resource");
        createScrollPane.setManaged(false);
        createScrollPane.setVisible(false);
        editScrollPane.setManaged(true);
        editScrollPane.setVisible(true);
        setActiveMode(btnModeEdit);

        if (res != null) {
            editResourceLabel.setText("Editing: #" + res.getId() + " — " + res.getName());
            editNameField.setText(res.getName());
            editTypeCombo.setValue(res.getResourceType());
            editTotalField.setText(String.valueOf(res.getTotalQuantity()));
            editAvailableField.setText(String.valueOf(res.getAvailableQuantity()));
            editStatusCombo.setValue(res.getStatus());
            editNotesArea.setText(res.getNotes() != null ? res.getNotes() : "");
            if (res.getAssignedReportId() > 0) {
                reportData.stream()
                        .filter(r -> r.getId() == res.getAssignedReportId())
                        .findFirst()
                        .ifPresent(editReportCombo::setValue);
            } else {
                editReportCombo.setValue(null);
            }
        } else {
            editResourceLabel.setText("No resource selected — click Edit in the table.");
        }
        hideEditValidation();
    }

    private void setActiveMode(Button active) {
        for (Button btn : new Button[]{ btnModeCreate, btnModeEdit }) {
            btn.getStyleClass().removeAll("btn-primary", "btn-secondary");
            btn.getStyleClass().add(btn == active ? "btn-primary" : "btn-secondary");
        }
    }

    private void clearCreateForm() {
        nameField.clear();
        typeCombo.setValue(null);
        totalField.clear();
        availableField.clear();
        statusCombo.setValue(null);
        reportCombo.setValue(null);
        notesArea.clear();
    }

    // ── Banner / validation ────────────────────────────────────────────────
    private void showBanner(String msg, boolean success) {
        bannerLabel.setText(msg);
        bannerLabel.getStyleClass().removeAll("success-label", "validation-label");
        bannerLabel.getStyleClass().add(success ? "success-label" : "validation-label");
        bannerLabel.setManaged(true);
        bannerLabel.setVisible(true);
    }

    private void showCreateValidation(String msg) {
        createValidationLabel.setText(msg);
        createValidationLabel.setManaged(true);
        createValidationLabel.setVisible(true);
    }

    private void hideCreateValidation() {
        createValidationLabel.setManaged(false);
        createValidationLabel.setVisible(false);
    }

    private void showEditValidation(String msg) {
        editValidationLabel.setText(msg);
        editValidationLabel.setManaged(true);
        editValidationLabel.setVisible(true);
    }

    private void hideEditValidation() {
        editValidationLabel.setManaged(false);
        editValidationLabel.setVisible(false);
    }
}
