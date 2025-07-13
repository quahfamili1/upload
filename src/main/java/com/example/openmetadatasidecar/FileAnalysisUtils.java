package com.example.openmetadatasidecar;

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

public class FileAnalysisUtils {

    public static String readFirstNLines(InputStream inputStream, int n) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return reader.lines().limit(n).collect(Collectors.joining("\n"));
        }
    }

    public static List<Map<String, String>> inferSchema(String content) {
        if (content == null || content.isEmpty()) {
            return new ArrayList<>();
        }

        // Simple heuristic to detect if it's log data
        if (isLogData(content)) {
            return inferSchemaFromLogs(content);
        } else {
            return inferSchemaFromDelimited(content);
        }
    }

    private static boolean isLogData(String content) {
        // If the first line contains common log patterns like '=', '[]', or ':', it's likely a log file.
        String firstLine = content.split("\n")[0];
        return firstLine.contains("=") || firstLine.contains("[") || firstLine.contains(":");
    }

    private static List<Map<String, String>> inferSchemaFromDelimited(String content) {
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

    private static List<Map<String, String>> inferSchemaFromLogs(String content) {
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

    private static char detectDelimiter(String content) {
        if (content.contains(",")) {
            return ',';
        } else if (content.contains("\t")) {
            return '\t';
        } else if (content.contains(";")) {
            return ';';
        }
        return ','; // Default to comma
    }

    private static String inferDataType(List<Map<String, String>> data, String columnName) {
        for (Map<String, String> row : data) {
            String value = row.get(columnName);
            if (value != null && !value.isEmpty()) {
                return inferDataType(value);
            }
        }
        return "STRING";
    }

    private static String inferDataType(String value) {
        if (value.matches("-?\\d+")) {
            return "INT";
        } else if (value.matches("-?\\d*\\.\\d+")) {
            return "FLOAT";
        } else if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return "BOOLEAN";
        }
        // Basic date detection, can be improved
        else if (value.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z")) {
            return "TIMESTAMP";
        }
        return "STRING";
    }
}
