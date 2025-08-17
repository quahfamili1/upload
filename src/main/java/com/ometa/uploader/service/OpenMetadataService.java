package com.ometa.uploader.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenMetadataService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${openmetadata.api.url}")
    private String openMetadataApiUrl;

    public void ensureSchemaExists(String databaseName, String schemaName, String authorization) {
        String fullyQualifiedSchemaName = databaseName + "." + schemaName;
        String url = openMetadataApiUrl + "/databaseSchemas/name/" + fullyQualifiedSchemaName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            // 1. Check if schema exists
            restTemplate.exchange(url, HttpMethod.GET, requestEntity, Void.class);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // 2. If not found, create it
                createSchema(databaseName, schemaName, authorization);
            } else {
                throw e;
            }
        }
    }

    private void createSchema(String databaseName, String schemaName, String authorization) {
        String url = openMetadataApiUrl + "/databaseSchemas";
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("database", databaseName);
        requestBody.put("name", schemaName);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // Assuming a PUT request to create a schema
        restTemplate.put(url, requestEntity);
    }

    public void createTable(String tableName, String fullyQualifiedSchemaName, List<Map<String, String>> columns, String description, String authorization) {
        String url = openMetadataApiUrl + "/tables";
        Map<String, Object> requestBody = createOpenMetadataRequest(tableName, fullyQualifiedSchemaName, columns, description);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        restTemplate.put(url, requestEntity);
    }

    private Map<String, Object> createOpenMetadataRequest(String tableName, String fullyQualifiedSchemaName, List<Map<String, String>> columns, String description) {
        Map<String, Object> request = new HashMap<>();
        request.put("name", tableName);
        if (description != null && !description.isEmpty()) {
            request.put("description", description);
        }
        request.put("databaseSchema", fullyQualifiedSchemaName);
        request.put("columns", columns);
        return request;
    }
}
