package com.example.petcareplus.models;

import java.io.Serializable;

public class Reminder implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private String petId;
    private String ownerId;
    private String title;
    private String type;
    private long dateTimeMillis;
    private String repeatType;
    private String notes;

    public Reminder() {}

    public Reminder(String id, String petId, String ownerId, String title, String type,
                    long dateTimeMillis, String repeatType, String notes) {
        this.id = id;
        this.petId = petId;
        this.ownerId = ownerId;
        this.title = title;
        this.type = type;
        this.dateTimeMillis = dateTimeMillis;
        this.repeatType = repeatType;
        this.notes = notes;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public long getDateTimeMillis() { return dateTimeMillis; }
    public void setDateTimeMillis(long dateTimeMillis) { this.dateTimeMillis = dateTimeMillis; }
    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}