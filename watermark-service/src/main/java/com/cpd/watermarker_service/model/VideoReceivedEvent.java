package com.cpd.watermarker_service.model;

public class VideoReceivedEvent {
    private String uuid;
    private String originalFilename;
    private String filePath;

    public VideoReceivedEvent() {}

    public VideoReceivedEvent(String uuid, String originalFilename, String filePath) {
        this.uuid = uuid;
        this.originalFilename = originalFilename;
        this.filePath = filePath;
    }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}