package com.coit20258.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class DepartmentUpdate implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_RESPONDING    = "RESPONDING";
    public static final String STATUS_COMPLETED     = "COMPLETED";
    public static final String STATUS_NEEDS_SUPPORT = "NEEDS_SUPPORT";

    private int id;
    private int reportId;
    private int departmentId;
    private int updatedById;
    private String updateText;
    private String responseStatus;
    private LocalDateTime updatedAt;

    // Populated by DAO join — not persisted
    private String departmentName;
    private String reportType;
    private String reportLocation;
    private String updatedByName;

    public DepartmentUpdate() {}

    public DepartmentUpdate(int reportId, int departmentId, int updatedById,
                            String updateText, String responseStatus) {
        this.reportId       = reportId;
        this.departmentId   = departmentId;
        this.updatedById    = updatedById;
        this.updateText     = updateText;
        this.responseStatus = responseStatus;
    }

    public int getId()                  { return id; }
    public void setId(int id)           { this.id = id; }

    public int getReportId()            { return reportId; }
    public void setReportId(int v)      { this.reportId = v; }

    public int getDepartmentId()        { return departmentId; }
    public void setDepartmentId(int v)  { this.departmentId = v; }

    public int getUpdatedById()         { return updatedById; }
    public void setUpdatedById(int v)   { this.updatedById = v; }

    public String getUpdateText()                { return updateText; }
    public void setUpdateText(String v)          { this.updateText = v; }

    public String getResponseStatus()            { return responseStatus; }
    public void setResponseStatus(String v)      { this.responseStatus = v; }

    public LocalDateTime getUpdatedAt()          { return updatedAt; }
    public void setUpdatedAt(LocalDateTime v)    { this.updatedAt = v; }

    public String getDepartmentName()            { return departmentName; }
    public void setDepartmentName(String v)      { this.departmentName = v; }

    public String getReportType()                { return reportType; }
    public void setReportType(String v)          { this.reportType = v; }

    public String getReportLocation()            { return reportLocation; }
    public void setReportLocation(String v)      { this.reportLocation = v; }

    public String getUpdatedByName()             { return updatedByName; }
    public void setUpdatedByName(String v)       { this.updatedByName = v; }

    @Override
    public String toString() {
        return String.format("[%d] Report#%d | Dept#%d | %s | %s",
                id, reportId, departmentId, responseStatus, updatedAt);
    }
}
