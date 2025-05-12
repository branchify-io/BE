package com.example.merging.slackOAuth;

import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/oauth/slack")
public class SlackOAuthController {

    private final SlackOAuthService slackOAuthService;

    public SlackOAuthController(SlackOAuthService slackOAuthService) {
        this.slackOAuthService = slackOAuthService;
    }

    @Value("${slack.redirect.uri}")
    private String slackRedirectUri;

    // Slack OAuth 요청
    @GetMapping("/connect")
    public ResponseEntity<Void> redirectToSlackAuthPage(@RequestParam String userEmail, @RequestParam String assistantName) {
        String state = userEmail + ":" + assistantName;

        String slackAuthUrl = "https://slack.com/oauth/v2/authorize" +
                "?client_id=" + "7867953200945.8391102662993" +
                "&scope=app_mentions:read,bookmarks:read,calls:write,channels:history,chat:write,commands,emoji:read,groups:history,im:history,metadata.message:read,mpim:history,reactions:read" +
                "&redirect_uri=" + slackRedirectUri +
                "&state=" + state;

        return ResponseEntity.status(302).header("Location", slackAuthUrl).build();
    }

    // Slack OAuth Callback 처리
    @GetMapping("/callback")
    public ResponseEntity<String> handleSlackCallback(@RequestParam("code") String code,
                                                      @RequestParam("state") String state) {
        try {
            String[] stateParts = state.split(":");
            String userEmail = stateParts[0];
            String assistantName = stateParts[1];

            // Slack OAuth 콜백이 도착하는지 확인하는 로그
            System.out.println("Slack OAuth callback received!");
            System.out.println("Authorization Code: " + code);
            System.out.println("User Email: " + userEmail);
            System.out.println("Assistant Name: " + assistantName);

            // Authorization Code로 Access Token 교환
            OAuthV2AccessResponse response = slackOAuthService.exchangeSlackToken(code);

            // Slack API 응답 검증
            if (!response.isOk()) {
                System.out.println("Slack API error: " + response.getError()); // 디버깅 로그

                return ResponseEntity.status(400)
                        .body("Slack API error: " + response.getError());
            }

            // 사용자 정보 저장 및 DTO 변환
            SlackOAuthDTO slackUserDTO = slackOAuthService.saveOrUpdateSlackOAuth(response, userEmail, assistantName);

            // 사용자의 Slack 워크스페이스로 리다이렉트
            // String redirectUrl = "slack://open?team=" + slackUserDTO.getWorkspaceId();
            // System.out.println("Redirecting to: " + redirectUrl); // 디버깅 로그

            // return ResponseEntity.status(302).header("Location", redirectUrl).build();

            // OAuth 인증 성공 시 새 창 닫기 위한 JavaScript 포함된 HTML 반환
            String successHtml = "<!DOCTYPE html>" +
                    "<html lang='en'>" +
                    "<head><meta charset='UTF-8'><title>Slack OAuth</title></head>" +
                    "<body>" +
                    "<script>" +
                    "  window.opener.postMessage('slack_auth_success', '*');" + // 부모 창에 알림
                    "  window.close();" + // 현재 창 닫기
                    "</script>" +
                    "<p>Slack account connected successfully! You can close this window.</p>" +
                    "</body>" +
                    "</html>";

            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(successHtml);

        } catch (IOException | SlackApiException e) {
            System.out.println("Error during Slack OAuth callback: " + e.getMessage()); // 디버깅 로그
            String errorHtml = "<!DOCTYPE html>" +
                    "<html lang='en'>" +
                    "<head><meta charset='UTF-8'><title>Slack OAuth</title></head>" +
                    "<body>" +
                    "<p>Failed to connect Slack account: " + e.getMessage() + "</p>" +
                    "</body>" +
                    "</html>";

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.TEXT_HTML).body(errorHtml);
        } catch (Exception e) {
            System.out.println("Unexpected error: " + e.getMessage()); // 디버깅 로그
            String errorHtml = "<!DOCTYPE html>" +
                    "<html lang='en'>" +
                    "<head><meta charset='UTF-8'><title>Slack OAuth</title></head>" +
                    "<body>" +
                    "<p>Unexpected error: " + e.getMessage() + "</p>" +
                    "</body>" +
                    "</html>";

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).contentType(MediaType.TEXT_HTML).body(errorHtml);        }
    }
}
