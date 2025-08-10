package com.example.merging.jiraOAuth;

import com.example.merging.assistantlist.AssistantList;
import com.example.merging.assistantlist.AssistantListRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JiraOAuthService {

    @Value("${jira.client.id}")
    private String clientId;

    @Value("${jira.client.secret}")
    private String clientSecret;

    @Value("${jira.client.redirect.uri}")
    private String redirectUri;

    private final AssistantListRepository assistantListRepository;
    private final JiraOAuthRepository jiraOAuthRepository;
    private final WebClient webClient;

    public JiraOAuthService(AssistantListRepository assistantListRepository,
                            JiraOAuthRepository jiraOAuthRepository,
                            WebClient.Builder webClientBuilder) {
        this.assistantListRepository = assistantListRepository;
        this.jiraOAuthRepository = jiraOAuthRepository;
        this.webClient = webClientBuilder.build();
    }

    // Jira OAuth URL 생성
    public String generateAuthorizationUrl(Long assistantId) {
        String scopes = "read:jira-work manage:jira-project manage:jira-configuration read:jira-user write:jira-work offline_access";
        return String.format(
                "https://auth.atlassian.com/authorize?audience=api.atlassian.com&client_id=%s&scope=%s&redirect_uri=%s&state=%s&response_type=code&prompt=consent",
                clientId, scopes, redirectUri, assistantId
        );
    }

    // Authorization code를 Access Token으로 교환
    @Transactional
    public Map<String, String> exchangeAuthorizationCodeForTokens(String code) {
        Map<String, String> body = new HashMap<>();
        body.put("grant_type", "authorization_code");
        body.put("client_id", clientId);
        body.put("client_secret", clientSecret);
        body.put("code", code);
        body.put("redirect_uri", redirectUri);

        Map<String, Object> response = webClient.post()
                .uri("https://auth.atlassian.com/oauth/token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        if (response == null || !response.containsKey("access_token")) {
            throw new RuntimeException("Failed to exchange authorization code for access token");
        }

        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", response.get("access_token").toString());
        if (response.containsKey("refresh_token")) {
            tokens.put("refresh_token", response.get("refresh_token").toString());
        }
        return tokens;
    }
    
    // assistantId로 Jira 프로젝트 목록 가져오기
    public List<JiraProjectInfoDTO> getJiraProjectsByAssistantId(Long assistantId) {
        JiraOAuth jiraOAuth = jiraOAuthRepository.findByAssistant_Id(assistantId)
                .orElseThrow(() -> new RuntimeException("Jira OAuth information not found for assistant id: " + assistantId));
        
        return getJiraProjects(jiraOAuth.getAccessToken());
    }

    // Jira API를 호출하여 프로젝트 목록 반환
    public List<JiraProjectInfoDTO> getJiraProjects(String accessToken) {
        String cloudIdUrl = "https://api.atlassian.com/oauth/token/accessible-resources";

        JsonNode cloudResponse = webClient.get()
                .uri(cloudIdUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (cloudResponse == null || cloudResponse.isEmpty()) {
            throw new RuntimeException("Failed to fetch Cloud ID. The token may not have access to any Jira sites.");
        }

        String cloudId = cloudResponse.get(0).get("id").asText();
        String projectUrl = String.format("https://api.atlassian.com/ex/jira/%s/rest/api/3/project", cloudId);

        JsonNode projectResponse = webClient.get()
                .uri(projectUrl)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<JiraProjectInfoDTO> projects = new ArrayList<>();
        if (projectResponse != null && projectResponse.isArray()) {
            projectResponse.forEach(project -> projects.add(new JiraProjectInfoDTO(
                    project.get("id").asText(),
                    project.get("name").asText(),
                    project.get("self").asText(),
                    project.get("key").asText()
            )));
        }

        return projects;
    }

    // Jira 프로젝트 정보를 DB에 저장
    @Transactional
    public void saveJiraOAuth(Long assistantId, String accessToken, String refreshToken, JiraProjectInfoDTO project, String email) {
        AssistantList assistant = assistantListRepository.findById(assistantId)
                .orElseThrow(() -> new RuntimeException("Assistant not found with id: " + assistantId));

        JiraOAuth jiraOAuth = jiraOAuthRepository.findByAssistant_Id(assistantId).orElse(new JiraOAuth());
        jiraOAuth.setAssistant(assistant);
        jiraOAuth.setAccessToken(accessToken);
        jiraOAuth.setRefreshToken(refreshToken);
        jiraOAuth.setProjectId(project.getId());
        jiraOAuth.setProjectName(project.getName());
        jiraOAuth.setProjectUrl(project.getUrl());
        jiraOAuth.setProjectKey(project.getKey());
        jiraOAuth.setEmail(email);

        jiraOAuthRepository.save(jiraOAuth);
    }
}
