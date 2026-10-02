package com.example.petcareplus.models;

import java.io.Serializable;

public class SnackItem implements Serializable {

    public String name;
    public String icon;
    public String species; // "Dog", "Cat", "Both"
    public String note;

    public SnackItem(String name, String icon, String species, String note) {
        this.name = name;
        this.icon = icon;
        this.species = species;
        this.note = note;
    }
}