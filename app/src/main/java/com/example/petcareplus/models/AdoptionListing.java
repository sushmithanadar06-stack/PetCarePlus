package com.example.petcareplus.models;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AdoptionListing implements Serializable {

    private String id;
    private String ownerId;
    private String ownerName;
    private String petName;
    private String breed;
    private String age;
    private String gender;
    private String reason;
    private String location;
    private List<String> photoList;
    private String photoBase64; // legacy field — old listings created before the multi-photo migration
    private boolean ngoVerified;
    private long timestampMillis;

    // NEW — "Adopted" status. Old listings don't have these fields, so they
    // default to not adopted.
    private boolean adopted;
    private long adoptedAtMillis;
    private String adoptedById;    // uid of the adopter, when they were one of the requesters (else null)
    private String adoptedByName;  // name of the adopter (kept for the Notifications module later)

    public AdoptionListing() {
        this.photoList = new ArrayList<>();
    }

    public AdoptionListing(String id, String ownerId, String ownerName, String petName, String breed,
                           String age, String gender, String reason, String location,
                           List<String> photoList, boolean ngoVerified, long timestampMillis) {
        this.id = id;
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.petName = petName;
        this.breed = breed;
        this.age = age;
        this.gender = gender;
        this.reason = reason;
        this.location = location;
        this.photoList = photoList != null ? photoList : new ArrayList<>();
        this.ngoVerified = ngoVerified;
        this.timestampMillis = timestampMillis;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getOwnerName() { return ownerName; }
    public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
    public String getPetName() { return petName; }
    public void setPetName(String petName) { this.petName = petName; }
    public String getBreed() { return breed; }
    public void setBreed(String breed) { this.breed = breed; }
    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public List<String> getPhotoList() { return photoList; }
    public void setPhotoList(List<String> photoList) { this.photoList = photoList; }
    public String getPhotoBase64() { return photoBase64; }
    public void setPhotoBase64(String photoBase64) { this.photoBase64 = photoBase64; }
    public boolean isNgoVerified() { return ngoVerified; }
    public void setNgoVerified(boolean ngoVerified) { this.ngoVerified = ngoVerified; }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }

    public boolean isAdopted() { return adopted; }
    public void setAdopted(boolean adopted) { this.adopted = adopted; }
    public long getAdoptedAtMillis() { return adoptedAtMillis; }
    public void setAdoptedAtMillis(long adoptedAtMillis) { this.adoptedAtMillis = adoptedAtMillis; }
    public String getAdoptedById() { return adoptedById; }
    public void setAdoptedById(String adoptedById) { this.adoptedById = adoptedById; }
    public String getAdoptedByName() { return adoptedByName; }
    public void setAdoptedByName(String adoptedByName) { this.adoptedByName = adoptedByName; }

    /**
     * Returns the first available photo for this listing, whether it was
     * created under the old single-photo schema or the new photoList one.
     */
    public String getFirstPhoto() {
        if (photoList != null && !photoList.isEmpty()) {
            return photoList.get(0);
        }
        return photoBase64;
    }
}