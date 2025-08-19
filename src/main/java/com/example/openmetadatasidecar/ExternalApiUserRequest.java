package com.example.openmetadatasidecar;

import java.util.List;

public class ExternalApiUserRequest {
    private List<String> emails;

    public ExternalApiUserRequest(List<String> emails) {
        this.emails = emails;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }
}
