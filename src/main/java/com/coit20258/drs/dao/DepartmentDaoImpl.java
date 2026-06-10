package com.coit20258.drs.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.coit20258.drs.dao.DepartmentDao;
import com.coit20258.drs.model.Department;
import com.coit20258.drs.util.Database;

public class DepartmentDaoImpl implements DepartmentDao {

    private static final Logger LOGGER =
            Logger.getLogger(DepartmentDaoImpl.class.getName());

    @Override
    public List<Department> findAll() {
        final String SQL =
            "SELECT * FROM departments WHERE isActive = 1 ORDER BY name ASC";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL);
             ResultSet rs = ps.executeQuery()) {
            List<Department> list = new ArrayList<>();
            while (rs.next()) list.add(mapRow(rs));
            return list;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findAll() failed", e);
            throw new RuntimeException("Failed to fetch departments: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Department> findById(int departmentId) {
        final String SQL = "SELECT * FROM departments WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setInt(1, departmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findById() failed", e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Department> findByName(String name) {
        final String SQL = "SELECT * FROM departments WHERE name = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "findByName() failed", e);
            throw new RuntimeException("Database error: " + e.getMessage(), e);
        }
    }

    private Department mapRow(ResultSet rs) throws SQLException {
        return new Department(
            rs.getInt("id"),
            rs.getString("name"),
            rs.getString("contactEmail"),
            rs.getString("contactPhone"),
            rs.getBoolean("isActive")
        );
    }
}