package com.ometa.uploader.strategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DelimitedFileAnalysisStrategy implements FileAnalysisStrategy {

    @Override
    public List<Map<String, String>> inferSchema(InputStream inputStream, int linesToScan) throws IOException {
        String content = readFirstNLines(inputStream, linesToScan);
        return inferSchemaFromDelimited(content);
    }

    private String readFirstNLines(InputStream inputStream, int n) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().limit(n).collect(Collectors.joining("\n"));
        }
    }

    private List<Map<String, String>> inferSchemaFromDelimited(String content) {
        List<Map<String, String>> columns = new ArrayList<>();
        char delimiter = detectDelimiter(content);
        String[] lines = content.split("\n");
        if (lines.length == 0) {
            return columns;
        }

        String[] headers = lines[0].split(String.valueOf(delimiter));
        List<Map<String, String>> data = new ArrayList<>();
        for (int i = 1; i < lines.length; i++) {
            String[] values = lines[i].split(String.valueOf(delimiter));
            Map<String, String> row = new HashMap<>();
            for (int j = 0; j < headers.length && j < values.length; j++) {
                row.put(headers[j].trim(), values[j].trim());
            }
            data.add(row);
        }

        for (String header : headers) {
            Map<String, String> column = new HashMap<>();
            column.put("name", header.trim());
            column.put("dataType", inferDataType(data, header.trim()));
            columns.add(column);
        }

        return columns;
    }

    private char detectDelimiter(String content) {
        if (content.contains(",")) {
            return ',';
        } else if (content.contains("\t")) {
            return '\t';
        } else if (content.contains(";")) {
            return ';';
        }
        return ','; // Default to comma
    }

    private String inferDataType(List<Map<String, String>> data, String columnName) {
        for (Map<String, String> row : data) {
            String value = row.get(columnName);
            if (value != null && !value.isEmpty()) {
                return inferDataType(value);
            }
        }
        return "STRING";
    }

    private String inferDataType(String value) {
        if (value.matches("-?\\d+")) {
            return "INT";
        } else if (value.matches("-?\\d*\\.\\d+")) {
            return "FLOAT";
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return "BOOLEAN";
        } else if (value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
            return "TIMESTAMP";
        }
        return "STRING";
    }
}
