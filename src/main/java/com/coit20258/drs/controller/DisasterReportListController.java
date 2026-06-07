package com.coit20258.drs.controller;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import com.coit20258.drs.dao.DepartmentDao;
import com.coit20258.drs.dao.DepartmentDaoImpl;
import com.coit20258.drs.dao.DisasterAssessmentDao;
import com.coit20258.drs.dao.DisasterAssessmentDaoImpl;
import com.coit20258.drs.dao.DisasterReportDao;
import com.coit20258.drs.dao.DisasterReportDaoImpl;
import com.coit20258.drs.model.Department;
import com.coit20258.drs.model.DisasterAssessment;
import com.coit20258.drs.model.DisasterReport;
import com.coit20258.drs.model.User;
import com.coit20258.drs.util.SceneManager;

public class DisasterReportListController implements Initializable {

    private static final Logger LOGGER
            = Logger.getLogger(DisasterReportListController.class.getName());
    private static final DateTimeFormatter DATE_FMT
            = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── FXML ───────────────────────────────────────────────────────────────
    @FXML private TableView<DisasterReport>              reportsTable;
    @FXML private TableColumn<DisasterReport, String>    colType;
    @FXML private TableColumn<DisasterReport, String>    colLocation;
    @FXML private TableColumn<DisasterReport, String>    colSeverity;
    @FXML private TableColumn<DisasterReport, String>    colStatus;
    @FXML private TableColumn<DisasterReport, User>      colReportedBy;
    @FXML private TableColumn<DisasterReport, LocalDateTime> colReportedAt;
    @FXML private TableColumn<DisasterReport, Void>      colAction;
    @FXML private Label                                  bannerLabel;
    @FXML private ComboBox<Department>                   departmentFilterCombo;

    private final DisasterReportDao     reportDao     = new DisasterReportDaoImpl();
    private final DisasterAssessmentDao assessmentDao = new DisasterAssessmentDaoImpl();
    private final DepartmentDao         departmentDao = new DepartmentDaoImpl();

    private List<DisasterReport>     allReports    = List.of();
    // reportId → set of department IDs assigned via its assessment
    private Map<Integer, Set<Integer>> reportDeptMap = Map.of();

    private final ObservableList<DisasterReport> reportData
            = FXCollections.observableArrayList();

    // ── Initialise ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        reportsTable.setItems(reportData);
        loadDataAsync();
        attachFilterListener();
    }

    // ── FXML handlers ──────────────────────────────────────────────────────
    @FXML
    private void handleNewReport() {
        SceneManager.switchContent("DisasterReportFormView");
    }

    // ── Private helpers ────────────────────────────────────────────────────
    private void setupTableColumns() {
        colType.setCellValueFactory(new PropertyValueFactory<>("disasterType"));
        colLocation.setCellValueFactory(new PropertyValueFactory<>("location"));
        colSeverity.setCellValueFactory(new PropertyValueFactory<>("severityLevel"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colReportedBy.setCellValueFactory(new PropertyValueFactory<>("reportedBy"));
        colReportedAt.setCellValueFactory(new PropertyValueFactory<>("reportedAt"));

        colSeverity.setCellFactory(col -> buildSeverityCell());
        colStatus.setCellFactory(col -> buildStatusComboCell());
        colReportedBy.setCellFactory(col -> buildReportedByCell());
        colReportedAt.setCellFactory(col -> buildDateCell());
        colAction.setCellFactory(col -> buildActionCell());
    }

    private void loadDataAsync() {
        new Thread(() -> {
            try {
                List<DisasterReport> reports       = reportDao.findAll();
                List<DisasterAssessment> assessments = assessmentDao.findAll();
                List<Department> departments        = departmentDao.findAll();

                Map<Integer, Set<Integer>> deptMap = assessments.stream()
                        .collect(Collectors.toMap(
                                DisasterAssessment::getReportId,
                                a -> a.getAssignedDepartments().stream()
                                        .map(Department::getId)
                                        .collect(Collectors.toSet()),
                                (a, b) -> { a.addAll(b); return a; }
                        ));

                Platform.runLater(() -> {
                    allReports    = reports;
                    reportDeptMap = deptMap;
                    reportData.setAll(reports);

                    // "All" sentinel at top, then real departments
                    List<Department> items = new java.util.ArrayList<>();
                    items.add(null);          // represents "All"
                    items.addAll(departments);
                    departmentFilterCombo.setItems(FXCollections.observableArrayList(items));

                    // Show "All departments" for the null entry
                    departmentFilterCombo.setButtonCell(new ListCell<>() {
                        @Override protected void updateItem(Department d, boolean empty) {
                            super.updateItem(d, empty);
                            setText(d == null ? "All departments" : d.getName());
                        }
                    });
                    departmentFilterCombo.setCellFactory(lv -> new ListCell<>() {
                        @Override protected void updateItem(Department d, boolean empty) {
                            super.updateItem(d, empty);
                            setText(empty ? null : (d == null ? "All departments" : d.getName()));
                        }
                    });
                    departmentFilterCombo.getSelectionModel().selectFirst();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load data", ex);
                Platform.runLater(() -> showBanner(
                        "Could not load reports: " + ex.getMessage(), false));
            }
        }, "load-reports-thread").start();
    }

    private void attachFilterListener() {
        departmentFilterCombo.valueProperty().addListener((obs, old, dept) -> {
            if (dept == null) {
                reportData.setAll(allReports);
            } else {
                List<DisasterReport> filtered = allReports.stream()
                        .filter(r -> {
                            Set<Integer> depts = reportDeptMap.get(r.getId());
                            return depts != null && depts.contains(dept.getId());
                        })
                        .toList();
                reportData.setAll(filtered);
            }
        });
    }

    private void showBanner(String msg, boolean success) {
        bannerLabel.setText(msg);
        bannerLabel.getStyleClass().removeAll("success-label", "validation-label");
        bannerLabel.getStyleClass().add(success ? "success-label" : "validation-label");
        bannerLabel.setManaged(true);
        bannerLabel.setVisible(true);
    }

    // ── Cell factory builders ──────────────────────────────────────────────
    private TableCell<DisasterReport, String> buildSeverityCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String sev, boolean empty) {
                super.updateItem(sev, empty);
                if (empty || sev == null) { setText(null); setStyle(""); return; }
                setText(sev);
            }
        };
    }

    private TableCell<DisasterReport, String> buildStatusComboCell() {
        return new TableCell<>() {
            private final ComboBox<String> combo = new ComboBox<>();

            {
                combo.getItems().addAll(
                        DisasterReport.STATUS_REPORTED,
                        DisasterReport.STATUS_RESOLVED,
                        DisasterReport.STATUS_RESPONDING,
                        DisasterReport.STATUS_UNDER_ASSESSMENT
                );
                combo.getStyleClass().add("status-combo");
            }

            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setGraphic(null); return; }
                combo.setOnAction(null);
                combo.setValue(status);
                combo.setOnAction(e -> {
                    DisasterReport r = getTableView().getItems().get(getIndex());
                    String selected = combo.getValue();
                    if (selected != null && !selected.equals(r.getStatus())) {
                        new Thread(() -> {
                            try {
                                reportDao.updateStatus(r.getId(), selected);
                                r.setStatus(selected);
                                Platform.runLater(() -> getTableView().refresh());
                            } catch (Exception ex) {
                                LOGGER.warning("Status update failed: " + ex.getMessage());
                                Platform.runLater(() -> combo.setValue(r.getStatus()));
                            }
                        }, "status-update-thread").start();
                    }
                });
                setGraphic(combo);
            }
        };
    }

    private TableCell<DisasterReport, User> buildReportedByCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty) { setText(null); return; }
                User u = getTableView().getItems().get(getIndex()).getReportedBy();
                setText(u != null ? u.getFullName() : "—");
            }
        };
    }

    private TableCell<DisasterReport, LocalDateTime> buildDateCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime dt, boolean empty) {
                super.updateItem(dt, empty);
                setText((empty || dt == null) ? null : dt.format(DATE_FMT));
            }
        };
    }

    private TableCell<DisasterReport, Void> buildActionCell() {
        return new TableCell<>() {
            private final Button btn = new Button("Assess");

            {
                btn.getStyleClass().add("btn-action");
                btn.setOnAction(e -> {
                    DisasterReport r = getTableView().getItems().get(getIndex());
                    DisasterAssessmentController ctrl
                            = SceneManager.switchContentWithController("DisasterAssessmentView");
                    ctrl.loadContext(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
            }
        };
    }
}
