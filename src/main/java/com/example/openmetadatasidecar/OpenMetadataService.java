package com.example.openmetadatasidecar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenMetadataService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openmetadata.api.url}")
    private String openMetadataApiUrl;

    public void createTable(String tableName, String databaseSchema, List<Map<String, String>> columns, String description, String authorization) {
        String url = openMetadataApiUrl + "/tables";
        Map<String, Object> requestBody = createOpenMetadataRequest(tableName, databaseSchema, columns, description);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        restTemplate.put(url, requestEntity);
    }

    private Map<String, Object> createOpenMetadataRequest(String tableName, String databaseSchema, List<Map<String, String>> columns, String description) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", tableName.replace(".csv", ""));
        if (description != null && !description.isEmpty()) {
            request.put("description", description);
        }
        request.put("databaseSchema", databaseSchema);
        request.put("columns", columns);
        return request;
    }
}
