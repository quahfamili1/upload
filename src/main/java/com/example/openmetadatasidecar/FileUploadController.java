package com.example.openmetadatasidecar;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class FileUploadController {

    @Autowired
    private OpenMetadataService openMetadataService;

    @PutMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file,
                                             @RequestParam("databaseSchema") String databaseSchema,
                                             @RequestParam(value = "tableName", required = false) String tableName,
                                             @RequestParam(value = "linesToScan", required = false, defaultValue = "10") int linesToScan,
                                             @RequestHeader("Authorization") String authorization) {
        try {
            String content = FileAnalysisUtils.readFirstNLines(file.getInputStream(), linesToScan);
            List<Map<String, String>> columns = FileAnalysisUtils.inferSchema(content);
            String name = (tableName != null && !tableName.isEmpty()) ? tableName : file.getOriginalFilename();

            openMetadataService.createTable(name, databaseSchema, columns, null, authorization);

            return ResponseEntity.ok("File uploaded and metadata created successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process file: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to connect to OpenMetadata: " + e.getMessage());
        }
    }

    @PostMapping("/upload-tables-from-csv")
    public ResponseEntity<String> uploadTablesFromCsv(@RequestParam("file") MultipartFile file,
                                                      @RequestParam("databaseSchema") String databaseSchema,
                                                      @RequestHeader("Authorization") String authorization) {
        try {
            List<TableDefinition> tables = parseCsvToTableDefinitions(file.getInputStream());

            for (TableDefinition tableDef : tables) {
                List<Map<String, String>> columns = parseAttributesToColumns(tableDef.getAttributes());
                openMetadataService.createTable(tableDef.getName(), databaseSchema, columns, tableDef.getDescription(), authorization);
            }

            return ResponseEntity.ok("Successfully created " + tables.size() + " tables in OpenMetadata.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process CSV file: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create tables in OpenMetadata: " + e.getMessage());
        }
    }

    private List<TableDefinition> parseCsvToTableDefinitions(InputStream inputStream) throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = CsvSchema.emptySchema().withHeader();
        MappingIterator<TableDefinition> it = mapper.readerFor(TableDefinition.class).with(schema).readValues(inputStream);
        return it.readAll();
    }

    private List<Map<String, String>> parseAttributesToColumns(String attributes) {
        if (attributes == null || attributes.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(attributes.split("\\|"))
                .map(String::trim)
                .map(attr -> {
                    Map<String, String> column = new HashMap<>();
                    column.put("name", attr);
                    column.put("dataType", "STRING");
                    return column;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/user-sync")
    public ResponseEntity<String> syncUsers(@RequestHeader("Authorization") String authorization) {
        try {
            openMetadataService.syncUsers(authorization);
            return ResponseEntity.ok("User synchronization process started.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to start user synchronization: " + e.getMessage());
        }
    }
}
