package com.coit20258.drs.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.coit20258.drs.dao.DisasterAssessmentDao;
import com.coit20258.drs.dao.DepartmentDao;
import com.coit20258.drs.dao.DisasterAssessmentDaoImpl;
import com.coit20258.drs.dao.DepartmentDaoImpl;
import com.coit20258.drs.model.Department;
import com.coit20258.drs.model.DisasterAssessment;
import com.coit20258.drs.model.DisasterReport;
import com.coit20258.drs.util.SceneManager;
import com.coit20258.drs.util.SessionContext;

public class DisasterAssessmentController implements Initializable {

    private static final Logger LOGGER =
            Logger.getLogger(DisasterAssessmentController.class.getName());

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Static list of response action strings — no enum needed. */
    private static final List<String> ALL_ACTIONS = List.of(
        "Evacuate Zone",
        "Deploy Fire Brigade",
        "Deploy Search and Rescue",
        "Dispatch Medical Services",
        "Issue Public Warning",
        "Isolate Infrastructure",
        "Deploy Law Enforcement",
        "Activate Emergency Shelter"
    );

    // ── FXML — header ──────────────────────────────────────────────────────
    @FXML private Label headerReportLabel;

    // ── FXML — summary card ────────────────────────────────────────────────
    @FXML private Label summaryType;
    @FXML private Label summaryLocation;
    @FXML private Label summaryReported;
    @FXML private Label summaryDescription;

    // ── FXML — form fields ─────────────────────────────────────────────────
    @FXML private ComboBox<String>         assessedSeverityCombo;
    @FXML private TextField                estimatedAffectedField;
    @FXML private CheckBox                 infrastructureDamagedCheck;
    @FXML private CheckBox                 hazardActiveCheck;
    @FXML private Label                    priorityScoreLabel;
    @FXML private ListView<String>         recommendedActionsList;
    @FXML private ListView<Department>     assignedDepartmentsList;
    @FXML private TextArea                 assessmentNotesArea;

    // ── FXML — feedback ────────────────────────────────────────────────────
    @FXML private Label validationLabel;

    // ── State ──────────────────────────────────────────────────────────────
    private DisasterReport     linkedReport;
    private DisasterAssessment existingAssessment;

    private final DisasterAssessmentDao assessmentDao = new DisasterAssessmentDaoImpl();
    private final DepartmentDao         departmentDao = new DepartmentDaoImpl();

    // ── Initialise ─────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Severity combo
        assessedSeverityCombo.setItems(FXCollections.observableArrayList(
            DisasterReport.SEVERITY_CRITICAL,
            DisasterReport.SEVERITY_HIGH,
            DisasterReport.SEVERITY_MEDIUM,
            DisasterReport.SEVERITY_LOW
        ));

        // Recommended actions — plain strings, multi-select
        recommendedActionsList.setItems(
            FXCollections.observableArrayList(ALL_ACTIONS));
        recommendedActionsList.getSelectionModel()
            .setSelectionMode(SelectionMode.MULTIPLE);

        // Departments — loaded from DB on background thread
        assignedDepartmentsList.getSelectionModel()
            .setSelectionMode(SelectionMode.MULTIPLE);
        loadDepartmentsAsync();

        attachScoreListeners();
        hideValidation();
    }

    // ── Public API — called by DisasterReportListController ────────────────

    /**
     * Receives the report to assess. Loads any existing assessment from the
     * DB and pre-fills the form if found.
     */
    public void loadContext(DisasterReport report) {
        this.linkedReport = report;
        populateSummary(report);

        new Thread(() -> {
            try {
                Optional<DisasterAssessment> found =
                        assessmentDao.findByReportId(report.getId());
                Platform.runLater(() -> {
                    found.ifPresent(a -> {
                        existingAssessment = a;
                        preFillForm(a);
                    });
                    refreshScoreDisplay();
                });
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load existing assessment", ex);
                Platform.runLater(this::refreshScoreDisplay);
            }
        }, "load-assessment-thread").start();
    }

    // ── FXML handlers ──────────────────────────────────────────────────────

    @FXML
    private void handleSubmit() {
        hideValidation();

        String error = validateForm();
        if (error != null) {
            showValidation(error);
            return;
        }

        new Thread(() -> {
            try {
                if (existingAssessment != null) {
                    applyFormToAssessment(existingAssessment);
                    assessmentDao.update(existingAssessment);
                    LOGGER.info("Assessment updated: id=" + existingAssessment.getId());
                } else {
                    DisasterAssessment newAssessment = buildAssessment();
                    assessmentDao.save(newAssessment);
                    LOGGER.info("Assessment saved: id=" + newAssessment.getId());
                }
                Platform.runLater(() ->
                    SceneManager.switchContent("DisasterReportListView"));

            } catch (Exception ex) {
                LOGGER.log(Level.SEVERE, "Failed to save assessment", ex);
                Platform.runLater(() ->
                    showValidation("Save failed: " + ex.getMessage()));
            }
        }, "save-assessment-thread").start();
    }

    @FXML
    private void handleBack() {
        SceneManager.switchContent("DisasterReportListView");
    }

    // ── Private helpers ────────────────────────────────────────────────────

    /**
     * Loads all departments from the DB on a background thread and
     * populates the ListView. Safe to call during initialize() before
     * loadContext() is invoked.
     */
    private void loadDepartmentsAsync() {
        new Thread(() -> {
            try {
                List<Department> departments = departmentDao.findAll();
                Platform.runLater(() ->
                    assignedDepartmentsList.setItems(
                        FXCollections.observableArrayList(departments)));
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Could not load departments", ex);
            }
        }, "load-departments-thread").start();
    }

    private void populateSummary(DisasterReport report) {
        String reportedStr  = report.getReportedAt() != null
                ? report.getReportedAt().format(DATE_FMT) : "Unknown";
        String reporterName = report.getReportedBy() != null
                ? report.getReportedBy().getFullName() : "Unknown";

        headerReportLabel.setText(
            "Report #" + report.getId() + "  |  " + report.getDisasterType());
        summaryType.setText(report.getDisasterType());
        summaryLocation.setText(report.getLocation());
        summaryReported.setText(reporterName + "  ·  " + reportedStr);
        summaryDescription.setText(
            report.getDescription() != null ? report.getDescription() : "—");
    }

    private void preFillForm(DisasterAssessment a) {
        assessedSeverityCombo.setValue(a.getAssessedSeverity());
        estimatedAffectedField.setText(String.valueOf(a.getEstimatedAffected()));
        infrastructureDamagedCheck.setSelected(a.isInfrastructureDamaged());
        hazardActiveCheck.setSelected(a.isHazardActive());
        assessmentNotesArea.setText(
            a.getAssessmentNotes() != null ? a.getAssessmentNotes() : "");

        // Restore selected actions (match by string value)
        MultipleSelectionModel<String> actionSel =
                recommendedActionsList.getSelectionModel();
        for (String action : a.getRecommendedActions()) {
            int idx = recommendedActionsList.getItems().indexOf(action);
            if (idx >= 0) actionSel.select(idx);
        }

        // Restore selected departments (match by departmentId via equals())
        // Wait until the department list is populated, then select
        Platform.runLater(() -> {
            MultipleSelectionModel<Department> deptSel =
                    assignedDepartmentsList.getSelectionModel();
            for (Department dept : a.getAssignedDepartments()) {
                int idx = assignedDepartmentsList.getItems().indexOf(dept);
                if (idx >= 0) deptSel.select(idx);
            }
        });
    }

    private void applyFormToAssessment(DisasterAssessment a) {
        a.setAssessedSeverity(assessedSeverityCombo.getValue());
        a.setEstimatedAffected(parseAffected());
        a.setInfrastructureDamaged(infrastructureDamagedCheck.isSelected());
        a.setHazardActive(hazardActiveCheck.isSelected());
        a.setAssessmentNotes(assessmentNotesArea.getText().trim());
        a.setRecommendedActions(
            new ArrayList<>(recommendedActionsList.getSelectionModel().getSelectedItems()));
        a.setAssignedDepartments(
            new ArrayList<>(assignedDepartmentsList.getSelectionModel().getSelectedItems()));
    }

    private DisasterAssessment buildAssessment() {
        DisasterAssessment a = new DisasterAssessment(
            0,
            SessionContext.getCurrentUser().getId(),
            assessedSeverityCombo.getValue(),
            parseAffected(),
            infrastructureDamagedCheck.isSelected(),
            hazardActiveCheck.isSelected(),
            assessmentNotesArea.getText().trim(),
            new ArrayList<>(recommendedActionsList.getSelectionModel().getSelectedItems()),
            new ArrayList<>(assignedDepartmentsList.getSelectionModel().getSelectedItems())
        );
        a.setReportId(linkedReport.getId());
        return a;
    }

    private void refreshScoreDisplay() {
        DisasterAssessment temp = new DisasterAssessment(
            0, 0,
            assessedSeverityCombo.getValue(),
            parseAffected(),
            infrastructureDamagedCheck.isSelected(),
            hazardActiveCheck.isSelected(),
            "", new ArrayList<>(), new ArrayList<>()
        );
        int score = temp.computePriorityScore();
        priorityScoreLabel.setText(String.valueOf(score));
        priorityScoreLabel.setStyle(
            "-fx-text-fill:" + scoreColour(score) + ";"
          + "-fx-font-weight:bold; -fx-font-size:22px;");
    }

    private void attachScoreListeners() {
        assessedSeverityCombo.valueProperty()
                .addListener((o, old, v) -> refreshScoreDisplay());
        estimatedAffectedField.textProperty()
                .addListener((o, old, v) -> refreshScoreDisplay());
        infrastructureDamagedCheck.selectedProperty()
                .addListener((o, old, v) -> refreshScoreDisplay());
        hazardActiveCheck.selectedProperty()
                .addListener((o, old, v) -> refreshScoreDisplay());
    }

    private String validateForm() {
        if (assessedSeverityCombo.getValue() == null)
            return "Please select an assessed severity level.";
        String affectedText = estimatedAffectedField.getText();
        if (affectedText == null || affectedText.isBlank())
            return "Estimated people affected is required.";
        try {
            if (Integer.parseInt(affectedText.trim()) < 0)
                return "Estimated affected must be 0 or greater.";
        } catch (NumberFormatException e) {
            return "Estimated affected must be a whole number.";
        }
        if (recommendedActionsList.getSelectionModel().getSelectedItems().isEmpty())
            return "Please select at least one recommended action.";
        if (assignedDepartmentsList.getSelectionModel().getSelectedItems().isEmpty())
            return "Please assign at least one department.";
        return null;
    }

    private int parseAffected() {
        try {
            String t = estimatedAffectedField.getText();
            return (t != null && !t.isBlank()) ? Integer.parseInt(t.trim()) : 0;
        } catch (NumberFormatException e) { return 0; }
    }

    private String scoreColour(int score) {
        if (score >= 70) return "#e74c3c";
        if (score >= 45) return "#e67e22";
        if (score >= 20) return "#f39c12";
        return "#27ae60";
    }

    private void showValidation(String message) {
        validationLabel.setText("⚠  " + message);
        validationLabel.setManaged(true);
        validationLabel.setVisible(true);
    }

    private void hideValidation() {
        validationLabel.setManaged(false);
        validationLabel.setVisible(false);
    }
}