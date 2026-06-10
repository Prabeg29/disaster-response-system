package com.coit20258.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DisasterAssessment implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Scoring constants ──────────────────────────────────────────────────
    private static final int SCORE_SEVERITY_CRITICAL = 40;
    private static final int SCORE_SEVERITY_HIGH = 30;
    private static final int SCORE_SEVERITY_MEDIUM = 20;
    private static final int SCORE_SEVERITY_LOW = 10;
    private static final int SCORE_PER_100_AFFECTED = 5;
    private static final int SCORE_AFFECTED_CAP = 30;
    private static final int SCORE_INFRASTRUCTURE_DAMAGED = 15;
    private static final int SCORE_HAZARD_ACTIVE = 15;

    // ── Fields ─────────────────────────────────────────────────────────────
    private int id;
    private int reportId;
    private DisasterReport disasterReport;
    private int assessorId;
    private User assessedBy;
    private String assessedSeverity;
    private int estimatedAffected;
    private boolean isInfrastructureDamaged;
    private boolean isHazardActive;
    private int priorityScore;
    private List<String> recommendedActions;
    private List<Department> assignedDepartments;
    private String assessmentNotes;
    private LocalDateTime assessedAt;

    public DisasterAssessment() {
        this.recommendedActions = new ArrayList<>();
        this.assignedDepartments = new ArrayList<>();
    }

    public DisasterAssessment(
            int id,
            int assessorId,
            String assessedSeverity,
            int estimatedAffected,
            boolean isInfrastructureDamaged,
            boolean isHazardActive,
            String assessmentNotes,
            List<String> recommendedActions,
            List<Department> assignedDepartments) {

        this.id = id;
        this.assessorId = assessorId;
        this.assessedSeverity = assessedSeverity;
        this.estimatedAffected = estimatedAffected;
        this.isInfrastructureDamaged = isInfrastructureDamaged;
        this.isHazardActive = isHazardActive;
        this.assessmentNotes = assessmentNotes;
        this.recommendedActions = recommendedActions != null
                ? recommendedActions : new ArrayList<>();
        this.assignedDepartments = assignedDepartments != null
                ? assignedDepartments : new ArrayList<>();
        this.priorityScore = computePriorityScore();
    }

    public DisasterAssessment(
            int id,
            int reportId,
            DisasterReport disasterReport,
            int assessorId,
            User assessedBy,
            String assessedSeverity,
            int estimatedAffected,
            boolean isInfrastructureDamaged,
            boolean isHazardActive,
            int priorityScore,
            String assessmentNotes,
            List<String> recommendedActions,
            List<Department> assignedDepartments,
            LocalDateTime assessedAt) {

        this.id = id;
        this.reportId = reportId;
        this.disasterReport = disasterReport;
        this.assessorId = assessorId;
        this.assessedBy = assessedBy;
        this.assessedSeverity = assessedSeverity;
        this.estimatedAffected = estimatedAffected;
        this.isInfrastructureDamaged = isInfrastructureDamaged;
        this.isHazardActive = isHazardActive;
        this.priorityScore = priorityScore;
        this.assessmentNotes = assessmentNotes;
        this.recommendedActions = recommendedActions != null
                ? recommendedActions : new ArrayList<>();
        this.assignedDepartments = assignedDepartments != null
                ? assignedDepartments : new ArrayList<>();
        this.assessedAt = assessedAt;
    }

    // ── Scoring ────────────────────────────────────────────────────────────
    public int computePriorityScore() {
        int score = 0;

        if (this.assessedSeverity != null) {
            score += switch (this.assessedSeverity) {
                case DisasterReport.SEVERITY_CRITICAL ->
                    SCORE_SEVERITY_CRITICAL;
                case DisasterReport.SEVERITY_HIGH ->
                    SCORE_SEVERITY_HIGH;
                case DisasterReport.SEVERITY_MEDIUM ->
                    SCORE_SEVERITY_MEDIUM;
                case DisasterReport.SEVERITY_LOW ->
                    SCORE_SEVERITY_LOW;
                default -> throw new IllegalStateException("Unexpected value: " + (this.assessedSeverity));
            };
        }

        int affectedContribution = (this.estimatedAffected / 100) * SCORE_PER_100_AFFECTED;
        score += Math.min(affectedContribution, SCORE_AFFECTED_CAP);

        if (this.isInfrastructureDamaged) {
            score += SCORE_INFRASTRUCTURE_DAMAGED;
        }
        if (this.isHazardActive) {
            score += SCORE_HAZARD_ACTIVE;
        }

        return score;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public DisasterReport getDisasterReport() {
        return disasterReport;
    }

    public void setDisasterReport(DisasterReport r) {
        this.disasterReport = r;
    }

    public int getAssessorId() {
        return assessorId;
    }

    public void setAssessorId(int id) {
        this.assessorId = id;
    }

    public User getAssessedBy() {
        return assessedBy;
    }

    public void setAssessedBy(User u) {
        this.assessedBy = u;
    }

    public String getAssessedSeverity() {
        return assessedSeverity;
    }

    public void setAssessedSeverity(String s) {
        this.assessedSeverity = s;
        this.priorityScore = computePriorityScore();
    }

    public int getEstimatedAffected() {
        return estimatedAffected;
    }

    public void setEstimatedAffected(int n) {
        this.estimatedAffected = n;
        this.priorityScore = computePriorityScore();
    }

    public boolean isInfrastructureDamaged() {
        return isInfrastructureDamaged;
    }

    public void setInfrastructureDamaged(boolean b) {
        this.isInfrastructureDamaged = b;
        this.priorityScore = computePriorityScore();
    }

    public boolean isHazardActive() {
        return isHazardActive;
    }

    public void setHazardActive(boolean b) {
        this.isHazardActive = b;
        this.priorityScore = computePriorityScore();
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(int s) {
        this.priorityScore = s;
    }

    public String getAssessmentNotes() {
        return assessmentNotes;
    }

    public void setAssessmentNotes(String n) {
        this.assessmentNotes = n;
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public void setRecommendedActions(List<String> a) {
        this.recommendedActions = a;
    }

    public List<Department> getAssignedDepartments() {
        return assignedDepartments;
    }

    public void setAssignedDepartments(List<Department> d) {
        this.assignedDepartments = d;
    }

    public LocalDateTime getAssessedAt() {
        return assessedAt;
    }

    public void setAssessedAt(LocalDateTime t) {
        this.assessedAt = t;
    }

    // ── Utility ────────────────────────────────────────────────────────────
    public void addRecommendedAction(String action) {
        if (action != null && !this.recommendedActions.contains(action)) {
            this.recommendedActions.add(action);
        }
    }

    public void addAssignedDepartment(Department department) {
        if (department != null && !this.assignedDepartments.contains(department)) {
            this.assignedDepartments.add(department);
        }
    }

    @Override
    public String toString() {
        return String.format("[%d] → Report: %d | Severity: %s | Score: %d | Depts: %s",
                id, reportId, assessedSeverity, priorityScore, assignedDepartments);
    }
}
