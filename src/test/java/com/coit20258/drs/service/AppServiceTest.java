package com.coit20258.drs.service;

import com.coit20258.drs.model.*;
import com.coit20258.drs.server.DrsRequest;
import com.coit20258.drs.server.DrsResponse;
import com.coit20258.drs.server.StubDrsServer;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AppService.
 *
 * Each test starts a StubDrsServer on a random free port and redirects
 * AppService to it via the package-private useEndpoint() method.  No real
 * DrsServer or database is needed; the stub verifies that the correct command
 * and arguments are sent and that the response is correctly deserialised.
 */
class AppServiceTest {

    private StubDrsServer stub;
    private final AppService service = AppService.getInstance();

    @AfterEach
    void tearDown() {
        if (stub != null) stub.stop();
        AppService.resetEndpoint();
    }

    private void useStub(Function<DrsRequest, DrsResponse> handler) throws IOException {
        stub = new StubDrsServer(handler);
        int port = stub.start();
        AppService.useEndpoint("localhost", port);
    }

    // ── Auth ─────────────────────────────────────────────────────────────────

    @Test
    void login_returnsUser_whenCredentialsMatch() throws IOException {
        User user = stubUser(1, "john@test.com");
        useStub(req -> {
            assertEquals(DrsRequest.CMD_LOGIN, req.getCommand());
            assertEquals("john@test.com", req.argStr(0));
            assertEquals("secret", req.argStr(1));
            return DrsResponse.ok(user);
        });

        Optional<User> result = service.login("john@test.com", "secret");
        assertTrue(result.isPresent());
        assertEquals(1, result.get().getId());
        assertEquals("John", result.get().getFirstName());
    }

    @Test
    void login_returnsEmpty_whenCredentialsDontMatch() throws IOException {
        useStub(req -> DrsResponse.ok(null));

        assertFalse(service.login("bad@test.com", "wrong").isPresent());
    }

    @Test
    void register_returnsPersistedUser() throws IOException {
        User saved = stubUser(5, "jane@test.com");
        useStub(req -> {
            assertEquals(DrsRequest.CMD_REGISTER, req.getCommand());
            return DrsResponse.ok(saved);
        });

        User result = service.register(new User("Jane", "Doe", "jane@test.com", "hash", User.ROLE_OPERATOR));
        assertEquals(5, result.getId());
        assertEquals("jane@test.com", result.getEmail());
    }

    @Test
    void emailExists_returnsTrue_whenInUse() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_EMAIL_EXISTS, req.getCommand());
            assertEquals("used@test.com", req.argStr(0));
            return DrsResponse.ok(true);
        });

        assertTrue(service.emailExists("used@test.com"));
    }

    @Test
    void emailExists_returnsFalse_whenAvailable() throws IOException {
        useStub(req -> DrsResponse.ok(false));

        assertFalse(service.emailExists("free@test.com"));
    }

    // ── Reports ──────────────────────────────────────────────────────────────

    @Test
    void findAllReports_returnsList() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_REPORTS_FIND_ALL, req.getCommand());
            return DrsResponse.ok(List.of(stubReport(1), stubReport(2)));
        });

        List<DisasterReport> result = service.findAllReports();
        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getId());
        assertEquals(2, result.get(1).getId());
    }

    @Test
    void findReportsAssignedToDepartment_passesDeptId() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_REPORTS_FIND_BY_DEPT, req.getCommand());
            assertEquals(3, req.argInt(0));
            return DrsResponse.ok(List.of(stubReport(1)));
        });

        List<DisasterReport> result = service.findReportsAssignedToDepartment(3);
        assertEquals(1, result.size());
    }

    @Test
    void saveReport_returnsPersistedReport() throws IOException {
        DisasterReport saved = stubReport(7);
        useStub(req -> {
            assertEquals(DrsRequest.CMD_REPORTS_SAVE, req.getCommand());
            return DrsResponse.ok(saved);
        });

        DisasterReport result = service.saveReport(stubReport(0));
        assertEquals(7, result.getId());
        assertEquals("FLOOD", result.getDisasterType());
    }

    @Test
    void updateReportStatus_sendsIdAndStatus() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_REPORTS_UPDATE_STATUS, req.getCommand());
            assertEquals(4, req.argInt(0));
            assertEquals(DisasterReport.STATUS_RESOLVED, req.argStr(1));
            return DrsResponse.ok(null);
        });

        service.updateReportStatus(4, DisasterReport.STATUS_RESOLVED);
    }

    // ── Assessments ──────────────────────────────────────────────────────────

    @Test
    void findAllAssessments_returnsList() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_ASSESSMENTS_FIND_ALL, req.getCommand());
            return DrsResponse.ok(List.of(stubAssessment(1), stubAssessment(2)));
        });

        assertEquals(2, service.findAllAssessments().size());
    }

    @Test
    void findAssessmentByReport_returnsPresent_whenFound() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_ASSESSMENTS_FIND_BY_REPORT, req.getCommand());
            assertEquals(9, req.argInt(0));
            return DrsResponse.ok(stubAssessment(3));
        });

        Optional<DisasterAssessment> result = service.findAssessmentByReport(9);
        assertTrue(result.isPresent());
        assertEquals(3, result.get().getId());
    }

    @Test
    void findAssessmentByReport_returnsEmpty_whenNotFound() throws IOException {
        useStub(req -> DrsResponse.ok(null));

        assertFalse(service.findAssessmentByReport(99).isPresent());
    }

    @Test
    void saveAssessment_returnsPersistedAssessment() throws IOException {
        DisasterAssessment saved = stubAssessment(11);
        useStub(req -> {
            assertEquals(DrsRequest.CMD_ASSESSMENTS_SAVE, req.getCommand());
            return DrsResponse.ok(saved);
        });

        DisasterAssessment result = service.saveAssessment(stubAssessment(0));
        assertEquals(11, result.getId());
    }

    @Test
    void updateAssessment_returnsTrue() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_ASSESSMENTS_UPDATE, req.getCommand());
            return DrsResponse.ok(true);
        });

        assertTrue(service.updateAssessment(stubAssessment(5)));
    }

    // ── Departments ───────────────────────────────────────────────────────────

    @Test
    void findAllDepartments_returnsList() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_DEPARTMENTS_FIND_ALL, req.getCommand());
            return DrsResponse.ok(List.of(stubDept(1), stubDept(2)));
        });

        assertEquals(2, service.findAllDepartments().size());
    }

    @Test
    void findDepartmentById_returnsPresent_whenFound() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_DEPARTMENTS_FIND_BY_ID, req.getCommand());
            assertEquals(2, req.argInt(0));
            return DrsResponse.ok(stubDept(2));
        });

        Optional<Department> result = service.findDepartmentById(2);
        assertTrue(result.isPresent());
        assertEquals(2, result.get().getId());
    }

    @Test
    void findDepartmentById_returnsEmpty_whenNotFound() throws IOException {
        useStub(req -> DrsResponse.ok(null));

        assertFalse(service.findDepartmentById(99).isPresent());
    }

    // ── Department Updates ────────────────────────────────────────────────────

    @Test
    void findUpdatesByDepartment_passesDeptId() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_DEPT_UPDATES_BY_DEPT, req.getCommand());
            assertEquals(5, req.argInt(0));
            return DrsResponse.ok(List.of());
        });

        assertTrue(service.findUpdatesByDepartment(5).isEmpty());
    }

    @Test
    void findUpdatesByReport_passesReportId() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_DEPT_UPDATES_BY_REPORT, req.getCommand());
            assertEquals(8, req.argInt(0));
            return DrsResponse.ok(List.of(stubDeptUpdate(20)));
        });

        List<DepartmentUpdate> result = service.findUpdatesByReport(8);
        assertEquals(1, result.size());
        assertEquals(20, result.get(0).getId());
    }

    @Test
    void saveDepartmentUpdate_returnsPersistedUpdate() throws IOException {
        DepartmentUpdate saved = stubDeptUpdate(20);
        useStub(req -> {
            assertEquals(DrsRequest.CMD_DEPT_UPDATES_SAVE, req.getCommand());
            return DrsResponse.ok(saved);
        });

        DepartmentUpdate result = service.saveDepartmentUpdate(
                new DepartmentUpdate(1, 2, 3, "On site", DepartmentUpdate.STATUS_RESPONDING));
        assertEquals(20, result.getId());
    }

    // ── Evacuation Zones ──────────────────────────────────────────────────────

    @Test
    void findAllEvacuationZones_returnsList() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_EVAC_ZONES_FIND_ALL, req.getCommand());
            return DrsResponse.ok(List.of(stubZone(1), stubZone(2)));
        });

        assertEquals(2, service.findAllEvacuationZones().size());
    }

    @Test
    void findEvacuationZonesByReport_passesReportId() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_EVAC_ZONES_FIND_BY_REPORT, req.getCommand());
            assertEquals(3, req.argInt(0));
            return DrsResponse.ok(List.of(stubZone(1)));
        });

        assertEquals(1, service.findEvacuationZonesByReport(3).size());
    }

    @Test
    void findEvacuationZoneById_returnsPresent_whenFound() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_EVAC_ZONES_FIND_BY_ID, req.getCommand());
            assertEquals(7, req.argInt(0));
            return DrsResponse.ok(stubZone(7));
        });

        Optional<EvacuationZone> result = service.findEvacuationZoneById(7);
        assertTrue(result.isPresent());
        assertEquals(7, result.get().getId());
    }

    @Test
    void findEvacuationZoneById_returnsEmpty_whenNotFound() throws IOException {
        useStub(req -> DrsResponse.ok(null));

        assertFalse(service.findEvacuationZoneById(99).isPresent());
    }

    @Test
    void saveEvacuationZone_returnsPersistedZone() throws IOException {
        EvacuationZone saved = stubZone(14);
        useStub(req -> {
            assertEquals(DrsRequest.CMD_EVAC_ZONES_SAVE, req.getCommand());
            return DrsResponse.ok(saved);
        });

        EvacuationZone result = service.saveEvacuationZone(stubZone(0));
        assertEquals(14, result.getId());
        assertEquals("Zone 14", result.getName());
    }

    @Test
    void updateEvacuationZone_returnsTrue() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_EVAC_ZONES_UPDATE, req.getCommand());
            return DrsResponse.ok(true);
        });

        assertTrue(service.updateEvacuationZone(stubZone(5)));
    }

    @Test
    void updateEvacuationZoneOccupancy_sendsIdOccupancyAndStatus() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_EVAC_ZONES_UPDATE_OCC, req.getCommand());
            assertEquals(6,    req.argInt(0));
            assertEquals(120,  req.argInt(1));
            assertEquals(EvacuationZone.STATUS_FULL, req.argStr(2));
            return DrsResponse.ok(true);
        });

        assertTrue(service.updateEvacuationZoneOccupancy(6, 120, EvacuationZone.STATUS_FULL));
    }

    @Test
    void deleteEvacuationZone_sendsIdAndReturnsTrue() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_EVAC_ZONES_DELETE, req.getCommand());
            assertEquals(9, req.argInt(0));
            return DrsResponse.ok(true);
        });

        assertTrue(service.deleteEvacuationZone(9));
    }

    // ── Resources ─────────────────────────────────────────────────────────────

    @Test
    void findAllResources_returnsList() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_RESOURCES_FIND_ALL, req.getCommand());
            return DrsResponse.ok(List.of(stubResource(1), stubResource(2)));
        });

        assertEquals(2, service.findAllResources().size());
    }

    @Test
    void findResourcesByType_passesType() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_RESOURCES_FIND_BY_TYPE, req.getCommand());
            assertEquals(Resource.TYPE_VEHICLE, req.argStr(0));
            return DrsResponse.ok(List.of(stubResource(1)));
        });

        List<Resource> result = service.findResourcesByType(Resource.TYPE_VEHICLE);
        assertEquals(1, result.size());
        assertEquals(Resource.TYPE_VEHICLE, result.get(0).getResourceType());
    }

    @Test
    void findResourceById_returnsPresent_whenFound() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_RESOURCES_FIND_BY_ID, req.getCommand());
            assertEquals(3, req.argInt(0));
            return DrsResponse.ok(stubResource(3));
        });

        Optional<Resource> result = service.findResourceById(3);
        assertTrue(result.isPresent());
        assertEquals(3, result.get().getId());
    }

    @Test
    void findResourceById_returnsEmpty_whenNotFound() throws IOException {
        useStub(req -> DrsResponse.ok(null));

        assertFalse(service.findResourceById(99).isPresent());
    }

    @Test
    void saveResource_returnsPersistedResource() throws IOException {
        Resource saved = stubResource(10);
        useStub(req -> {
            assertEquals(DrsRequest.CMD_RESOURCES_SAVE, req.getCommand());
            return DrsResponse.ok(saved);
        });

        Resource result = service.saveResource(stubResource(0));
        assertEquals(10, result.getId());
        assertEquals("Resource 10", result.getName());
    }

    @Test
    void updateResource_returnsTrue() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_RESOURCES_UPDATE, req.getCommand());
            return DrsResponse.ok(true);
        });

        assertTrue(service.updateResource(stubResource(3)));
    }

    @Test
    void deleteResource_sendsIdAndReturnsTrue() throws IOException {
        useStub(req -> {
            assertEquals(DrsRequest.CMD_RESOURCES_DELETE, req.getCommand());
            assertEquals(12, req.argInt(0));
            return DrsResponse.ok(true);
        });

        assertTrue(service.deleteResource(12));
    }

    // ── Error propagation ─────────────────────────────────────────────────────

    @Test
    void send_throwsRuntimeException_whenServerReturnsError() throws IOException {
        useStub(req -> DrsResponse.error("Database connection failed"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.findAllReports());
        assertTrue(ex.getMessage().contains("Server error"));
        assertTrue(ex.getMessage().contains("Database connection failed"));
    }

    @Test
    void send_throwsRuntimeException_whenConnectionRefused() {
        // Point at a port with no listener — no stub started
        AppService.useEndpoint("localhost", 19999);

        assertThrows(RuntimeException.class, () -> service.findAllReports());
    }

    // ── Test data factories ───────────────────────────────────────────────────

    private static User stubUser(int id, String email) {
        return new User(id, "John", "Doe", email, "hash",
                User.ROLE_REPORTER, true, LocalDateTime.now(), null);
    }

    private static DisasterReport stubReport(int id) {
        return new DisasterReport(id, "FLOOD", "Test City",
                DisasterReport.SEVERITY_HIGH, "Description",
                DisasterReport.STATUS_REPORTED, null, LocalDateTime.now());
    }

    private static DisasterAssessment stubAssessment(int id) {
        DisasterAssessment a = new DisasterAssessment();
        a.setId(id);
        a.setReportId(1);
        a.setAssessedSeverity(DisasterReport.SEVERITY_HIGH);
        a.setEstimatedAffected(200);
        a.setAssessedAt(LocalDateTime.now());
        return a;
    }

    private static Department stubDept(int id) {
        return new Department(id, "Fire Dept " + id, "fire@test.com", "0400000000", true);
    }

    private static DepartmentUpdate stubDeptUpdate(int id) {
        DepartmentUpdate u = new DepartmentUpdate(1, 2, 3, "On site",
                DepartmentUpdate.STATUS_RESPONDING);
        u.setId(id);
        return u;
    }

    private static EvacuationZone stubZone(int id) {
        return new EvacuationZone(id, "Zone " + id, "Location " + id,
                500, 0, EvacuationZone.STATUS_ACTIVE, 1, null, LocalDateTime.now());
    }

    private static Resource stubResource(int id) {
        return new Resource(id, "Resource " + id, Resource.TYPE_VEHICLE,
                10, 8, Resource.STATUS_AVAILABLE, 0, null, LocalDateTime.now());
    }
}
