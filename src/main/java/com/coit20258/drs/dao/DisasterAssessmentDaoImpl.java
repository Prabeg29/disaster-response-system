package com.coit20258.drs.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.coit20258.drs.model.Department;
import com.coit20258.drs.model.DisasterAssessment;
import com.coit20258.drs.model.DisasterReport;
import com.coit20258.drs.model.User;
import com.coit20258.drs.util.Database;

public class DisasterAssessmentDaoImpl implements DisasterAssessmentDao {

    private static final Logger LOGGER
            = Logger.getLogger(DisasterAssessmentDaoImpl.class.getName());

    private final DepartmentDao departmentDao = new DepartmentDaoImpl();

    private static final String SELECT_FULL
            = "SELECT a.id AS assessmentId, a.reportId, a.assessorId, "
            + "       a.assessedSeverity, a.estimatedAffected, "
            + "       a.isInfrastructureDamaged, a.isHazardActive, "
            + "       a.priorityScore, a.recommendedActions, "
            + "       a.assignedDepartments, a.assessmentNotes, a.assessedAt, "
            + "       u.firstName AS assessorFirst, u.lastName AS assessorLast, "
            + "       u.email AS assessorEmail, u.role AS assessorRole, "
            + "       r.disasterType AS reportDisasterType, r.location AS reportLocation, "
            + "       r.severityLevel AS reportSeverity, r.status AS reportStatus, "
            + "       r.reportedAt AS reportReportedAt, r.reportedById "
            + "FROM disaster_assessments a "
            + "JOIN users u            ON a.assessorId = u.id "
            + "JOIN disaster_reports r ON a.reportId   = r.id ";

    @Override
    public DisasterAssessment save(DisasterAssessment assessment) {
        final String SQL
                = "INSERT INTO disaster_assessments "
                + "(reportId, assessorId, assessedSeverity, estimatedAffected, "
                + " isInfrastructureDamaged, isHazardActive, priorityScore, "
                + " recommendedActions, assignedDepartments, assessmentNotes) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            bindParams(ps, assessment);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    assessment.setId(keys.getInt(1));
                }
            }

            findById(assessment.getId())
                    .ifPresent(a -> assessment.setAssessedAt(a.getAssessedAt()));

            LOGGER.info("Assessment saved: id=" + assessment.getId());
            return assessment;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "save() failed", e);
            throw new RuntimeException("Failed to save assessment: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(DisasterAssessment assessment) {
        final String SQL
                = "UPDATE disaster_assessments SET "
                + "assessedSeverity=?, estimatedAffected=?, "
                + "isInfrastructureDamaged=?, isHazardActive=?, priorityScore=?, "
                + "recommendedActions=?, assignedDepartments=?, assessmentNotes=? "
                + "WHERE id=?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setString(1, assessment.getAssessedSeverity());
            ps.setInt(2, assessment.getEstimatedAffected());
            ps.setBoolean(3, assessment.isInfrastructureDamaged());
            ps.setBoolean(4, assessment.isHazardActive());
            ps.setInt(5, assessment.computePriorityScore());
            ps.setString(6, serializeActions(assessment.getRecommendedActions()));
            ps.setString(7, serializeDepartments(assessment.getAssignedDepartments()));
            ps.setString(8, assessment.getAssessmentNotes());
            ps.setInt(9, assessment.getId());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "update() failed", e);
            throw new RuntimeException("Failed to update assessment: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<DisasterAssessment> findByReportId(int reportId) {
        return querySingle(SELECT_FULL + "WHERE a.reportId = ?",
                ps -> ps.setInt(1, reportId));
    }

    @Override
    public Optional<DisasterAssessment> findById(int assessmentId) {
        return querySingle(SELECT_FULL + "WHERE a.id = ?",
                ps -> ps.setInt(1, assessmentId));
    }

    @Override
    public List<DisasterAssessment> findAll() {
        final String SQL = SELECT_FULL + "ORDER BY a.assessedAt DESC";
        Map<Integer, Department> deptMap = buildDepartmentMap();

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {
            List<DisasterAssessment> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapRow(rs, deptMap));
            }
            return list;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findAll() failed", e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────
    @FunctionalInterface
    private interface PsSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private Optional<DisasterAssessment> querySingle(String sql, PsSetter setter) {
        Map<Integer, Department> deptMap = buildDepartmentMap();
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs, deptMap));
                }
            }
            return Optional.empty();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "querySingle() failed", e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    private Map<Integer, Department> buildDepartmentMap() {
        return departmentDao.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Function.identity()));
    }

    private void bindParams(PreparedStatement ps, DisasterAssessment a) throws SQLException {
        ps.setInt(1, a.getReportId());
        ps.setInt(2, a.getAssessorId());
        ps.setString(3, a.getAssessedSeverity());
        ps.setInt(4, a.getEstimatedAffected());
        ps.setBoolean(5, a.isInfrastructureDamaged());
        ps.setBoolean(6, a.isHazardActive());
        ps.setInt(7, a.computePriorityScore());
        ps.setString(8, serializeActions(a.getRecommendedActions()));
        ps.setString(9, serializeDepartments(a.getAssignedDepartments()));
        ps.setString(10, a.getAssessmentNotes());
    }

    private DisasterAssessment mapRow(ResultSet rs, Map<Integer, Department> deptMap) throws SQLException {
        User assessor = new User(
                rs.getString("assessorFirst"),
                rs.getString("assessorLast"),
                rs.getString("assessorEmail"),
                "",
                rs.getString("assessorRole")
        );

        Timestamp reportedTs = rs.getTimestamp("reportReportedAt");
        DisasterReport report = new DisasterReport();
        report.setId(rs.getInt("reportId"));
        report.setDisasterType(rs.getString("reportDisasterType"));
        report.setLocation(rs.getString("reportLocation"));
        report.setSeverityLevel(rs.getString("reportSeverity"));
        report.setStatus(rs.getString("reportStatus"));
        report.setReportedAt(reportedTs != null ? reportedTs.toLocalDateTime() : null);

        Timestamp assessedTs = rs.getTimestamp("assessedAt");

        return new DisasterAssessment(
                rs.getInt("assessmentId"),
                rs.getInt("reportId"),
                report,
                rs.getInt("assessorId"),
                assessor,
                rs.getString("assessedSeverity"),
                rs.getInt("estimatedAffected"),
                rs.getBoolean("isInfrastructureDamaged"),
                rs.getBoolean("isHazardActive"),
                rs.getInt("priorityScore"),
                rs.getString("assessmentNotes"),
                deserializeActions(rs.getString("recommendedActions")),
                deserializeDepartments(rs.getString("assignedDepartments"), deptMap),
                assessedTs != null ? assessedTs.toLocalDateTime() : null
        );
    }

    // ── Serialisation helpers ──────────────────────────────────────────────
    private String serializeActions(List<String> actions) {
        if (actions == null || actions.isEmpty()) return "";
        return String.join(",", actions);
    }

    private String serializeDepartments(List<Department> depts) {
        if (depts == null || depts.isEmpty()) return "";
        return depts.stream()
                .map(d -> String.valueOf(d.getId()))
                .collect(Collectors.joining(","));
    }

    private List<String> deserializeActions(String csv) {
        List<String> list = new ArrayList<>();
        if (csv == null || csv.isBlank()) return list;
        Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(list::add);
        return list;
    }

    private List<Department> deserializeDepartments(String csv, Map<Integer, Department> deptMap) {
        List<Department> list = new ArrayList<>();
        if (csv == null || csv.isBlank()) return list;
        for (String token : csv.split(",")) {
            try {
                Department d = deptMap.get(Integer.parseInt(token.trim()));
                if (d != null) list.add(d);
            } catch (NumberFormatException ignored) {
            }
        }
        return list;
    }
}
