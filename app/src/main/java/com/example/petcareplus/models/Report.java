package com.example.petcareplus.models;

import java.io.Serializable;

public class Report implements Serializable {

    private String id;
    private String reporterId;
    private String reporterName;
    private String reportedUserId;
    private String reportedUserName;
    private String contentType;   // "message" or "listing"
    private String contentId;
    private String contentPreview;
    private String reason;
    private long timestampMillis;
    private boolean resolved;

    public Report() {}

    public Report(String id, String reporterId, String reporterName, String reportedUserId,
                  String reportedUserName, String contentType, String contentId,
                  String contentPreview, String reason, long timestampMillis, boolean resolved) {
        this.id = id;
        this.reporterId = reporterId;
        this.reporterName = reporterName;
        this.reportedUserId = reportedUserId;
        this.reportedUserName = reportedUserName;
        this.contentType = contentType;
        this.contentId = contentId;
        this.contentPreview = contentPreview;
        this.reason = reason;
        this.timestampMillis = timestampMillis;
        this.resolved = resolved;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getReporterId() { return reporterId; }
    public void setReporterId(String reporterId) { this.reporterId = reporterId; }
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    public String getReportedUserId() { return reportedUserId; }
    public void setReportedUserId(String reportedUserId) { this.reportedUserId = reportedUserId; }
    public String getReportedUserName() { return reportedUserName; }
    public void setReportedUserName(String reportedUserName) { this.reportedUserName = reportedUserName; }
    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }
    public String getContentId() { return contentId; }
    public void setContentId(String contentId) { this.contentId = contentId; }
    public String getContentPreview() { return contentPreview; }
    public void setContentPreview(String contentPreview) { this.contentPreview = contentPreview; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public long getTimestampMillis() { return timestampMillis; }
    public void setTimestampMillis(long timestampMillis) { this.timestampMillis = timestampMillis; }
    public boolean isResolved() { return resolved; }
    public void setResolved(boolean resolved) { this.resolved = resolved; }
}