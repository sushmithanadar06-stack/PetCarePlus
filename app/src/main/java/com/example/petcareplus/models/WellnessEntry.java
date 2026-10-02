package com.example.petcareplus.models;

import java.io.Serializable;

public class WellnessEntry implements Serializable {

    private String id;
    private String petId;
    private String type;       // "weight", "meal", "walk", "water", "mood"
    private String value;      // e.g. "4.0" for weight, "Happy" for mood
    private String note;
    private long timestampMillis;

    public WellnessEntry() {
    }

    public WellnessEntry(String id, String petId, String type, String value,
                         String note, long timestampMillis) {
        this.id = id;
        this.petId = petId;
        this.type = type;
        this.value = value;
        this.note = note;
        this.timestampMillis = timestampMillis;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
}