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

import com.coit20258.drs.model.DisasterAssessment;
import com.coit20258.drs.model.DisasterReport;
import com.coit20258.drs.service.AppService;

public class DashboardController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(DashboardController.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label bannerLabel;

    // ── Stat card labels ───────────────────────────────────────────────────
    @FXML private Label totalLabel;
    @FXML private Label criticalLabel;
    @FXML private Label highLabel;
    @FXML private Label resolvedLabel;

    // ── Priority table ─────────────────────────────────────────────────────
    @FXML private TableView<DisasterAssessment>              priorityTable;
    @FXML private TableColumn<DisasterAssessment, Integer>   colScore;
    @FXML private TableColumn<DisasterAssessment, String>    colType;
    @FXML private TableColumn<DisasterAssessment, String>    colLocation;
    @FXML private TableColumn<DisasterAssessment, String>    colSeverity;
    @FXML private TableColumn<DisasterAssessment, String>    colAssessedBy;
    @FXML private TableColumn<DisasterAssessment, String>    colAssessedAt;

    private final AppService service = AppService.getInstance();
    private final ObservableList<DisasterAssessment> tableData = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTable();
        loadData();
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            try {
                List<DisasterReport>    reports     = service.findAllReports();
                List<DisasterAssessment> assessments = service.findAllAssessments();

                long total    = reports.size();
                long critical = reports.stream()
                        .filter(r -> DisasterReport.SEVERITY_CRITICAL.equals(r.getSeverityLevel()))
                        .count();
                long high     = reports.stream()
                        .filter(r -> DisasterReport.SEVERITY_HIGH.equals(r.getSeverityLevel()))
                        .count();
                long resolved = reports.stream()
                        .filter(r -> DisasterReport.STATUS_RESOLVED.equals(r.getStatus()))
                        .count();

                List<DisasterAssessment> sorted = assessments.stream()
                        .sorted((a, b) -> Integer.compare(b.getPriorityScore(), a.getPriorityScore()))
                        .toList();

                Platform.runLater(() -> {
                    totalLabel.setText(String.valueOf(total));
                    criticalLabel.setText(String.valueOf(critical));
                    highLabel.setText(String.valueOf(high));
                    resolvedLabel.setText(String.valueOf(resolved));
                    tableData.setAll(sorted);
                });
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load dashboard data", ex);
                Platform.runLater(() -> showBanner("Could not load data: " + ex.getMessage(), false));
            }
        }, "load-dashboard-thread").start();
    }

    private void setupTable() {
        colScore.setCellValueFactory(new PropertyValueFactory<>("priorityScore"));

        colType.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                DisasterReport r = ((DisasterAssessment) getTableRow().getItem()).getDisasterReport();
                setText(r != null ? r.getDisasterType() : "—");
            }
        });

        colLocation.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                DisasterReport r = ((DisasterAssessment) getTableRow().getItem()).getDisasterReport();
                setText(r != null ? r.getLocation() : "—");
            }
        });

        colSeverity.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                setText(((DisasterAssessment) getTableRow().getItem()).getAssessedSeverity());
            }
        });

        colAssessedBy.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                var u = ((DisasterAssessment) getTableRow().getItem()).getAssessedBy();
                setText(u != null ? u.getFullName() : "—");
            }
        });

        colAssessedAt.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) { setText(null); return; }
                var dt = ((DisasterAssessment) getTableRow().getItem()).getAssessedAt();
                setText(dt != null ? dt.format(DATE_FMT) : "—");
            }
        });

        priorityTable.setItems(tableData);
    }

    private void showBanner(String msg, boolean success) {
        bannerLabel.setText(msg);
        bannerLabel.getStyleClass().removeAll("success-label", "validation-label");
        bannerLabel.getStyleClass().add(success ? "success-label" : "validation-label");
        bannerLabel.setManaged(true);
        bannerLabel.setVisible(true);
    }
}
