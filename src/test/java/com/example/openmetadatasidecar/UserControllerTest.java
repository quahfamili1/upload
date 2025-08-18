package com.example.openmetadatasidecar;

import com.example.openmetadatasidecar.model.Team;
import com.example.openmetadatasidecar.model.User;
import com.example.openmetadatasidecar.model.WrapperServiceResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class UserControllerTest {

    @Mock
    private OpenMetadataService openMetadataService;

    @Mock
    private WrapperService wrapperService;

    @InjectMocks
    private UserController userController;

    @Test
    public void syncUser_success() {
        // Given
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        when(openMetadataService.getCurrentUser(anyString())).thenReturn(user);

        WrapperServiceResponse wrapperResponse = new WrapperServiceResponse();
        wrapperResponse.setName("Test User");
        wrapperResponse.setTeams(Collections.singletonList("Test Team"));
        when(wrapperService.getUserInfo(anyString())).thenReturn(wrapperResponse);

        Team team = new Team();
        team.setId(UUID.randomUUID());
        team.setName("Test Team");
        when(openMetadataService.getTeamByName(anyString(), anyString())).thenReturn(team);

        // When
        ResponseEntity<String> response = userController.syncUser("auth");

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("User synchronized successfully.", response.getBody());
    }

    @Test
    public void syncUser_userNotFound() {
        // Given
        when(openMetadataService.getCurrentUser(anyString())).thenReturn(null);

        // When
        ResponseEntity<String> response = userController.syncUser("auth");

        // Then
        assertEquals(404, response.getStatusCodeValue());
        assertEquals("Current user not found.", response.getBody());
    }

    @Test
    public void syncUser_wrapperServiceFails() {
        // Given
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        when(openMetadataService.getCurrentUser(anyString())).thenReturn(user);

        when(wrapperService.getUserInfo(anyString())).thenReturn(null);

        // When
        ResponseEntity<String> response = userController.syncUser("auth");

        // Then
        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Failed to get user info from wrapper service.", response.getBody());
    }

    @Test
    public void syncUser_createNewTeam() {
        // Given
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        when(openMetadataService.getCurrentUser(anyString())).thenReturn(user);

        WrapperServiceResponse wrapperResponse = new WrapperServiceResponse();
        wrapperResponse.setName("Test User");
        wrapperResponse.setTeams(Collections.singletonList("New Team"));
        when(wrapperService.getUserInfo(anyString())).thenReturn(wrapperResponse);

        when(openMetadataService.getTeamByName(anyString(), anyString())).thenReturn(null);

        Team newTeam = new Team();
        newTeam.setId(UUID.randomUUID());
        newTeam.setName("New Team");
        when(openMetadataService.createTeam(any(Team.class), anyString())).thenReturn(newTeam);

        // When
        ResponseEntity<String> response = userController.syncUser("auth");

        // Then
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("User synchronized successfully.", response.getBody());
    }
}
