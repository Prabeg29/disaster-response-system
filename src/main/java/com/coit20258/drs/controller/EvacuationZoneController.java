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
    @FXML private Label   formTitleLabel;
    @FXML private Button  btnModeCreate;
    @FXML private Button  btnModeUpdate;
    @FXML private VBox    createFormPane;
    @FXML private VBox    updateFormPane;

    // ── FXML — create form ─────────────────────────────────────────────────
    @FXML private TextField              nameField;
    @FXML private TextField              locationField;
    @FXML private TextField              capacityField;
    @FXML private ComboBox<DisasterReport> reportCombo;
    @FXML private Label                  createValidationLabel;
    @FXML private Button                 createBtn;

    // ── FXML — update form ─────────────────────────────────────────────────
    @FXML private Label              selectedZoneLabel;
    @FXML private TextField          occupancyField;
    @FXML private ComboBox<String>   statusCombo;
    @FXML private Label              updateValidationLabel;
    @FXML private Button             updateBtn;

    // ── State ──────────────────────────────────────────────────────────────
    private final AppService service = AppService.getInstance();
    private final ObservableList<EvacuationZone>   zoneData   = FXCollections.observableArrayList();
    private final ObservableList<DisasterReport>   reportData = FXCollections.observableArrayList();
    private EvacuationZone selectedZone;

    // ── Initialise ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        setupStatusCombo();
        setupReportCombo();
        loadData();
        showCreateForm();
    }

    // ── FXML handlers ──────────────────────────────────────────────────────
    @FXML
    private void handleModeCreate() {
        showCreateForm();
    }

    @FXML
    private void handleModeUpdate() {
        showUpdateForm();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    @FXML
    private void handleCreate() {
        hideCreateValidation();

        String name     = nameField.getText().trim();
        String location = locationField.getText().trim();
        String capText  = capacityField.getText().trim();
        DisasterReport report = reportCombo.getValue();

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
        EvacuationZone zone = new EvacuationZone(name, location, capacity, report.getId());

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

    @FXML
    private void handleUpdate() {
        hideUpdateValidation();

        if (selectedZone == null) {
            showUpdateValidation("No zone selected. Click 'Select' in the table first.");
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
                if (empty || getIndex() >= zoneData.size()) { setText(null); return; }
                EvacuationZone z = zoneData.get(getIndex());
                setText(z.getCreatedAt() != null ? z.getCreatedAt().format(DATE_FMT) : "—");
            }
        });

        colAction.setCellFactory(col -> buildSelectCell());
        zonesTable.setItems(zoneData);
    }

    private void setupStatusCombo() {
        statusCombo.setItems(FXCollections.observableArrayList(
                EvacuationZone.STATUS_ACTIVE,
                EvacuationZone.STATUS_FULL,
                EvacuationZone.STATUS_CLOSED));
    }

    private void setupReportCombo() {
        reportCombo.setItems(reportData);
        reportCombo.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(DisasterReport r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null
                        : "#" + r.getId() + " — " + r.getDisasterType() + " at " + r.getLocation());
            }
        });
        reportCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(DisasterReport r, boolean empty) {
                super.updateItem(r, empty);
                setText(empty || r == null ? null
                        : "#" + r.getId() + " — " + r.getDisasterType() + " at " + r.getLocation());
            }
        });
    }

    private TableCell<EvacuationZone, Void> buildSelectCell() {
        return new TableCell<>() {
            private final Button btn = new Button("Select");

            {
                btn.getStyleClass().add("btn-action");
                btn.setOnAction(e -> {
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
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private void showCreateForm() {
        formTitleLabel.setText("Create Evacuation Zone");
        createFormPane.setManaged(true);
        createFormPane.setVisible(true);
        updateFormPane.setManaged(false);
        updateFormPane.setVisible(false);
        btnModeCreate.getStyleClass().removeAll("btn-secondary");
        if (!btnModeCreate.getStyleClass().contains("btn-primary")) {
            btnModeCreate.getStyleClass().add("btn-primary");
        }
        btnModeUpdate.getStyleClass().removeAll("btn-primary");
        if (!btnModeUpdate.getStyleClass().contains("btn-secondary")) {
            btnModeUpdate.getStyleClass().add("btn-secondary");
        }
    }

    private void showUpdateForm() {
        formTitleLabel.setText("Update Zone Occupancy");
        createFormPane.setManaged(false);
        createFormPane.setVisible(false);
        updateFormPane.setManaged(true);
        updateFormPane.setVisible(true);
        btnModeUpdate.getStyleClass().removeAll("btn-secondary");
        if (!btnModeUpdate.getStyleClass().contains("btn-primary")) {
            btnModeUpdate.getStyleClass().add("btn-primary");
        }
        btnModeCreate.getStyleClass().removeAll("btn-primary");
        if (!btnModeCreate.getStyleClass().contains("btn-secondary")) {
            btnModeCreate.getStyleClass().add("btn-secondary");
        }
    }

    private void clearCreateForm() {
        nameField.clear();
        locationField.clear();
        capacityField.clear();
        reportCombo.setValue(null);
    }

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
