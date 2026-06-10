package com.coit20258.drs.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.coit20258.drs.model.DepartmentUpdate;
import com.coit20258.drs.util.Database;

public class DepartmentUpdateDaoImpl implements DepartmentUpdateDao {

    private static final Logger LOGGER =
            Logger.getLogger(DepartmentUpdateDaoImpl.class.getName());

    private static final String SELECT_FULL =
            "SELECT du.id, du.reportId, du.departmentId, du.updatedById, "
            + "du.updateText, du.responseStatus, du.updatedAt, "
            + "d.name AS departmentName, "
            + "dr.disasterType AS reportType, dr.location AS reportLocation, "
            + "u.firstName AS updaterFirst, u.lastName AS updaterLast "
            + "FROM department_updates du "
            + "JOIN departments d    ON du.departmentId = d.id "
            + "JOIN disaster_reports dr ON du.reportId   = dr.id "
            + "JOIN users u          ON du.updatedById  = u.id ";

    @Override
    public DepartmentUpdate save(DepartmentUpdate update) {
        final String SQL =
                "INSERT INTO department_updates "
                + "(reportId, departmentId, updatedById, updateText, responseStatus) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, update.getReportId());
            ps.setInt(2, update.getDepartmentId());
            ps.setInt(3, update.getUpdatedById());
            ps.setString(4, update.getUpdateText());
            ps.setString(5, update.getResponseStatus());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) update.setId(keys.getInt(1));
            }

            findById(update.getId()).ifPresent(u -> update.setUpdatedAt(u.getUpdatedAt()));
            return update;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "save() failed", e);
            throw new RuntimeException("Failed to save department update: " + e.getMessage(), e);
        }
    }

    @Override
    public List<DepartmentUpdate> findByReportId(int reportId) {
        return query(SELECT_FULL + "WHERE du.reportId = ? ORDER BY du.updatedAt DESC",
                ps -> ps.setInt(1, reportId));
    }

    @Override
    public List<DepartmentUpdate> findByDepartmentId(int departmentId) {
        return query(SELECT_FULL + "WHERE du.departmentId = ? ORDER BY du.updatedAt DESC",
                ps -> ps.setInt(1, departmentId));
    }

    // ── Private helpers ────────────────────────────────────────────────────
    private java.util.Optional<DepartmentUpdate> findById(int id) {
        final String SQL = SELECT_FULL + "WHERE du.id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return java.util.Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "findById failed", e);
        }
        return java.util.Optional.empty();
    }

    @FunctionalInterface
    private interface Setter { void set(PreparedStatement ps) throws SQLException; }

    private List<DepartmentUpdate> query(String sql, Setter setter) {
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<DepartmentUpdate> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "query() failed", e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    private DepartmentUpdate mapRow(ResultSet rs) throws SQLException {
        DepartmentUpdate u = new DepartmentUpdate();
        u.setId(rs.getInt("id"));
        u.setReportId(rs.getInt("reportId"));
        u.setDepartmentId(rs.getInt("departmentId"));
        u.setUpdatedById(rs.getInt("updatedById"));
        u.setUpdateText(rs.getString("updateText"));
        u.setResponseStatus(rs.getString("responseStatus"));
        Timestamp ts = rs.getTimestamp("updatedAt");
        u.setUpdatedAt(ts != null ? ts.toLocalDateTime() : null);
        u.setDepartmentName(rs.getString("departmentName"));
        u.setReportType(rs.getString("reportType"));
        u.setReportLocation(rs.getString("reportLocation"));
        u.setUpdatedByName(rs.getString("updaterFirst") + " " + rs.getString("updaterLast"));
        return u;
    }
}
