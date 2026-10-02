package com.example.petcareplus.models;

import java.io.Serializable;

public class BlockedUser implements Serializable {

    private String blockedUid;
    private String blockedName;
    private long timestampMillis;

    public BlockedUser() {}

    public BlockedUser(String blockedUid, String blockedName, long timestampMillis) {
        this.blockedUid = blockedUid;
        this.blockedName = blockedName;
        this.timestampMillis = timestampMillis;
    }

    public String getBlockedUid() { return blockedUid; }
    public void setBlockedUid(String blockedUid) { this.blockedUid = blockedUid; }
    public String getBlockedName() { return blockedName; }
    public void setBlockedName(String blockedName) { this.blockedName = blockedName; }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
}