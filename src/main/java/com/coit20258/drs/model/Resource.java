package com.coit20258.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Resource implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String TYPE_VEHICLE   = "VEHICLE";
    public static final String TYPE_EQUIPMENT = "EQUIPMENT";
    public static final String TYPE_PERSONNEL = "PERSONNEL";
    public static final String TYPE_MEDICAL   = "MEDICAL";
    public static final String TYPE_SUPPLIES  = "SUPPLIES";

    public static final String STATUS_AVAILABLE   = "AVAILABLE";
    public static final String STATUS_DEPLOYED    = "DEPLOYED";
    public static final String STATUS_MAINTENANCE = "MAINTENANCE";

    private int           id;
    private String        name;
    private String        resourceType;
    private int           totalQuantity;
    private int           availableQuantity;
    private String        status;
    private int           assignedReportId;
    private String        notes;
    private LocalDateTime updatedAt;

    public Resource() {}

    public Resource(String name, String resourceType, int totalQuantity,
                    int availableQuantity, String status, int assignedReportId, String notes) {
        this.name              = name;
        this.resourceType      = resourceType;
        this.totalQuantity     = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.status            = status;
        this.assignedReportId  = assignedReportId;
        this.notes             = notes;
        this.updatedAt         = LocalDateTime.now();
    }

    public Resource(int id, String name, String resourceType, int totalQuantity,
                    int availableQuantity, String status, int assignedReportId,
                    String notes, LocalDateTime updatedAt) {
        this.id                = id;
        this.name              = name;
        this.resourceType      = resourceType;
        this.totalQuantity     = totalQuantity;
        this.availableQuantity = availableQuantity;
        this.status            = status;
        this.assignedReportId  = assignedReportId;
        this.notes             = notes;
        this.updatedAt         = updatedAt;
    }

    public int getId()                  { return id; }
    public String getName()             { return name; }
    public String getResourceType()     { return resourceType; }
    public int getTotalQuantity()       { return totalQuantity; }
    public int getAvailableQuantity()   { return availableQuantity; }
    public String getStatus()           { return status; }
    public int getAssignedReportId()    { return assignedReportId; }
    public String getNotes()            { return notes; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    public void setId(int id)                             { this.id = id; }
    public void setName(String name)                      { this.name = name; }
    public void setResourceType(String resourceType)      { this.resourceType = resourceType; }
    public void setTotalQuantity(int totalQuantity)       { this.totalQuantity = totalQuantity; }
    public void setAvailableQuantity(int qty)             { this.availableQuantity = qty; }
    public void setStatus(String status)                  { this.status = status; }
    public void setAssignedReportId(int assignedReportId) { this.assignedReportId = assignedReportId; }
    public void setNotes(String notes)                    { this.notes = notes; }
    public void setUpdatedAt(LocalDateTime updatedAt)     { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) | %d/%d | %s",
                id, name, resourceType, availableQuantity, totalQuantity, status);
    }
}
