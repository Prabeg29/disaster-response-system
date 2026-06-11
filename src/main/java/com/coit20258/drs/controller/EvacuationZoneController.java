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
import com.coit20258.drs.model.EvacuationZone;
import com.coit20258.drs.service.AppService;

public class EvacuationZoneController implements Initializable {

    private static final Logger LOGGER =
            Logger.getLogger(EvacuationZoneController.class.getName());
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML — header/banner ───────────────────────────────────────────────
    @FXML private Label bannerLabel;

    // ── FXML — table ──────────────────────────────────────────────────────
    @FXML private TableView<EvacuationZone>              zonesTable;
    @FXML private TableColumn<EvacuationZone, String>    colName;
    @FXML private TableColumn<EvacuationZone, String>    colLocation;
    @FXML private TableColumn<EvacuationZone, Integer>   colCapacity;
    @FXML private TableColumn<EvacuationZone, Integer>   colOccupancy;
    @FXML private TableColumn<EvacuationZone, String>    colStatus;
    @FXML private TableColumn<EvacuationZone, Integer>   colReportId;
    @FXML private TableColumn<EvacuationZone, String>    colCreatedAt;
    @FXML private TableColumn<EvacuationZone, Void>      colAction;

    // ── FXML — form mode toggle ────────────────────────────────────────────
    @FXML private Label      formTitleLabel;
    @FXML private Button     btnModeCreate;
    @FXML private Button     btnModeEdit;
    @FXML private Button     btnModeUpdate;
    @FXML private ScrollPane createScrollPane;
    @FXML private ScrollPane editScrollPane;

    // ── FXML — create form ─────────────────────────────────────────────────
    @FXML private VBox                     createFormPane;
    @FXML private TextField                nameField;
    @FXML private TextField                locationField;
    @FXML private TextField                capacityField;
    @FXML private ComboBox<DisasterReport> reportCombo;
    @FXML private TextArea                 notesArea;
    @FXML private Label                    createValidationLabel;
    @FXML private Button                   createBtn;

    // ── FXML — edit form ───────────────────────────────────────────────────
    @FXML private VBox                     editFormPane;
    @FXML private Label                    editZoneLabel;
    @FXML private TextField                editNameField;
    @FXML private TextField                editLocationField;
    @FXML private TextField                editCapacityField;
    @FXML private ComboBox<DisasterReport> editReportCombo;
    @FXML private TextArea                 editNotesArea;
    @FXML private Label                    editValidationLabel;
    @FXML private Button                   editBtn;

    // ── FXML — update occupancy form ───────────────────────────────────────
    @FXML private VBox             updateFormPane;
    @FXML private Label            selectedZoneLabel;
    @FXML private TextField        occupancyField;
    @FXML private ComboBox<String> statusCombo;
    @FXML private Label            updateValidationLabel;
    @FXML private Button           updateBtn;

    // ── State ──────────────────────────────────────────────────────────────
    private final AppService service = AppService.getInstance();
    private final ObservableList<EvacuationZone>  zoneData   = FXCollections.observableArrayList();
    private final ObservableList<DisasterReport>  reportData = FXCollections.observableArrayList();
    private EvacuationZone editingZone;
    private EvacuationZone selectedZone;

    // ── Initialise ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupStatusCombo();
        setupReportCombos();
        loadData();
        showCreateForm();
    }

    // ── FXML handlers — mode toggles ───────────────────────────────────────
    @FXML
    private void handleModeCreate() {
        showCreateForm();
    }

    @FXML
    private void handleModeEdit() {
        showEditForm(editingZone);
    }

    @FXML
    private void handleModeUpdate() {
        showUpdateForm();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    // ── FXML handlers — create ─────────────────────────────────────────────
    @FXML
    private void handleCreate() {
        hideCreateValidation();

        String name     = nameField.getText().trim();
        String location = locationField.getText().trim();
        String capText  = capacityField.getText().trim();
        DisasterReport report = reportCombo.getValue();
        String notes    = notesArea.getText().trim();

        if (name.isEmpty()) {
            showCreateValidation("Zone name is required.");
            return;
        }
        if (location.isEmpty()) {
            showCreateValidation("Location is required.");
            return;
        }
        if (capText.isEmpty()) {
            showCreateValidation("Capacity is required.");
            return;
        }
        int capacity;
        try {
            capacity = Integer.parseInt(capText);
            if (capacity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showCreateValidation("Capacity must be a positive whole number.");
            return;
        }
        if (report == null) {
            showCreateValidation("Please select a linked disaster report.");
            return;
        }

        createBtn.setDisable(true);
        EvacuationZone zone = new EvacuationZone(name, location, capacity, report.getId(),
                notes.isEmpty() ? null : notes);

        new Thread(() -> {
            try {
                service.saveEvacuationZone(zone);
                Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    clearCreateForm();
                    showBanner("Evacuation zone created successfully.", true);
                    loadData();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to create evacuation zone", ex);
                Platform.runLater(() -> {
                    createBtn.setDisable(false);
                    showCreateValidation("Failed: " + ex.getMessage());
                });
            }
        }, "create-zone-thread").start();
    }

    // ── FXML handlers — edit ───────────────────────────────────────────────
    @FXML
    private void handleEditSave() {
        hideEditValidation();

        if (editingZone == null) {
            showEditValidation("No zone selected. Click 'Edit' in the table first.");
            return;
        }

        String name     = editNameField.getText().trim();
        String location = editLocationField.getText().trim();
        String capText  = editCapacityField.getText().trim();
        DisasterReport report = editReportCombo.getValue();
        String notes    = editNotesArea.getText().trim();

        if (name.isEmpty()) {
            showEditValidation("Zone name is required.");
            return;
        }
        if (location.isEmpty()) {
            showEditValidation("Location is required.");
            return;
        }
        if (capText.isEmpty()) {
            showEditValidation("Capacity is required.");
            return;
        }
        int capacity;
        try {
            capacity = Integer.parseInt(capText);
            if (capacity <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showEditValidation("Capacity must be a positive whole number.");
            return;
        }
        if (report == null) {
            showEditValidation("Please select a linked disaster report.");
            return;
        }

        editBtn.setDisable(true);
        editingZone.setName(name);
        editingZone.setLocation(location);
        editingZone.setCapacity(capacity);
        editingZone.setReportId(report.getId());
        editingZone.setNotes(notes.isEmpty() ? null : notes);

        EvacuationZone toUpdate = editingZone;
        new Thread(() -> {
            try {
                service.updateEvacuationZone(toUpdate);
                Platform.runLater(() -> {
                    editBtn.setDisable(false);
                    editingZone = null;
                    showBanner("Zone updated successfully.", true);
                    showCreateForm();
                    loadData();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to update evacuation zone", ex);
                Platform.runLater(() -> {
                    editBtn.setDisable(false);
                    showEditValidation("Failed: " + ex.getMessage());
                });
            }
        }, "edit-zone-thread").start();
    }

    // ── FXML handlers — update occupancy ──────────────────────────────────
    @FXML
    private void handleUpdate() {
        hideUpdateValidation();

        if (selectedZone == null) {
            showUpdateValidation("No zone selected. Click 'Occ.' in the table first.");
            return;
        }
        String occText = occupancyField.getText().trim();
        String status  = statusCombo.getValue();

        if (occText.isEmpty()) {
            showUpdateValidation("Current occupancy is required.");
            return;
        }
        int occupancy;
        try {
            occupancy = Integer.parseInt(occText);
            if (occupancy < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            showUpdateValidation("Occupancy must be a non-negative whole number.");
            return;
        }
        if (status == null) {
            showUpdateValidation("Please select a status.");
            return;
        }

        updateBtn.setDisable(true);
        int zoneId = selectedZone.getId();

        new Thread(() -> {
            try {
                service.updateEvacuationZoneOccupancy(zoneId, occupancy, status);
                Platform.runLater(() -> {
                    updateBtn.setDisable(false);
                    showBanner("Zone occupancy updated successfully.", true);
                    loadData();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to update evacuation zone", ex);
                Platform.runLater(() -> {
                    updateBtn.setDisable(false);
                    showUpdateValidation("Failed: " + ex.getMessage());
                });
            }
        }, "update-zone-thread").start();
    }

    // ── Private helpers ────────────────────────────────────────────────────
    private void loadData() {
        new Thread(() -> {
            try {
                List<EvacuationZone>  zones   = service.findAllEvacuationZones();
                List<DisasterReport>  reports = service.findAllReports();
                Platform.runLater(() -> {
                    zoneData.setAll(zones);
                    reportData.setAll(reports);
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load evacuation zone data", ex);
                Platform.runLater(() -> showBanner("Could not load data: " + ex.getMessage(), false));
            }
        }, "load-zones-thread").start();
    }

    private void setupTable() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colCapacity.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        colOccupancy.setCellValueFactory(new PropertyValueFactory<>("currentOccupancy"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReportId.setCellValueFactory(new PropertyValueFactory<>("reportId"));

        colCreatedAt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    return;
                }
                EvacuationZone z = (EvacuationZone) getTableRow().getItem();
                setText(z.getCreatedAt() != null ? z.getCreatedAt().format(DATE_FMT) : "—");
            }
        });

        colAction.setCellFactory(col -> buildActionCell());
        zonesTable.setItems(zoneData);
    }

    private void setupStatusCombo() {
        statusCombo.setItems(FXCollections.observableArrayList(
                EvacuationZone.STATUS_ACTIVE,
                EvacuationZone.STATUS_FULL,
                EvacuationZone.STATUS_CLOSED));
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
            @Override
            protected void updateItem(DisasterReport r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null
                        : "#" + r.getId() + " — " + r.getDisasterType() + " at " + r.getLocation());
            }
        };
    }

    private TableCell<EvacuationZone, Void> buildActionCell() {
        return new TableCell<>() {
            private final Button editBtn   = new Button("Edit");
            private final Button occBtn    = new Button("Occ.");
            private final Button deleteBtn = new Button("Delete");
            private final HBox   box       = new HBox(4, editBtn, occBtn, deleteBtn);

            {
                editBtn.getStyleClass().add("btn-action");
                occBtn.getStyleClass().add("btn-secondary");
                occBtn.setStyle("-fx-font-size:11px; -fx-padding:5px 8px;");
                deleteBtn.getStyleClass().add("btn-danger");

                editBtn.setOnAction(e -> {
                    EvacuationZone zone = getTableView().getItems().get(getIndex());
                    editingZone = zone;
                    showEditForm(zone);
                });

                occBtn.setOnAction(e -> {
                    selectedZone = getTableView().getItems().get(getIndex());
                    selectedZoneLabel.setText(
                            "Zone #" + selectedZone.getId()
                            + " — " + selectedZone.getName()
                            + " (" + selectedZone.getCurrentOccupancy()
                            + "/" + selectedZone.getCapacity() + ")");
                    occupancyField.setText(String.valueOf(selectedZone.getCurrentOccupancy()));
                    statusCombo.setValue(selectedZone.getStatus());
                    updateBtn.setDisable(false);
                    showUpdateForm();
                });

                deleteBtn.setOnAction(e -> {
                    EvacuationZone zone = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Delete Zone");
                    confirm.setHeaderText("Delete \"" + zone.getName() + "\"?");
                    confirm.setContentText("This cannot be undone.");
                    confirm.showAndWait().ifPresent(result -> {
                        if (result == ButtonType.OK) {
                            deleteZone(zone.getId());
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        };
    }

    private void deleteZone(int id) {
        new Thread(() -> {
            try {
                service.deleteEvacuationZone(id);
                Platform.runLater(() -> {
                    showBanner("Zone deleted.", true);
                    if (editingZone != null && editingZone.getId() == id) {
                        editingZone = null;
                    }
                    if (selectedZone != null && selectedZone.getId() == id) {
                        selectedZone = null;
                        selectedZoneLabel.setText("No zone selected — click 'Occ.' in the table.");
                        updateBtn.setDisable(true);
                    }
                    loadData();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to delete evacuation zone", ex);
                Platform.runLater(() -> showBanner("Delete failed: " + ex.getMessage(), false));
            }
        }, "delete-zone-thread").start();
    }

    // ── Form visibility helpers ────────────────────────────────────────────
    private void showCreateForm() {
        formTitleLabel.setText("Create Evacuation Zone");
        createScrollPane.setManaged(true);
        createScrollPane.setVisible(true);
        editScrollPane.setManaged(false);
        editScrollPane.setVisible(false);
        updateFormPane.setManaged(false);
        updateFormPane.setVisible(false);
        setActiveMode(btnModeCreate);
    }

    private void showEditForm(EvacuationZone zone) {
        formTitleLabel.setText("Edit Evacuation Zone");
        createScrollPane.setManaged(false);
        createScrollPane.setVisible(false);
        editScrollPane.setManaged(true);
        editScrollPane.setVisible(true);
        updateFormPane.setManaged(false);
        updateFormPane.setVisible(false);
        setActiveMode(btnModeEdit);

        if (zone != null) {
            editZoneLabel.setText("Editing Zone #" + zone.getId() + " — " + zone.getName());
            editNameField.setText(zone.getName());
            editLocationField.setText(zone.getLocation());
            editCapacityField.setText(String.valueOf(zone.getCapacity()));
            editNotesArea.setText(zone.getNotes() != null ? zone.getNotes() : "");
            // Select the matching report in the combo once report data is available
            reportData.stream()
                    .filter(r -> r.getId() == zone.getReportId())
                    .findFirst()
                    .ifPresent(editReportCombo::setValue);
        } else {
            editZoneLabel.setText("No zone selected — click Edit in the table.");
        }
        hideEditValidation();
    }

    private void showUpdateForm() {
        formTitleLabel.setText("Update Zone Occupancy");
        createScrollPane.setManaged(false);
        createScrollPane.setVisible(false);
        editScrollPane.setManaged(false);
        editScrollPane.setVisible(false);
        updateFormPane.setManaged(true);
        updateFormPane.setVisible(true);
        setActiveMode(btnModeUpdate);
    }

    private void setActiveMode(Button active) {
        for (Button btn : new Button[]{ btnModeCreate, btnModeEdit, btnModeUpdate }) {
            btn.getStyleClass().removeAll("btn-primary", "btn-secondary");
            btn.getStyleClass().add(btn == active ? "btn-primary" : "btn-secondary");
        }
    }

    private void clearCreateForm() {
        nameField.clear();
        locationField.clear();
        capacityField.clear();
        reportCombo.setValue(null);
        notesArea.clear();
    }

    // ── Banner / validation helpers ────────────────────────────────────────
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

    private void showUpdateValidation(String msg) {
        updateValidationLabel.setText(msg);
        updateValidationLabel.setManaged(true);
        updateValidationLabel.setVisible(true);
    }

    private void hideUpdateValidation() {
        updateValidationLabel.setManaged(false);
        updateValidationLabel.setVisible(false);
    }
}
