package com.coit20258.drs.model;

import java.io.Serializable;

public class Department implements Serializable {

    private static final long serialVersionUID = 1L;

    private int id;
    private String name;
    private String contactEmail;
    private String contactPhone;
    private boolean isActive;

    // ── Constructors ───────────────────────────────────────────────────────
    public Department() {
    }


    public Department(int departmentId, String name,
            String contactEmail, String contactPhone,
            boolean isActive) {
        this.id = departmentId;
        this.name = name;
        this.contactEmail = contactEmail;
        this.contactPhone = contactPhone;
        this.isActive = isActive;
    }

    // ── Getters & Setters ──────────────────────────────────────────────────
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    // ── Utility ────────────────────────────────────────────────────────────
    /**
     * Used by JavaFX ComboBox/ListView to display the department name without a
     * custom cell factory.
     */
    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Department d)) {
            return false;
        }
        return id == d.id;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }
}
