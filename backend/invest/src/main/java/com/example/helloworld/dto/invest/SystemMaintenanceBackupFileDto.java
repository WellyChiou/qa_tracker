package com.example.helloworld.dto.invest;

import java.util.Date;

public class SystemMaintenanceBackupFileDto {
    private String filename;
    private Long size;
    private String sizeFormatted;
    private Date modified;
    private String database;
    private String relativePath;

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getSizeFormatted() {
        return sizeFormatted;
    }

    public void setSizeFormatted(String sizeFormatted) {
        this.sizeFormatted = sizeFormatted;
    }

    public Date getModified() {
        return modified;
    }

    public void setModified(Date modified) {
        this.modified = modified;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }
}
