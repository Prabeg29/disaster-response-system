package com.coit20258.drs.server;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.coit20258.drs.dao.*;
import com.coit20258.drs.model.*;
import com.coit20258.drs.dao.ResourceDao;
import com.coit20258.drs.dao.ResourceDaoImpl;

public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;

    // Each handler has its own DAO instances (one per client thread)
    private final UserDao             userDao       = new UserDaoImpl();
    private final DisasterReportDao   reportDao     = new DisasterReportDaoImpl();
    private final DisasterAssessmentDao assessmentDao = new DisasterAssessmentDaoImpl();
    private final DepartmentDao       departmentDao = new DepartmentDaoImpl();
    private final DepartmentUpdateDao updateDao     = new DepartmentUpdateDaoImpl();
    private final EvacuationZoneDao   evacZoneDao   = new EvacuationZoneDaoImpl();
    private final ResourceDao         resourceDao   = new ResourceDaoImpl();

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // Write OOS first to avoid deadlock during stream handshake
        try (ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream  in  = new ObjectInputStream(socket.getInputStream())) {

            DrsRequest  request  = (DrsRequest) in.readObject();
            DrsResponse response = dispatch(request);
            out.writeObject(response);
            out.flush();

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "ClientHandler error for " + socket.getRemoteSocketAddress(), e);
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private DrsResponse dispatch(DrsRequest req) {
        try {
            return switch (req.getCommand()) {

                // ── Auth ───────────────────────────────────────────────────
                case DrsRequest.CMD_LOGIN ->
                    DrsResponse.ok(userDao.login(req.argStr(0), req.argStr(1)).orElse(null));

                case DrsRequest.CMD_REGISTER -> {
                    User u = req.arg(0);
                    yield DrsResponse.ok(userDao.register(u));
                }

                case DrsRequest.CMD_EMAIL_EXISTS ->
                    DrsResponse.ok(userDao.emailExists(req.argStr(0)));

                // ── Reports ────────────────────────────────────────────────
                case DrsRequest.CMD_REPORTS_FIND_ALL ->
                    DrsResponse.ok(reportDao.findAll());

                case DrsRequest.CMD_REPORTS_FIND_BY_DEPT ->
                    DrsResponse.ok(reportDao.findAssignedToDepartment(req.argInt(0)));

                case DrsRequest.CMD_REPORTS_SAVE -> {
                    DisasterReport r = req.arg(0);
                    yield DrsResponse.ok(reportDao.create(r));
                }

                case DrsRequest.CMD_REPORTS_UPDATE_STATUS -> {
                    reportDao.updateStatus(req.argInt(0), req.argStr(1));
                    yield DrsResponse.ok(true);
                }

                // ── Assessments ────────────────────────────────────────────
                case DrsRequest.CMD_ASSESSMENTS_FIND_ALL ->
                    DrsResponse.ok(assessmentDao.findAll());

                case DrsRequest.CMD_ASSESSMENTS_FIND_BY_REPORT ->
                    DrsResponse.ok(assessmentDao.findByReportId(req.argInt(0)).orElse(null));

                case DrsRequest.CMD_ASSESSMENTS_SAVE -> {
                    DisasterAssessment a = req.arg(0);
                    yield DrsResponse.ok(assessmentDao.save(a));
                }

                case DrsRequest.CMD_ASSESSMENTS_UPDATE -> {
                    DisasterAssessment a = req.arg(0);
                    yield DrsResponse.ok(assessmentDao.update(a));
                }

                // ── Departments ────────────────────────────────────────────
                case DrsRequest.CMD_DEPARTMENTS_FIND_ALL ->
                    DrsResponse.ok(departmentDao.findAll());

                case DrsRequest.CMD_DEPARTMENTS_FIND_BY_ID ->
                    DrsResponse.ok(departmentDao.findById(req.argInt(0)).orElse(null));

                // ── Department updates ─────────────────────────────────────
                case DrsRequest.CMD_DEPT_UPDATES_BY_DEPT ->
                    DrsResponse.ok(updateDao.findByDepartmentId(req.argInt(0)));

                case DrsRequest.CMD_DEPT_UPDATES_BY_REPORT ->
                    DrsResponse.ok(updateDao.findByReportId(req.argInt(0)));

                case DrsRequest.CMD_DEPT_UPDATES_SAVE -> {
                    DepartmentUpdate upd = req.arg(0);
                    yield DrsResponse.ok(updateDao.save(upd));
                }

                // ── Evacuation zones ───────────────────────────────────────
                case DrsRequest.CMD_EVAC_ZONES_FIND_ALL ->
                    DrsResponse.ok(evacZoneDao.findAll());

                case DrsRequest.CMD_EVAC_ZONES_FIND_BY_REPORT ->
                    DrsResponse.ok(evacZoneDao.findByReportId(req.argInt(0)));

                case DrsRequest.CMD_EVAC_ZONES_FIND_BY_ID ->
                    DrsResponse.ok(evacZoneDao.findById(req.argInt(0)).orElse(null));

                case DrsRequest.CMD_EVAC_ZONES_SAVE -> {
                    EvacuationZone z = req.arg(0);
                    yield DrsResponse.ok(evacZoneDao.create(z));
                }

                case DrsRequest.CMD_EVAC_ZONES_UPDATE -> {
                    EvacuationZone z = req.arg(0);
                    yield DrsResponse.ok(evacZoneDao.update(z));
                }

                case DrsRequest.CMD_EVAC_ZONES_UPDATE_OCC ->
                    DrsResponse.ok(evacZoneDao.updateOccupancy(
                            req.argInt(0), req.argInt(1), req.argStr(2)));

                case DrsRequest.CMD_EVAC_ZONES_DELETE ->
                    DrsResponse.ok(evacZoneDao.delete(req.argInt(0)));

                // ── Resources ──────────────────────────────────────────────
                case DrsRequest.CMD_RESOURCES_FIND_ALL ->
                    DrsResponse.ok(resourceDao.findAll());

                case DrsRequest.CMD_RESOURCES_FIND_BY_TYPE ->
                    DrsResponse.ok(resourceDao.findByType(req.argStr(0)));

                case DrsRequest.CMD_RESOURCES_FIND_BY_ID ->
                    DrsResponse.ok(resourceDao.findById(req.argInt(0)).orElse(null));

                case DrsRequest.CMD_RESOURCES_SAVE -> {
                    Resource res = req.arg(0);
                    yield DrsResponse.ok(resourceDao.create(res));
                }

                case DrsRequest.CMD_RESOURCES_UPDATE -> {
                    Resource res = req.arg(0);
                    yield DrsResponse.ok(resourceDao.update(res));
                }

                case DrsRequest.CMD_RESOURCES_DELETE ->
                    DrsResponse.ok(resourceDao.delete(req.argInt(0)));

                default -> DrsResponse.error("Unknown command: " + req.getCommand());
            };
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Dispatch error [" + req.getCommand() + "]", e);
            return DrsResponse.error(e.getMessage());
        }
    }
}
