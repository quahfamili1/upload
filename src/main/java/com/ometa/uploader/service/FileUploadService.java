package com.ometa.uploader.service;

import com.ometa.uploader.dto.FileUploadRequest;
import com.ometa.uploader.exception.ApiException;
import com.ometa.uploader.strategy.DelimitedFileAnalysisStrategy;
import com.ometa.uploader.strategy.FileAnalysisStrategy;
import com.ometa.uploader.strategy.LogFileAnalysisStrategy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Service
public class FileUploadService {

    private final OpenMetadataService openMetadataService;
    private final Map<String, FileAnalysisStrategy> strategies;

    @Autowired
    public FileUploadService(OpenMetadataService openMetadataService) {
        this.openMetadataService = openMetadataService;
        // In a real app, these would be injected as beans
        strategies = Map.of(
                "delimited", new DelimitedFileAnalysisStrategy(),
                "log", new LogFileAnalysisStrategy()
        );
    }

    public void uploadFile(FileUploadRequest request, String authorization) {
        try {
            String filePath = request.getFilePath();
            MultipartFile file = request.getFile();

            // 1. Parse filePath
            String[] parts = filePath.split("/");
            if (parts.length != 3) {
                throw new IllegalArgumentException("filePath must be in the format database/schema/filename.ext");
            }
            String databaseName = parts[0];
            String schemaName = parts[1];
            String fileName = parts[2];

            // Remove file extension to get table name
            String tableName = fileName;
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                tableName = fileName.substring(0, dotIndex);
            }

            // 2. Ensure schema exists
            openMetadataService.ensureSchemaExists(databaseName, schemaName, authorization);

            // 3. Select strategy
            FileAnalysisStrategy strategy = selectStrategy(fileName);

            // 4. Infer schema
            List<Map<String, String>> columns = strategy.inferSchema(file.getInputStream(), request.getLinesToScan());

            // 5. Create table
            String fullyQualifiedSchemaName = databaseName + "." + schemaName;
            openMetadataService.createTable(tableName, fullyQualifiedSchemaName, columns, null, authorization);
        } catch (IOException e) {
            throw new ApiException("Failed to process file: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private FileAnalysisStrategy selectStrategy(String fileName) {
        if (fileName.endsWith(".log")) {
            return strategies.get("log");
        }
        // Default to delimited
        return strategies.get("delimited");
    }
}
