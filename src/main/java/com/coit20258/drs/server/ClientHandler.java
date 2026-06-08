package com.coit20258.drs.server;

import java.io.*;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.coit20258.drs.dao.*;
import com.coit20258.drs.model.*;

public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;

    // Each handler has its own DAO instances (one per client thread)
    private final UserDao             userDao       = new UserDaoImpl();
    private final DisasterReportDao   reportDao     = new DisasterReportDaoImpl();
    private final DisasterAssessmentDao assessmentDao = new DisasterAssessmentDaoImpl();
    private final DepartmentDao       departmentDao = new DepartmentDaoImpl();
    private final DepartmentUpdateDao updateDao     = new DepartmentUpdateDaoImpl();

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

                default -> DrsResponse.error("Unknown command: " + req.getCommand());
            };
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Dispatch error [" + req.getCommand() + "]", e);
            return DrsResponse.error(e.getMessage());
        }
    }
}
