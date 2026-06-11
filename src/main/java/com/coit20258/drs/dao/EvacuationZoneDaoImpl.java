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

import com.coit20258.drs.model.EvacuationZone;
import com.coit20258.drs.util.Database;

public class EvacuationZoneDaoImpl implements EvacuationZoneDao {

    @Override
    public EvacuationZone create(EvacuationZone zone) {
        final String SQL =
                "INSERT INTO evacuation_zones "
                + "(name, location, capacity, currentOccupancy, status, reportId, notes, createdAt) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, zone.getName());
            ps.setString(2, zone.getLocation());
            ps.setInt(3, zone.getCapacity());
            ps.setInt(4, zone.getCurrentOccupancy());
            ps.setString(5, zone.getStatus());
            ps.setInt(6, zone.getReportId());
            ps.setString(7, zone.getNotes());
            ps.setTimestamp(8, Timestamp.valueOf(zone.getCreatedAt()));

            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new RuntimeException("Create failed: no rows inserted.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    zone.setId(keys.getInt(1));
                }
            }
            return zone;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during create: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EvacuationZone> findAll() {
        final String SQL =
                "SELECT id, name, location, capacity, currentOccupancy, status, reportId, notes, createdAt "
                + "FROM evacuation_zones ORDER BY createdAt DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {

            List<EvacuationZone> zones = new ArrayList<>();
            while (rs.next()) {
                zones.add(mapRow(rs));
            }
            return zones;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during findAll: " + e.getMessage(), e);
        }
    }

    @Override
    public List<EvacuationZone> findByReportId(int reportId) {
        final String SQL =
                "SELECT id, name, location, capacity, currentOccupancy, status, reportId, notes, createdAt "
                + "FROM evacuation_zones WHERE reportId = ? ORDER BY createdAt DESC";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                List<EvacuationZone> zones = new ArrayList<>();
                while (rs.next()) {
                    zones.add(mapRow(rs));
                }
                return zones;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Database error during findByReportId: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<EvacuationZone> findById(int id) {
        final String SQL =
                "SELECT id, name, location, capacity, currentOccupancy, status, reportId, notes, createdAt "
                + "FROM evacuation_zones WHERE id = ?";

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
    public boolean update(EvacuationZone zone) {
        final String SQL =
                "UPDATE evacuation_zones "
                + "SET name = ?, location = ?, capacity = ?, reportId = ?, notes = ? "
                + "WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setString(1, zone.getName());
            ps.setString(2, zone.getLocation());
            ps.setInt(3, zone.getCapacity());
            ps.setInt(4, zone.getReportId());
            ps.setString(5, zone.getNotes());
            ps.setInt(6, zone.getId());
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during update: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean updateOccupancy(int id, int currentOccupancy, String status) {
        final String SQL =
                "UPDATE evacuation_zones SET currentOccupancy = ?, status = ? WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setInt(1, currentOccupancy);
            ps.setString(2, status);
            ps.setInt(3, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during updateOccupancy: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean delete(int id) {
        final String SQL = "DELETE FROM evacuation_zones WHERE id = ?";

        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Database error during delete: " + e.getMessage(), e);
        }
    }

    private EvacuationZone mapRow(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("createdAt");
        return new EvacuationZone(
                rs.getInt("id"),
                rs.getString("name"),
                rs.getString("location"),
                rs.getInt("capacity"),
                rs.getInt("currentOccupancy"),
                rs.getString("status"),
                rs.getInt("reportId"),
                rs.getString("notes"),
                createdAt != null ? createdAt.toLocalDateTime() : null);
    }
}
