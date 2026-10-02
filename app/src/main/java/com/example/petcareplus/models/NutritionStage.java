package com.example.petcareplus.models;

import java.io.Serializable;

public class NutritionStage implements Serializable {

    public String species;
    public String stage;
    public String proteinRange;
    public String fatRange;
    public String feedingFrequency;
    public String notes;

    public NutritionStage(String species, String stage, String proteinRange,
                          String fatRange, String feedingFrequency, String notes) {
        this.species = species;
        this.stage = stage;
        this.proteinRange = proteinRange;
        this.fatRange = fatRange;
        this.feedingFrequency = feedingFrequency;
        this.notes = notes;
    }
}