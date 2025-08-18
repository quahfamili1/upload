package com.example.openmetadatasidecar;

import com.example.openmetadatasidecar.model.Team;
import com.example.openmetadatasidecar.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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

    public User getCurrentUser(String authorization) {
        String url = openMetadataApiUrl + "/users/me";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        return restTemplate.exchange(url, HttpMethod.GET, requestEntity, User.class).getBody();
    }

    public User getUserByName(String name, String authorization) {
        String url = openMetadataApiUrl + "/users/name/" + name;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, HttpMethod.GET, requestEntity, User.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public Team getTeamByName(String name, String authorization) {
        String url = openMetadataApiUrl + "/teams/name/" + name;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<String> requestEntity = new HttpEntity<>(headers);
        try {
            return restTemplate.exchange(url, HttpMethod.GET, requestEntity, Team.class).getBody();
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
    }

    public Team createTeam(Team team, String authorization) {
        String url = openMetadataApiUrl + "/teams";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<Team> requestEntity = new HttpEntity<>(team, headers);
        return restTemplate.postForObject(url, requestEntity, Team.class);
    }

    public void updateUser(String userId, User user, String authorization) {
        String url = openMetadataApiUrl + "/users";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<User> requestEntity = new HttpEntity<>(user, headers);
        restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Void.class);
    }
}
