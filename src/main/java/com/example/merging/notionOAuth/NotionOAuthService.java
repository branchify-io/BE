package com.example.merging.notionOAuth;

import com.example.merging.assistantlist.AssistantList;
import com.example.merging.assistantlist.AssistantListRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotionOAuthService {

    @Value("${notion.client-id}")
    private String clientId;

    @Value("${notion.client-secret}")
    private String clientSecret;

    @Value("${notion.redirect-uri}")
    private String redirectUri;

    @Value("${notion.token-uri}")
    private String tokenUri;

    private final RestTemplate restTemplate;
    private final AssistantListRepository assistantListRepository;
    private final NotionOAuthRepository notionOAuthRepository;

    public NotionOAuthService(RestTemplate restTemplate, AssistantListRepository assistantListRepository,
                              NotionOAuthRepository notionOAuthRepository) {
        this.restTemplate = restTemplate;
        this.assistantListRepository = assistantListRepository;
        this.notionOAuthRepository = notionOAuthRepository;
    }

    // Authorization URL 생성
    public String getAuthorizationUrl(String userEmail, String assistantName) {
        String state = userEmail + ":" + assistantName; // 상태값 생성 (userEmail:assistantName)

        // clientId 값 확인
        if (clientId == null || clientId.isEmpty()) {
            throw new RuntimeException("CLIENT_ID is not set in application.yml");
        }

        // Authorization URL 생성
        return String.format(
                "https://api.notion.com/v1/oauth/authorize?client_id=%s&response_type=code&owner=user&redirect_uri=%s&state=%s",
                clientId,
                redirectUri,
                state
        );
    }

    // Notion에서 받은 Authorization Code를 Access Token으로 교환
    @Transactional
    public void exchangeAuthorizationCode(String code, String userEmail, String assistantName) {
        // Assistant 존재 여부 확인
        AssistantList assistant = assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
                .orElseThrow(() -> new RuntimeException("Assistant not found for user: " + userEmail));

        // Base64로 client_id와 client_secret 인코딩
        String credentials = clientId + ":" + clientSecret;
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());

        // Access Token 요청 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("Authorization", "Basic " + encodedCredentials); // Base64 인코딩된 인증 정보
        headers.add("Notion-Version", "2022-06-28"); // Notion API 버전

        // 요청 바디 설정
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("code", code);
        body.put("redirect_uri", redirectUri);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            // POST 요청 보내기
            ResponseEntity<NotionTokenResponseDTO> response = restTemplate.postForEntity(
                    tokenUri, request, NotionTokenResponseDTO.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                NotionTokenResponseDTO tokenResponseDTO = response.getBody();

                if (tokenResponseDTO == null) {
                    throw new RuntimeException("Response body is null. Failed to get access token.");
                }

                // Notion OAuth 정보 확인 (기존 정보가 있으면 업데이트, 없으면 새로 생성)
                NotionOAuth notionOAuth = notionOAuthRepository
                        .findByAssistant_AssistantNameAndAssistant_User_Email(assistantName, userEmail)
                        .orElse(new NotionOAuth());

                // AssistantList 설정 (FK 관계)
                notionOAuth.setAssistant(assistant);

                // Notion API 응답 값 저장
                notionOAuth.setAccessToken(tokenResponseDTO.getAccess_token());
                notionOAuth.setRefreshToken(tokenResponseDTO.getRefresh_token());
                notionOAuth.setTokenType(tokenResponseDTO.getToken_type());
                notionOAuth.setScope(tokenResponseDTO.getScope());
                notionOAuth.setWorkspaceId(tokenResponseDTO.getWorkspace_id());
                notionOAuth.setWorkspaceName(tokenResponseDTO.getWorkspace_name());

                notionOAuthRepository.save(notionOAuth);
            } else {
                // HTTP 응답 코드가 2xx가 아닌 경우 처리
                throw new RuntimeException("Failed to exchange authorization code: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            // 401 Unauthorized 또는 다른 HTTP 클라이언트 오류 처리
            throw new RuntimeException("Failed to connect Notion account: " + e.getStatusCode()
                    + " on POST request for " + tokenUri + ": " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            // 기타 예외 처리
            throw new RuntimeException("Unexpected error while exchanging authorization code: " + e.getMessage(), e);
        }
    }
}
