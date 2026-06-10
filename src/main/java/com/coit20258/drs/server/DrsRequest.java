package com.coit20258.drs.server;

import java.io.Serializable;

public class DrsRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Auth commands ──────────────────────────────────────────────────────
    public static final String CMD_LOGIN        = "LOGIN";
    public static final String CMD_REGISTER     = "REGISTER";
    public static final String CMD_EMAIL_EXISTS = "EMAIL_EXISTS";

    // ── Report commands ────────────────────────────────────────────────────
    public static final String CMD_REPORTS_FIND_ALL          = "REPORTS_FIND_ALL";
    public static final String CMD_REPORTS_FIND_BY_DEPT      = "REPORTS_FIND_BY_DEPT";
    public static final String CMD_REPORTS_SAVE              = "REPORTS_SAVE";
    public static final String CMD_REPORTS_UPDATE_STATUS     = "REPORTS_UPDATE_STATUS";

    // ── Assessment commands ────────────────────────────────────────────────
    public static final String CMD_ASSESSMENTS_FIND_ALL      = "ASSESSMENTS_FIND_ALL";
    public static final String CMD_ASSESSMENTS_FIND_BY_REPORT = "ASSESSMENTS_FIND_BY_REPORT";
    public static final String CMD_ASSESSMENTS_SAVE          = "ASSESSMENTS_SAVE";
    public static final String CMD_ASSESSMENTS_UPDATE        = "ASSESSMENTS_UPDATE";

    // ── Department commands ────────────────────────────────────────────────
    public static final String CMD_DEPARTMENTS_FIND_ALL  = "DEPARTMENTS_FIND_ALL";
    public static final String CMD_DEPARTMENTS_FIND_BY_ID = "DEPARTMENTS_FIND_BY_ID";

    // ── Department update commands ─────────────────────────────────────────
    public static final String CMD_DEPT_UPDATES_BY_DEPT   = "DEPT_UPDATES_BY_DEPT";
    public static final String CMD_DEPT_UPDATES_BY_REPORT = "DEPT_UPDATES_BY_REPORT";
    public static final String CMD_DEPT_UPDATES_SAVE      = "DEPT_UPDATES_SAVE";

    // ── Evacuation zone commands ───────────────────────────────────────────
    public static final String CMD_EVAC_ZONES_FIND_ALL       = "EVAC_ZONES_FIND_ALL";
    public static final String CMD_EVAC_ZONES_FIND_BY_REPORT = "EVAC_ZONES_FIND_BY_REPORT";
    public static final String CMD_EVAC_ZONES_SAVE           = "EVAC_ZONES_SAVE";
    public static final String CMD_EVAC_ZONES_UPDATE_OCC     = "EVAC_ZONES_UPDATE_OCC";

    // ── Payload ────────────────────────────────────────────────────────────
    private final String command;
    private final Object[] args;

    public DrsRequest(String command, Object... args) {
        this.command = command;
        this.args    = args;
    }

    public String getCommand()  { return command; }
    public Object[] getArgs()   { return args; }

    @SuppressWarnings("unchecked")
    public <T> T arg(int i)    { return (T) args[i]; }
    public int    argInt(int i) { return (Integer) args[i]; }
    public String argStr(int i) { return (String) args[i]; }
}
