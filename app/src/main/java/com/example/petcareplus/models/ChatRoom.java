package com.example.petcareplus.models;

import java.io.Serializable;

public class ChatRoom implements Serializable {

    public String id;
    public String name;
    public String icon;
    public String description;
    public int retentionDays;
    public boolean showHealthDisclaimer;

    public ChatRoom(String id, String name, String icon, String description,
                    int retentionDays, boolean showHealthDisclaimer) {
        this.id = id;
        this.name = name;
        this.icon = icon;
        this.description = description;
        this.retentionDays = retentionDays;
        this.showHealthDisclaimer = showHealthDisclaimer;
    }
}