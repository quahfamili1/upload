package com.example.openmetadatasidecar;

import com.example.openmetadatasidecar.model.WrapperServiceResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WrapperService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${wrapper.service.url}")
    private String wrapperServiceUrl;

    public WrapperServiceResponse getUserInfo(String email) {
        String url = wrapperServiceUrl + "/user-info?email=" + email;
        return restTemplate.getForObject(url, WrapperServiceResponse.class);
    }
}
