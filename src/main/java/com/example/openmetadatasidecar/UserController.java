package com.example.openmetadatasidecar;

import com.example.openmetadatasidecar.model.Team;
import com.example.openmetadatasidecar.model.User;
import com.example.openmetadatasidecar.model.WrapperServiceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class UserController {

    @Autowired
    private OpenMetadataService openMetadataService;

    @Autowired
    private WrapperService wrapperService;

    @PostMapping("/sync-user")
    public ResponseEntity<String> syncUser(@RequestHeader("Authorization") String authorization) {
        try {
            // 1. Get the current user from Open Metadata
            User currentUser = openMetadataService.getCurrentUser(authorization);
            if (currentUser == null) {
                return ResponseEntity.status(404).body("Current user not found.");
            }

            // 2. Get user info from the external wrapper service
            WrapperServiceResponse wrapperResponse = wrapperService.getUserInfo(currentUser.getEmail());
            if (wrapperResponse == null) {
                return ResponseEntity.status(500).body("Failed to get user info from wrapper service.");
            }

            // 3. Update user's display name
            currentUser.setDisplayName(wrapperResponse.getName());

            // 4. Handle teams
            List<UUID> teamIds = new ArrayList<>();
            if (currentUser.getTeams() != null) {
                teamIds.addAll(currentUser.getTeams());
            }

            for (String teamName : wrapperResponse.getTeams()) {
                Team team = openMetadataService.getTeamByName(teamName, authorization);
                if (team == null) {
                    // 5. If the team doesn't exist, create it
                    Team newTeam = new Team();
                    newTeam.setName(teamName);
                    newTeam.setDisplayName(teamName);
                    team = openMetadataService.createTeam(newTeam, authorization);
                }
                if (!teamIds.contains(team.getId())) {
                    teamIds.add(team.getId());
                }
            }

            // 6. Update the user's teams
            currentUser.setTeams(teamIds);

            // 7. Update the user in Open Metadata
            openMetadataService.updateUser(currentUser.getId().toString(), currentUser, authorization);

            return ResponseEntity.ok("User synchronized successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("An error occurred during user synchronization: " + e.getMessage());
        }
    }
}
