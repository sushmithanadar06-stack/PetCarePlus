package com.example.petcareplus.models;

import java.io.Serializable;

public class HealthRecord implements Serializable {

    private String id;
    private String petId;
    private String category;
    private String title;
    private String subtitle;
    private long dateMillis;
    private long nextDueMillis;
    private String notes;

    // Vet Visit only — location info
    private String address;
    private String hours;
    private String phone;

    public HealthRecord() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public long getDateMillis() { return dateMillis; }
    public void setDateMillis(long dateMillis) { this.dateMillis = dateMillis; }
    public long getNextDueMillis() { return nextDueMillis; }
    public void setNextDueMillis(long nextDueMillis) { this.nextDueMillis = nextDueMillis; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getHours() { return hours; }
    public void setHours(String hours) { this.hours = hours; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}