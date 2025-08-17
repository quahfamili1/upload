package com.ometa.uploader.controller;

import com.ometa.uploader.dto.FileUploadRequest;
import com.ometa.uploader.service.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Autowired
    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @PutMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam("filePath") String filePath,
                                             @RequestParam(value = "linesToScan", defaultValue = "10") int linesToScan,
                                             @RequestHeader("Authorization") String authorization) {
        FileUploadRequest request = new FileUploadRequest(file, filePath, linesToScan);
        fileUploadService.uploadFile(request, authorization);
        return ResponseEntity.ok("File uploaded and metadata created successfully.");
    }
}
