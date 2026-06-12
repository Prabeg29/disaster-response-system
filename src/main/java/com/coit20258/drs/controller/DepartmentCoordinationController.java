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

/**
 * DepartmentCoordinationController — Department Coordination View
 *
 * Allows Department Coordinators to view disaster reports assigned to
 * their department and post response updates (RESPONDING, COMPLETED,
 * or NEEDS_SUPPORT). Administrators and Operators see all reports
 * across all departments.
 *
 * Implemented by: Poojitha Myneni
 * COIT20258 — Assignment 3, HE T1 2026
 */
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

    // ── FXML — update form (right panel) ───────────────────────────────────
    @FXML private Label            selectedReportLabel;
    @FXML private ComboBox<String> responseStatusCombo;
    @FXML private TextArea         updateTextArea;
    @FXML private Button           submitUpdateBtn;
    @FXML private Label            formValidationLabel;

    // ── FXML — updates history table ───────────────────────────────────────
    @FXML private TableView<DepartmentUpdate>           updatesTable;
    @FXML private TableColumn<DepartmentUpdate, String> colUpdDept;
    @FXML private TableColumn<DepartmentUpdate, String> colUpdStatus;
    @FXML private TableColumn<DepartmentUpdate, String> colUpdText;
    @FXML private TableColumn<DepartmentUpdate, String> colUpdBy;
    @FXML private TableColumn<DepartmentUpdate, String> colUpdAt;

    // ── FXML — banner ──────────────────────────────────────────────────────
    @FXML private Label bannerLabel;

    // ── State ──────────────────────────────────────────────────────────────
    private final AppService service = AppService.getInstance();
    private final ObservableList<DisasterReport>   reportData = FXCollections.observableArrayList();
    private final ObservableList<DepartmentUpdate> updateData = FXCollections.observableArrayList();
    private DisasterReport selectedReport;
    private Department     currentDepartment;

    // ── Initialise ─────────────────────────────────────────────────────────

    /**
     * Called automatically after FXML injection. Sets up both tables,
     * initialises the form, and loads the current user's department context.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupReportTable();
        setupUpdatesTable();
        setupForm();
        loadUserContext();
    }

    // ── FXML handlers ──────────────────────────────────────────────────────

    /**
     * Validates the update form and, if valid, posts the department update
     * to the server on a background thread.
     */
    @FXML
    private void handleSubmitUpdate() {
        hideFormValidation();

        // Guard: a report must be selected before posting an update
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

        User me    = SessionContext.getCurrentUser();
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
                LOGGER.log(Level.SEVERE, "Failed to save department update", ex);
                Platform.runLater(() -> {
                    submitUpdateBtn.setDisable(false);
                    showFormValidation("Failed to post update: " + ex.getMessage());
                });
            }
        }, "submit-update-thread").start();
    }

    /**
     * Refresh button handler — reloads the assigned reports list.
     */
    @FXML
    private void handleRefresh() {
        loadAssignedReports();
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /**
     * Reads the current user's role from SessionContext and adjusts the
     * view accordingly. Department Coordinators see only their department's
     * reports; Admins and Operators see all reports.
     */
    private void loadUserContext() {
        User u     = SessionContext.getCurrentUser();
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
                    Platform.runLater(() ->
                            showBanner("Could not load department info: " + ex.getMessage(), false));
                }
            }, "load-dept-thread").start();
        } else {
            // Admin / Operator: show all departments and all reports
            headerDeptLabel.setText("All Departments");
            headerRoleLabel.setText(u.getRole());
            loadAllReports();
        }
    }

    /**
     * Loads reports assigned to the current Department Coordinator's
     * department using their departmentId.
     */
    private void loadAssignedReports() {
        User u = SessionContext.getCurrentUser();
        new Thread(() -> {
            try {
                List<DisasterReport> reports =
                        service.findReportsAssignedToDepartment(u.getDepartmentId());
                Platform.runLater(() -> {
                    reportData.setAll(reports);
                    if (reports.isEmpty()) {
                        showBanner("No reports are currently assigned to your department.", true);
                    }
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load assigned reports", ex);
                Platform.runLater(() ->
                        showBanner("Could not load reports: " + ex.getMessage(), false));
            }
        }, "load-assigned-thread").start();
    }

    /**
     * Loads all reports from the server (used by Admin / Operator roles).
     */
    private void loadAllReports() {
        new Thread(() -> {
            try {
                List<DisasterReport> reports = service.findAllReports();
                Platform.runLater(() -> reportData.setAll(reports));
            } catch (Exception ex) {
                Platform.runLater(() ->
                        showBanner("Could not load reports: " + ex.getMessage(), false));
            }
        }, "load-all-reports-thread").start();
    }

    /**
     * Fetches all department updates for the given report and populates
     * the update history table.
     *
     * @param report the report whose updates should be displayed
     */
    private void loadUpdatesForReport(DisasterReport report) {
        new Thread(() -> {
            try {
                List<DepartmentUpdate> updates = service.findUpdatesByReport(report.getId());
                Platform.runLater(() -> updateData.setAll(updates));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to load updates for report " + report.getId(), ex);
            }
        }, "load-updates-thread").start();
    }

    /**
     * Wires up the assigned-reports table columns.
     */
    private void setupReportTable() {
        colType.setCellValueFactory(new PropertyValueFactory<>("disasterType"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severityLevel"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAction.setCellFactory(col -> buildSelectCell());
        reportsTable.setItems(reportData);
    }

    /**
     * Wires up the response-updates history table columns.
     * The timestamp column uses a custom cell factory to format LocalDateTime.
     */
    private void setupUpdatesTable() {
        colUpdDept.setCellValueFactory(new PropertyValueFactory<>("departmentName"));
        colUpdStatus.setCellValueFactory(new PropertyValueFactory<>("responseStatus"));
        colUpdText.setCellValueFactory(new PropertyValueFactory<>("updateText"));
        colUpdBy.setCellValueFactory(new PropertyValueFactory<>("updatedByName"));

        colUpdAt.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || getIndex() >= updateData.size()) {
                    setText(null);
                    return;
                }
                DepartmentUpdate u = updateData.get(getIndex());
                setText(u.getUpdatedAt() != null ? u.getUpdatedAt().format(DATE_FMT) : "—");
            }
        });

        updatesTable.setItems(updateData);
    }

    /**
     * Initialises the update form: populates the status ComboBox,
     * disables the submit button, and hides the validation label.
     */
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

    /**
     * Builds a "Select" button cell for the action column of the reports table.
     * Clicking the button sets the selectedReport, updates the form header,
     * enables the submit button, and loads the update history for that report.
     */
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

    /**
     * Shows a coloured banner message at the top of the view.
     */
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
