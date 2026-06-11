package com.coit20258.drs.service;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.coit20258.drs.model.*;
import com.coit20258.drs.server.DrsRequest;
import com.coit20258.drs.server.DrsResponse;
import com.coit20258.drs.server.DrsServer;

public final class AppService {

    private static final Logger LOGGER   = Logger.getLogger(AppService.class.getName());
    private static final String HOST     = "localhost";
    private static final int    PORT     = DrsServer.PORT;

    private static final AppService INSTANCE = new AppService();
    private AppService() {}

    public static AppService getInstance() { return INSTANCE; }

    // ── Auth ───────────────────────────────────────────────────────────────
    public Optional<User> login(String email, String password) {
        return Optional.ofNullable(
                send(new DrsRequest(DrsRequest.CMD_LOGIN, email, password)).getData());
    }

    public User register(User user) {
        return send(new DrsRequest(DrsRequest.CMD_REGISTER, user)).getData();
    }

    public boolean emailExists(String email) {
        return send(new DrsRequest(DrsRequest.CMD_EMAIL_EXISTS, email)).getData();
    }

    // ── Reports ────────────────────────────────────────────────────────────
    public List<DisasterReport> findAllReports() {
        return send(new DrsRequest(DrsRequest.CMD_REPORTS_FIND_ALL)).getData();
    }

    public List<DisasterReport> findReportsAssignedToDepartment(int departmentId) {
        return send(new DrsRequest(DrsRequest.CMD_REPORTS_FIND_BY_DEPT, departmentId)).getData();
    }

    public DisasterReport saveReport(DisasterReport r) {
        return send(new DrsRequest(DrsRequest.CMD_REPORTS_SAVE, r)).getData();
    }

    public void updateReportStatus(int id, String status) {
        send(new DrsRequest(DrsRequest.CMD_REPORTS_UPDATE_STATUS, id, status));
    }

    // ── Assessments ────────────────────────────────────────────────────────
    public List<DisasterAssessment> findAllAssessments() {
        return send(new DrsRequest(DrsRequest.CMD_ASSESSMENTS_FIND_ALL)).getData();
    }

    public Optional<DisasterAssessment> findAssessmentByReport(int reportId) {
        return Optional.ofNullable(
                send(new DrsRequest(DrsRequest.CMD_ASSESSMENTS_FIND_BY_REPORT, reportId)).getData());
    }

    public DisasterAssessment saveAssessment(DisasterAssessment a) {
        return send(new DrsRequest(DrsRequest.CMD_ASSESSMENTS_SAVE, a)).getData();
    }

    public boolean updateAssessment(DisasterAssessment a) {
        return send(new DrsRequest(DrsRequest.CMD_ASSESSMENTS_UPDATE, a)).getData();
    }

    // ── Departments ────────────────────────────────────────────────────────
    public List<Department> findAllDepartments() {
        return send(new DrsRequest(DrsRequest.CMD_DEPARTMENTS_FIND_ALL)).getData();
    }

    public Optional<Department> findDepartmentById(int id) {
        return Optional.ofNullable(
                send(new DrsRequest(DrsRequest.CMD_DEPARTMENTS_FIND_BY_ID, id)).getData());
    }

    // ── Department Updates ─────────────────────────────────────────────────
    public List<DepartmentUpdate> findUpdatesByDepartment(int departmentId) {
        return send(new DrsRequest(DrsRequest.CMD_DEPT_UPDATES_BY_DEPT, departmentId)).getData();
    }

    public List<DepartmentUpdate> findUpdatesByReport(int reportId) {
        return send(new DrsRequest(DrsRequest.CMD_DEPT_UPDATES_BY_REPORT, reportId)).getData();
    }

    public DepartmentUpdate saveDepartmentUpdate(DepartmentUpdate u) {
        return send(new DrsRequest(DrsRequest.CMD_DEPT_UPDATES_SAVE, u)).getData();
    }

    // ── Resources ─────────────────────────────────────────────────────────
    public List<Resource> findAllResources() {
        return send(new DrsRequest(DrsRequest.CMD_RESOURCES_FIND_ALL)).getData();
    }

    public List<Resource> findResourcesByType(String resourceType) {
        return send(new DrsRequest(DrsRequest.CMD_RESOURCES_FIND_BY_TYPE, resourceType)).getData();
    }

    public Optional<Resource> findResourceById(int id) {
        return Optional.ofNullable(
                send(new DrsRequest(DrsRequest.CMD_RESOURCES_FIND_BY_ID, id)).getData());
    }

    public Resource saveResource(Resource resource) {
        return send(new DrsRequest(DrsRequest.CMD_RESOURCES_SAVE, resource)).getData();
    }

    public boolean updateResource(Resource resource) {
        return send(new DrsRequest(DrsRequest.CMD_RESOURCES_UPDATE, resource)).getData();
    }

    public boolean deleteResource(int id) {
        return send(new DrsRequest(DrsRequest.CMD_RESOURCES_DELETE, id)).getData();
    }

    // ── Evacuation Zones ───────────────────────────────────────────────────
    public List<EvacuationZone> findAllEvacuationZones() {
        return send(new DrsRequest(DrsRequest.CMD_EVAC_ZONES_FIND_ALL)).getData();
    }

    public List<EvacuationZone> findEvacuationZonesByReport(int reportId) {
        return send(new DrsRequest(DrsRequest.CMD_EVAC_ZONES_FIND_BY_REPORT, reportId)).getData();
    }

    public Optional<EvacuationZone> findEvacuationZoneById(int id) {
        return Optional.ofNullable(
                send(new DrsRequest(DrsRequest.CMD_EVAC_ZONES_FIND_BY_ID, id)).getData());
    }

    public EvacuationZone saveEvacuationZone(EvacuationZone zone) {
        return send(new DrsRequest(DrsRequest.CMD_EVAC_ZONES_SAVE, zone)).getData();
    }

    public boolean updateEvacuationZone(EvacuationZone zone) {
        return send(new DrsRequest(DrsRequest.CMD_EVAC_ZONES_UPDATE, zone)).getData();
    }

    public boolean updateEvacuationZoneOccupancy(int id, int occupancy, String status) {
        return send(new DrsRequest(DrsRequest.CMD_EVAC_ZONES_UPDATE_OCC, id, occupancy, status)).getData();
    }

    public boolean deleteEvacuationZone(int id) {
        return send(new DrsRequest(DrsRequest.CMD_EVAC_ZONES_DELETE, id)).getData();
    }

    // ── Transport ──────────────────────────────────────────────────────────
    private DrsResponse send(DrsRequest request) {
        try (Socket socket = new Socket(HOST, PORT);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(request);
            out.flush();
            DrsResponse response = (DrsResponse) in.readObject();

            if (!response.isSuccess()) {
                throw new RuntimeException("Server error: " + response.getErrorMessage());
            }
            return response;

        } catch (IOException | ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "AppService transport error for " + request.getCommand(), e);
            throw new RuntimeException("Server communication failed: " + e.getMessage(), e);
        }
    }
}
