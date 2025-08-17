package com.ometa.uploader.strategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogFileAnalysisStrategy implements FileAnalysisStrategy {

    @Override
    public List<Map<String, String>> inferSchema(InputStream inputStream, int linesToScan) throws IOException {
        String content = readFirstNLines(inputStream, linesToScan);
        return inferSchemaFromLogs(content);
    }

    private String readFirstNLines(InputStream inputStream, int n) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().limit(n).collect(Collectors.joining("\n"));
        }
    }

    private List<Map<String, String>> inferSchemaFromLogs(String content) {
        Map<String, String> fieldTypes = new HashMap<>();
        Pattern pattern = Pattern.compile("([a-zA-Z0-9_]+)=([^\\s]+)");

        for (String line : content.split("\n")) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                String key = matcher.group(1);
                String value = matcher.group(2);
                String type = inferDataType(value);
                fieldTypes.put(key, type);
            }
        }

        List<Map<String, String>> columns = new ArrayList<>();
        for (Map.Entry<String, String> entry : fieldTypes.entrySet()) {
            Map<String, String> column = new HashMap<>();
            column.put("name", entry.getKey());
            column.put("dataType", entry.getValue());
            columns.add(column);
        }
        return columns;
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
