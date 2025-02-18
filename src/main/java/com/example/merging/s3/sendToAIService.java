package com.example.merging.s3;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class sendToAIService {

    private final RestTemplate restTemplate;

    public sendToAIService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private static final String AI_SERVER_URL = "http://ai-server.com/api/process-pdf";

    // 업로드된 S3 파일 URL을 AI 서버에 전달
    public void sendFileToAI(String fileUrl) {
        Map<String, String> request = Map.of("fileUrl", fileUrl);
        restTemplate.postForObject(AI_SERVER_URL, request, String.class);
    }
}
