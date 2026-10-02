package com.example.petcareplus.models;

import java.io.Serializable;

public class VetNgoLocation implements Serializable {

    public String id;
    public String name;
    public String type;
    public String address;
    public String hours;
    public String phone;

    public double latitude;
    public double longitude;

    public boolean verified;

    public String addedByUid;

    public long timestampMillis;

    // Required by Firestore
    public VetNgoLocation() {
    }

    public VetNgoLocation(
            String id,
            String name,
            String type,
            String address,
            String hours,
            String phone,
            double latitude,
            double longitude,
            boolean verified,
            String addedByUid,
            long timestampMillis
    ) {

        this.id = id;
        this.name = name;
        this.type = type;
        this.address = address;
        this.hours = hours;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.verified = verified;
        this.addedByUid = addedByUid;
        this.timestampMillis = timestampMillis;
    }
}