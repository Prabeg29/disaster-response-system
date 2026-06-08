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

import com.coit20258.drs.model.Department;
import com.coit20258.drs.model.DepartmentUpdate;
import com.coit20258.drs.model.DisasterReport;
import com.coit20258.drs.model.User;
import com.coit20258.drs.service.AppService;
import com.coit20258.drs.util.SessionContext;

public class DepartmentCoordinationController implements Initializable {

    private static final Logger LOGGER =
            Logger.getLogger(DepartmentCoordinationController.class.getName());
    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML — header ──────────────────────────────────────────────────────
    @FXML private Label headerDeptLabel;
    @FXML private Label headerRoleLabel;

    // ── FXML — assigned reports table ──────────────────────────────────────
    @FXML private TableView<DisasterReport>           reportsTable;
    @FXML private TableColumn<DisasterReport, String> colType;
    @FXML private TableColumn<DisasterReport, String> colLocation;
    @FXML private TableColumn<DisasterReport, String> colSeverity;
    @FXML private TableColumn<DisasterReport, String> colStatus;
    @FXML private TableColumn<DisasterReport, Void>   colAction;

    // ── FXML — update form (right panel) ──────────────────────────────────
    @FXML private Label               selectedReportLabel;
    @FXML private ComboBox<String>    responseStatusCombo;
    @FXML private TextArea            updateTextArea;
    @FXML private Button              submitUpdateBtn;
    @FXML private Label               formValidationLabel;

    // ── FXML — updates history table ──────────────────────────────────────
    @FXML private TableView<DepartmentUpdate>              updatesTable;
    @FXML private TableColumn<DepartmentUpdate, String>    colUpdDept;
    @FXML private TableColumn<DepartmentUpdate, String>    colUpdStatus;
    @FXML private TableColumn<DepartmentUpdate, String>    colUpdText;
    @FXML private TableColumn<DepartmentUpdate, String>    colUpdBy;
    @FXML private TableColumn<DepartmentUpdate, String>    colUpdAt;

    // ── FXML — banner ─────────────────────────────────────────────────────
    @FXML private Label bannerLabel;

    // ── State ──────────────────────────────────────────────────────────────
    private final AppService service = AppService.getInstance();
    private final ObservableList<DisasterReport>   reportData  = FXCollections.observableArrayList();
    private final ObservableList<DepartmentUpdate> updateData  = FXCollections.observableArrayList();
    private DisasterReport selectedReport;
    private Department     currentDepartment;

    // ── Initialise ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupReportTable();
        setupUpdatesTable();
        setupForm();
        loadUserContext();
    }

    // ── FXML handlers ──────────────────────────────────────────────────────
    @FXML
    private void handleSubmitUpdate() {
        hideFormValidation();

        if (selectedReport == null) {
            showFormValidation("Please select a report from the table first.");
            return;
        }
        String status = responseStatusCombo.getValue();
        String text   = updateTextArea.getText().trim();

        if (status == null) {
            showFormValidation("Please select a response status.");
            return;
        }
        if (text.isEmpty()) {
            showFormValidation("Update text cannot be empty.");
            return;
        }

        User me = SessionContext.getCurrentUser();
        int deptId = currentDepartment != null ? currentDepartment.getId() : me.getDepartmentId();
        if (deptId <= 0) {
            showFormValidation("No department assigned to your account. Contact an administrator.");
            return;
        }

        submitUpdateBtn.setDisable(true);

        DepartmentUpdate upd = new DepartmentUpdate(
                selectedReport.getId(),
                deptId,
                me.getId(),
                text,
                status
        );

        new Thread(() -> {
            try {
                service.saveDepartmentUpdate(upd);
                Platform.runLater(() -> {
                    submitUpdateBtn.setDisable(false);
                    updateTextArea.clear();
                    showBanner("Update posted successfully.", true);
                    loadUpdatesForReport(selectedReport);
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to save update", ex);
                Platform.runLater(() -> {
                    submitUpdateBtn.setDisable(false);
                    showFormValidation("Failed: " + ex.getMessage());
                });
            }
        }, "submit-update-thread").start();
    }

    @FXML
    private void handleRefresh() {
        loadAssignedReports();
    }

    // ── Private helpers ────────────────────────────────────────────────────
    private void loadUserContext() {
        User u = SessionContext.getCurrentUser();
        boolean isDept = User.ROLE_DEPARTMENT.equals(u.getRole());

        if (isDept) {
            headerRoleLabel.setText("Department Coordinator");
            new Thread(() -> {
                try {
                    Department d = service.findDepartmentById(u.getDepartmentId()).orElse(null);
                    Platform.runLater(() -> {
                        currentDepartment = d;
                        headerDeptLabel.setText(d != null ? d.getName() : "Unknown Department");
                        loadAssignedReports();
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> showBanner("Could not load department: " + ex.getMessage(), false));
                }
            }, "load-dept-thread").start();
        } else {
            // Admin/Operator view — show all updates across all departments
            headerDeptLabel.setText("All Departments");
            headerRoleLabel.setText(u.getRole());
            loadAllReports();
        }
    }

    private void loadAssignedReports() {
        User u = SessionContext.getCurrentUser();
        new Thread(() -> {
            try {
                List<DisasterReport> reports = service.findReportsAssignedToDepartment(u.getDepartmentId());
                Platform.runLater(() -> {
                    reportData.setAll(reports);
                    if (reports.isEmpty()) {
                        showBanner("No reports are currently assigned to your department.", true);
                    }
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load assigned reports", ex);
                Platform.runLater(() -> showBanner("Could not load reports: " + ex.getMessage(), false));
            }
        }, "load-assigned-thread").start();
    }

    private void loadAllReports() {
        new Thread(() -> {
            try {
                List<DisasterReport> reports = service.findAllReports();
                Platform.runLater(() -> reportData.setAll(reports));
            } catch (Exception ex) {
                Platform.runLater(() -> showBanner("Could not load reports: " + ex.getMessage(), false));
            }
        }, "load-all-reports-thread").start();
    }

    private void loadUpdatesForReport(DisasterReport report) {
        new Thread(() -> {
            try {
                List<DepartmentUpdate> updates = service.findUpdatesByReport(report.getId());
                Platform.runLater(() -> updateData.setAll(updates));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to load updates", ex);
            }
        }, "load-updates-thread").start();
    }

    private void setupReportTable() {
        colType.setCellValueFactory(new PropertyValueFactory<>("disasterType"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severityLevel"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAction.setCellFactory(col -> buildSelectCell());
        reportsTable.setItems(reportData);
    }

    private void setupUpdatesTable() {
        colUpdDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colUpdStatus.setCellValueFactory(new PropertyValueFactory<>("responseStatus"));
        colUpdText.setCellValueFactory(new PropertyValueFactory<>("updateText"));
        colUpdBy.setCellValueFactory(new PropertyValueFactory<>("updatedByName"));
        colUpdAt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || getIndex() >= updateData.size()) { setText(null); return; }
                DepartmentUpdate u = updateData.get(getIndex());
                setText(u.getUpdatedAt() != null ? u.getUpdatedAt().format(DATE_FMT) : "—");
            }
        });
        updatesTable.setItems(updateData);
    }

    private void setupForm() {
        responseStatusCombo.setItems(FXCollections.observableArrayList(
                DepartmentUpdate.STATUS_RESPONDING,
                DepartmentUpdate.STATUS_COMPLETED,
                DepartmentUpdate.STATUS_NEEDS_SUPPORT
        ));
        selectedReportLabel.setText("No report selected — click 'Select' in the table above.");
        submitUpdateBtn.setDisable(true);
        hideFormValidation();
    }

    private TableCell<DisasterReport, Void> buildSelectCell() {
        return new TableCell<>() {
            private final Button btn = new Button("Select");

            {
                btn.getStyleClass().add("btn-action");
                btn.setOnAction(e -> {
                    selectedReport = getTableView().getItems().get(getIndex());
                    selectedReportLabel.setText(
                            "Report #" + selectedReport.getId()
                            + " — " + selectedReport.getDisasterType()
                            + " at " + selectedReport.getLocation());
                    submitUpdateBtn.setDisable(false);
                    loadUpdatesForReport(selectedReport);
                });
            }

            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }

    private void showBanner(String msg, boolean success) {
        bannerLabel.setText(msg);
        bannerLabel.getStyleClass().removeAll("success-label", "validation-label");
        bannerLabel.getStyleClass().add(success ? "success-label" : "validation-label");
        bannerLabel.setManaged(true);
        bannerLabel.setVisible(true);
    }

    private void showFormValidation(String msg) {
        formValidationLabel.setText(msg);
        formValidationLabel.setManaged(true);
        formValidationLabel.setVisible(true);
    }

    private void hideFormValidation() {
        formValidationLabel.setManaged(false);
        formValidationLabel.setVisible(false);
    }
}
