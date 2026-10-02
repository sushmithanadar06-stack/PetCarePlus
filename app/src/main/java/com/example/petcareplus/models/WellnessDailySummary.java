package com.example.petcareplus.models;

import java.io.Serializable;

public class WellnessDailySummary implements Serializable {

    private String id; // "yyyy-MM-dd"
    private String petId;
    private boolean hadMeal;
    private boolean hadWalk;
    private boolean hadWater;
    private String mood; // nullable
    private long timestampMillis; // start of that day

    public WellnessDailySummary() {}

    public WellnessDailySummary(String id, String petId, boolean hadMeal, boolean hadWalk,
                                boolean hadWater, String mood, long timestampMillis) {
        this.id = id;
        this.petId = petId;
        this.hadMeal = hadMeal;
        this.hadWalk = hadWalk;
        this.hadWater = hadWater;
        this.mood = mood;
        this.timestampMillis = timestampMillis;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }
    public boolean isHadMeal() { return hadMeal; }
    public void setHadMeal(boolean hadMeal) { this.hadMeal = hadMeal; }
    public boolean isHadWalk() { return hadWalk; }
    public void setHadWalk(boolean hadWalk) { this.hadWalk = hadWalk; }
    public boolean isHadWater() { return hadWater; }
    public void setHadWater(boolean hadWater) { this.hadWater = hadWater; }
    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
}