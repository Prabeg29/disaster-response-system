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
import com.coit20258.drs.util.SceneManager;

public class DisasterAssessmentListController implements Initializable {

    private static final Logger LOGGER = Logger.getLogger(DisasterAssessmentListController.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label bannerLabel;

    @FXML private TableView<DisasterAssessment>              assessmentsTable;
    @FXML private TableColumn<DisasterAssessment, Integer>   colReportId;
    @FXML private TableColumn<DisasterAssessment, String>    colType;
    @FXML private TableColumn<DisasterAssessment, String>    colLocation;
    @FXML private TableColumn<DisasterAssessment, String>    colSeverity;
    @FXML private TableColumn<DisasterAssessment, Integer>   colScore;
    @FXML private TableColumn<DisasterAssessment, String>    colAssessedBy;
    @FXML private TableColumn<DisasterAssessment, String>    colAssessedAt;
    @FXML private TableColumn<DisasterAssessment, Void>      colAction;

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
                List<DisasterAssessment> assessments = service.findAllAssessments();
                Platform.runLater(() -> tableData.setAll(assessments));
            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to load assessments", ex);
                Platform.runLater(() -> showBanner("Could not load assessments: " + ex.getMessage(), false));
            }
        }, "load-assessments-thread").start();
    }

    private void setupTable() {
        colReportId.setCellValueFactory(new PropertyValueFactory<>("reportId"));
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

        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Assess");
            {
                btn.getStyleClass().add("btn-action");
                btn.setOnAction(e -> {
                    DisasterAssessment a = getTableView().getItems().get(getIndex());
                    DisasterAssessmentController ctrl =
                            SceneManager.switchContentWithController("DisasterAssessmentView");
                    ctrl.loadContext(a.getDisasterReport());
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        assessmentsTable.setItems(tableData);
    }

    private void showBanner(String msg, boolean success) {
        bannerLabel.setText(msg);
        bannerLabel.getStyleClass().removeAll("success-label", "validation-label");
        bannerLabel.getStyleClass().add(success ? "success-label" : "validation-label");
        bannerLabel.setManaged(true);
        bannerLabel.setVisible(true);
    }
}
