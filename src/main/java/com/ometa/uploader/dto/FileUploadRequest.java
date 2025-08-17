package com.ometa.uploader.dto;

import org.springframework.web.multipart.MultipartFile;

public class FileUploadRequest {

    private final MultipartFile file;
    private final String filePath;
    private final int linesToScan;

    public FileUploadRequest(MultipartFile file, String filePath, int linesToScan) {
        this.file = file;
        this.filePath = filePath;
        this.linesToScan = linesToScan;
    }

    public MultipartFile getFile() {
        return file;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getLinesToScan() {
        return linesToScan;
    }
}
