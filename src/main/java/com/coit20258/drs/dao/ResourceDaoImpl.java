package com.coit20258.drs.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.coit20258.drs.model.Resource;
import com.coit20258.drs.util.Database;

public class ResourceDaoImpl implements ResourceDao {

    @Override
    public Resource create(Resource resource) {
        final String SQL =
                "INSERT INTO resources "
                + "(name, resourceType, totalQuantity, availableQuantity, "
                + " status, assignedReportId, notes, updatedAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, resource.getName());
            ps.setString(2, resource.getResourceType());
            ps.setInt(3, resource.getTotalQuantity());
            ps.setInt(4, resource.getAvailableQuantity());
            ps.setString(5, resource.getStatus());
            ps.setInt(6, resource.getAssignedReportId());
            ps.setString(7, resource.getNotes());
            ps.setTimestamp(8, Timestamp.valueOf(resource.getUpdatedAt()));

            int rows = ps.executeUpdate();
            if (rows == 0) throw new RuntimeException("Create failed: no rows inserted.");

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) resource.setId(keys.getInt(1));
            }
            return resource;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during create: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Resource> findAll() {
        final String SQL =
                "SELECT id, name, resourceType, totalQuantity, availableQuantity, "
                + "status, assignedReportId, notes, updatedAt "
                + "FROM resources ORDER BY updatedAt DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {

            List<Resource> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during findAll: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Resource> findById(int id) {
        final String SQL =
                "SELECT id, name, resourceType, totalQuantity, availableQuantity, "
                + "status, assignedReportId, notes, updatedAt "
                + "FROM resources WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error during findById: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Resource> findByType(String resourceType) {
        final String SQL =
                "SELECT id, name, resourceType, totalQuantity, availableQuantity, "
                + "status, assignedReportId, notes, updatedAt "
                + "FROM resources WHERE resourceType = ? ORDER BY updatedAt DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setString(1, resourceType);
            try (ResultSet rs = ps.executeQuery()) {
                List<Resource> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error during findByType: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Resource resource) {
        final String SQL =
                "UPDATE resources SET name=?, resourceType=?, totalQuantity=?, "
                + "availableQuantity=?, status=?, assignedReportId=?, notes=? "
                + "WHERE id=?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setString(1, resource.getName());
            ps.setString(2, resource.getResourceType());
            ps.setInt(3, resource.getTotalQuantity());
            ps.setInt(4, resource.getAvailableQuantity());
            ps.setString(5, resource.getStatus());
            ps.setInt(6, resource.getAssignedReportId());
            ps.setString(7, resource.getNotes());
            ps.setInt(8, resource.getId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during update: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        final String SQL = "DELETE FROM resources WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during delete: " + e.getMessage(), e);
        }
    }

    private Resource mapRow(ResultSet rs) throws SQLException {
        Timestamp updatedAt = rs.getTimestamp("updatedAt");
        return new Resource(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("resourceType"),
                rs.getInt("totalQuantity"),
                rs.getInt("availableQuantity"),
                rs.getString("status"),
                rs.getInt("assignedReportId"),
                rs.getString("notes"),
                updatedAt != null ? updatedAt.toLocalDateTime() : null);
    }
}
