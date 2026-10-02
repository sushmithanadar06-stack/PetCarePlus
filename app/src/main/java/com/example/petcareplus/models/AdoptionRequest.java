package com.example.petcareplus.models;

import java.io.Serializable;

public class AdoptionRequest implements Serializable {

    private String id;
    private String listingId;
    private String requesterId;
    private String requesterName;
    private long timestampMillis;

    public AdoptionRequest() {}

    public AdoptionRequest(String id, String listingId, String requesterId, String requesterName, long timestampMillis) {
        this.id = id;
        this.listingId = listingId;
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.timestampMillis = timestampMillis;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getListingId() { return listingId; }
    public void setListingId(String listingId) { this.listingId = listingId; }
    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
}