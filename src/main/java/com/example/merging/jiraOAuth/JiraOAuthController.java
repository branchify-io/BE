package com.example.merging.jiraOAuth;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oauth/jira")
public class JiraOAuthController {

    private final JiraOAuthService jiraOAuthService;

    public JiraOAuthController(JiraOAuthService jiraOAuthService) {
        this.jiraOAuthService = jiraOAuthService;
    }

    // Jira OAuth 인증 시작
    @GetMapping("/authorize")
    public ResponseEntity<Map<String, String>> authorizeJira(@RequestParam Long assistantId) {
        String authUrl = jiraOAuthService.generateAuthorizationUrl(assistantId);
        return ResponseEntity.ok(Map.of("authorizationUrl", authUrl));
    }

    // Jira Callback 처리
    @GetMapping("/callback")
    public ResponseEntity<Map<String, String>> handleCallback(@RequestParam String code, @RequestParam("state") Long assistantId) {
        Map<String, String> tokens = jiraOAuthService.exchangeAuthorizationCodeForTokens(code);
        return ResponseEntity.ok(tokens);
    }

    // Jira 프로젝트 목록 조회
    @GetMapping("/projects")
    public ResponseEntity<List<JiraProjectInfoDTO>> getProjects(@RequestHeader("Authorization") String authorization) {
        String accessToken = authorization.replace("Bearer ", "");
        List<JiraProjectInfoDTO> projects = jiraOAuthService.getJiraProjects(accessToken);
        return ResponseEntity.ok(projects);
    }

    // Jira 프로젝트 정보 저장
    @PostMapping("/projects")
    public ResponseEntity<String> saveProject(
            @RequestParam Long assistantId,
            @RequestBody SaveJiraProjectRequestDTO request) {

        String accessToken = request.getAccessToken();
        String refreshToken = request.getRefreshToken();
        JiraProjectInfoDTO project = request.getProject();

        String email = jiraOAuthService.getUserEmail(accessToken);

        jiraOAuthService.saveJiraOAuth(assistantId, accessToken, refreshToken, project, email);
        return ResponseEntity.ok("Jira project linked successfully with refresh token.");
    }
}
