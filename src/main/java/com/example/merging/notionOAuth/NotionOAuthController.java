package com.example.merging.notionOAuth;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/oauth/notion")
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

            // OAuth 인증 성공 시 새 창 닫기 위한 JavaScript 포함된 HTML 반환
            String successHtml = "<!DOCTYPE html>" +
                    "<html lang='en'>" +
                    "<head><meta charset='UTF-8'><title>Notion OAuth</title></head>" +
                    "<body>" +
                    "<script>" +
                    "  window.opener.postMessage('notion_auth_success', '*');" + // 부모 창에 알림
                    "  window.close();" + // 현재 창 닫기
                    "</script>" +
                    "<p>Notion account connected successfully! You can close this window.</p>" +
                    "</body>" +
                    "</html>";

            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(successHtml);
        } catch (Exception e) {
            // OAuth 인증 실패 시 에러 페이지 반환
            String errorHtml = "<!DOCTYPE html>" +
                    "<html lang='en'>" +
                    "<head><meta charset='UTF-8'><title>Notion OAuth</title></head>" +
                    "<body>" +
                    "<p>Failed to connect Notion account: " + e.getMessage() + "</p>" +
                    "</body>" +
                    "</html>";

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_HTML).body(errorHtml);
        }
    }
}
