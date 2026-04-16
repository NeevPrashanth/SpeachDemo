package com.demo.speech;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.time.Duration;

@Component
public class SttClient {
    private final RestTemplate restTemplate;
    private final String sttServiceUrl;

    public SttClient(RestTemplateBuilder builder, @Value("${stt.service.url}") String sttServiceUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofMinutes(5))
                .build();
        this.sttServiceUrl = sttServiceUrl;
    }

    public String transcribe(File audioFile) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(audioFile));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<SttResponse> response = restTemplate.exchange(
                sttServiceUrl,
                HttpMethod.POST,
                request,
                SttResponse.class
        );

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new IllegalStateException("STT service failed");
        }

        return response.getBody().text();
    }
}
