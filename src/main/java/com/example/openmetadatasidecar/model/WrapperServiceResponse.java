package com.example.openmetadatasidecar.model;

import java.util.List;

public class WrapperServiceResponse {
    private String name;
    private List<String> teams;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getTeams() {
        return teams;
    }

    public void setTeams(List<String> teams) {
        this.teams = teams;
    }
}
