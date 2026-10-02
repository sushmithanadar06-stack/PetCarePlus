package com.example.petcareplus.models;

import java.io.Serializable;

public class DocumentRecord implements Serializable {

    private String id;
    private String petId;
    private String fileName;
    private String localPath;
    private long sizeBytes;
    private long uploadedMillis;

    public DocumentRecord() {}

    public DocumentRecord(String id, String petId, String fileName, String localPath,
                          long sizeBytes, long uploadedMillis) {
        this.id = id;
        this.petId = petId;
        this.fileName = fileName;
        this.localPath = localPath;
        this.sizeBytes = sizeBytes;
        this.uploadedMillis = uploadedMillis;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPetId() { return petId; }
    public void setPetId(String petId) { this.petId = petId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getLocalPath() { return localPath; }
    public void setLocalPath(String localPath) { this.localPath = localPath; }
    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }
    public long getUploadedMillis() { return uploadedMillis; }
    public void setUploadedMillis(long uploadedMillis) { this.uploadedMillis = uploadedMillis; }
}