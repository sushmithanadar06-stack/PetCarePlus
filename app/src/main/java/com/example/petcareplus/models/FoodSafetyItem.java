package com.example.petcareplus.models;

import java.io.Serializable;

public class FoodSafetyItem implements Serializable {

    public String name;
    public String icon;

    public String dogStatus;   // "Safe", "Unsafe", "Caution"
    public String dogReason;

    public String catStatus;
    public String catReason;

    public FoodSafetyItem(String name, String icon,
                          String dogStatus, String dogReason,
                          String catStatus, String catReason) {
        this.name = name;
        this.icon = icon;
        this.dogStatus = dogStatus;
        this.dogReason = dogReason;
        this.catStatus = catStatus;
        this.catReason = catReason;
    }
}