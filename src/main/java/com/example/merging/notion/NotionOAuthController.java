package com.example.merging.notion;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notion/oauth")
public class NotionOAuthController {

    private final NotionOAuthService notionOAuthService;

    public NotionOAuthController(NotionOAuthService notionOAuthService) {
        this.notionOAuthService = notionOAuthService;
    }

    // Authorization URL 요청
    @GetMapping("/connect")
    public ResponseEntity<String> connectToNotion(@RequestParam String userEmail, @RequestParam String assistantName) {

        // Notion Authorization URL 생성
        String authUrl = notionOAuthService.getAuthorizationUrl(userEmail, assistantName);

        // HTTP 302 Redirect 응답
        return ResponseEntity.status(HttpStatus.FOUND) // 302 상태 코드
                .header("Location", authUrl) // Location 헤더에 URL 설정
                .build();
    }

    // Notion OAuth 콜백 처리
    @GetMapping("/callback")
    public ResponseEntity<String> handleCallback(@RequestParam String code, @RequestParam String state) {
        try {
            String[] stateParts = state.split(":");
            String userEmail = stateParts[0];
            String assistantName = stateParts[1];

            // Authorization Code 처리
            notionOAuthService.exchangeAuthorizationCode(code, userEmail, assistantName);

            return ResponseEntity.ok("Notion account connected successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Failed to connect Notion account: " + e.getMessage());
        }
    }
}
