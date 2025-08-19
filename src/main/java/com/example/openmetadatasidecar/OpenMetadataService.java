package com.example.openmetadatasidecar;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OpenMetadataService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openmetadata.api.url}")
    private String openMetadataApiUrl;

    @Value("${external.user.api.url}")
    private String externalUserApiUrl;

    @Value("${external.user.api.token}")
    private String externalUserApiToken;

    // ... (createTable and createOpenMetadataRequest methods remain the same)
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


    public void syncUsers(String authorization) {
        System.out.println("Starting user synchronization...");
        List<Map<String, Object>> omUsers = getUsers(authorization);
        if (omUsers.isEmpty()) {
            System.out.println("No users found in OpenMetadata. Skipping sync.");
            return;
        }
        List<String> emails = omUsers.stream()
                                     .map(user -> (String) user.get("email"))
                                     .collect(Collectors.toList());

        ExternalApiUserResponse externalApiResponse = callExternalUserApi(emails);
        if (externalApiResponse == null || externalApiResponse.getUsers() == null) {
            System.out.println("No user details received from external API.");
            return;
        }

        for (UserDetail userDetail : externalApiResponse.getUsers()) {
            omUsers.stream()
                .filter(omUser -> userDetail.getEmail().equals(omUser.get("email")))
                .findFirst()
                .ifPresent(omUser -> processUser(omUser, userDetail, authorization));
        }
        System.out.println("User synchronization finished.");
    }

    private void processUser(Map<String, Object> omUser, UserDetail userDetail, String authorization) {
        String userId = (String) omUser.get("id");
        String userName = (String) omUser.get("name");
        System.out.println("Processing user: " + userName);

        // Update display name
        if (!userDetail.getName().equals(omUser.get("displayName"))) {
            updateUserDisplayName(userId, userDetail.getName(), authorization);
        }

        // Handle Department -> BusinessUnit
        if (userDetail.getDepartment() != null && !userDetail.getDepartment().isEmpty()) {
            ensureUserIsInTeam(userId, omUser, userDetail.getDepartment(), "BusinessUnit", null, authorization);
        }

        // Handle Company -> Division/Department/Group hierarchy
        if (userDetail.getCompany() != null && !userDetail.getCompany().isEmpty()) {
            String[] orgHierarchy = userDetail.getCompany().split("-");
            String parentTeamId = null;
            if (orgHierarchy.length == 3) { // dep-cluster-sect -> Division-Department-Group
                parentTeamId = ensureUserIsInTeam(userId, omUser, orgHierarchy[0], "Division", null, authorization);
                parentTeamId = ensureUserIsInTeam(userId, omUser, orgHierarchy[1], "Department", parentTeamId, authorization);
                ensureUserIsInTeam(userId, omUser, orgHierarchy[2], "Group", parentTeamId, authorization);
            } else if (orgHierarchy.length == 2) { // cluster-sect -> Department-Group
                parentTeamId = ensureUserIsInTeam(userId, omUser, orgHierarchy[0], "Department", null, authorization);
                ensureUserIsInTeam(userId, omUser, orgHierarchy[1], "Group", parentTeamId, authorization);
            } else if (orgHierarchy.length == 1) { // sect -> Group
                 ensureUserIsInTeam(userId, omUser, orgHierarchy[0], "Group", null, authorization);
            }
        }
    }

    private String ensureUserIsInTeam(String userId, Map<String, Object> omUser, String teamName, String teamType, String parentTeamId, String authorization) {
        Optional<Map<String, Object>> teamOpt = getTeamByName(teamName, authorization);
        Map<String, Object> team;
        if (teamOpt.isEmpty()) {
            System.out.println("Team not found, creating new team: " + teamName);
            team = createTeam(teamName, teamType, parentTeamId, authorization);
        } else {
            team = teamOpt.get();
        }
        String teamId = (String) team.get("id");

        List<Map<String, Object>> userTeams = (List<Map<String, Object>>) omUser.get("teams");
        boolean isUserInTeam = userTeams != null && userTeams.stream().anyMatch(t -> t.get("id").equals(teamId));

        if (!isUserInTeam) {
            System.out.println("Adding user " + omUser.get("name") + " to team " + teamName);
            addUserToTeam(userId, teamId, authorization);
            // Refresh user object to reflect the change for subsequent checks
            omUser.put("teams", getUserById(userId, authorization).get("teams"));
        }
        return teamId;
    }


    private List<Map<String, Object>> getUsers(String authorization) {
        String url = openMetadataApiUrl + "/users?fields=teams,email";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
            if (response.getBody() != null && response.getBody().containsKey("data")) {
                return (List<Map<String, Object>>) response.getBody().get("data");
            }
        } catch (Exception e) {
            System.err.println("Error getting users from OpenMetadata: " + e.getMessage());
        }
        return Collections.emptyList();
    }

    private Map<String, Object> getUserById(String userId, String authorization) {
        String url = openMetadataApiUrl + "/users/" + userId + "?fields=teams";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error getting user by ID " + userId + ": " + e.getMessage());
            return Collections.emptyMap();
        }
    }


    private ExternalApiUserResponse callExternalUserApi(List<String> emails) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + externalUserApiToken);
        headers.set("Content-Type", "application/json");

        ExternalApiUserRequest requestBody = new ExternalApiUserRequest(emails);
        HttpEntity<ExternalApiUserRequest> requestEntity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<ExternalApiUserResponse> response = restTemplate.postForEntity(externalUserApiUrl, requestEntity, ExternalApiUserResponse.class);
            return response.getBody();
        } catch (Exception e) {
            System.err.println("Error calling external user API: " + e.getMessage());
            return null;
        }
    }

    private void updateUserDisplayName(String userId, String newDisplayName, String authorization) {
        // Implementation from before, using JSON Patch
        String url = openMetadataApiUrl + "/users/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        headers.set("Content-Type", "application/json-patch+json");
        List<Map<String, Object>> patch = Collections.singletonList(
            Map.of("op", "replace", "path", "/displayName", "value", newDisplayName)
        );
        HttpEntity<List<Map<String, Object>>> requestEntity = new HttpEntity<>(patch, headers);
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
            System.out.println("Updated display name for user " + userId);
        } catch (Exception e) {
            System.err.println("Error updating display name for user " + userId + ": " + e.getMessage());
        }
    }

    private Optional<Map<String, Object>> getTeamByName(String name, String authorization) {
        String url = openMetadataApiUrl + "/teams/name/" + name;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, Map.class);
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return Optional.empty();
            }
            throw e;
        }
    }

    private Map<String, Object> createTeam(String name, String teamType, String parentId, String authorization) {
        String url = openMetadataApiUrl + "/teams";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        headers.set("Content-Type", "application/json");

        Map<String, Object> teamDetails = new HashMap<>();
        teamDetails.put("name", name);
        teamDetails.put("teamType", teamType);
        if (parentId != null) {
            teamDetails.put("parents", Collections.singletonList(parentId));
        }

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(teamDetails, headers);
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Map.class);
        return response.getBody();
    }

    private void addUserToTeam(String userId, String teamId, String authorization) {
        // This uses the JSON Patch approach on the user object
        String url = openMetadataApiUrl + "/users/" + userId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authorization);
        headers.set("Content-Type", "application/json-patch+json");

        // Create the value part of the patch
        Map<String, String> teamReference = new HashMap<>();
        teamReference.put("id", teamId);
        teamReference.put("type", "team");

        // Create the patch operation
        List<Map<String, Object>> patch = Collections.singletonList(
            Map.of("op", "add", "path", "/teams/-", "value", teamReference)
        );

        HttpEntity<List<Map<String, Object>>> requestEntity = new HttpEntity<>(patch, headers);
        try {
            restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, Void.class);
        } catch (Exception e) {
            System.err.println("Error adding user " + userId + " to team " + teamId + ": " + e.getMessage());
        }
    }
}
