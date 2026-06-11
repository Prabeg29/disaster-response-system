package com.coit20258.drs.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class EvacuationZone implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_FULL   = "FULL";
    public static final String STATUS_CLOSED = "CLOSED";

    private int           id;
    private String        name;
    private String        location;
    private int           capacity;
    private int           currentOccupancy;
    private String        status;
    private int           reportId;
    private String        notes;
    private LocalDateTime createdAt;

    public EvacuationZone() {
    }

    public EvacuationZone(
            String name,
            String location,
            int capacity,
            int reportId,
            String notes) {
        this.name             = name;
        this.location         = location;
        this.capacity         = capacity;
        this.currentOccupancy = 0;
        this.status           = STATUS_ACTIVE;
        this.reportId         = reportId;
        this.notes            = notes;
        this.createdAt        = LocalDateTime.now();
    }

    public EvacuationZone(
            int id,
            String name,
            String location,
            int capacity,
            int currentOccupancy,
            String status,
            int reportId,
            String notes,
            LocalDateTime createdAt) {
        this.id               = id;
        this.name             = name;
        this.location         = location;
        this.capacity         = capacity;
        this.currentOccupancy = currentOccupancy;
        this.status           = status;
        this.reportId         = reportId;
        this.notes            = notes;
        this.createdAt        = createdAt;
    }

    public int getId()                  { return id; }
    public String getName()             { return name; }
    public String getLocation()         { return location; }
    public int getCapacity()            { return capacity; }
    public int getCurrentOccupancy()    { return currentOccupancy; }
    public String getStatus()           { return status; }
    public int getReportId()            { return reportId; }
    public String getNotes()            { return notes; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public void setId(int id)                             { this.id = id; }
    public void setName(String name)                      { this.name = name; }
    public void setLocation(String location)              { this.location = location; }
    public void setCapacity(int capacity)                 { this.capacity = capacity; }
    public void setCurrentOccupancy(int currentOccupancy) { this.currentOccupancy = currentOccupancy; }
    public void setStatus(String status)                  { this.status = status; }
    public void setReportId(int reportId)                 { this.reportId = reportId; }
    public void setNotes(String notes)                    { this.notes = notes; }
    public void setCreatedAt(LocalDateTime createdAt)     { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format(
                "[%d] %s at %s | %d/%d | %s",
                id, name, location, currentOccupancy, capacity, status, notes);
    }
}
