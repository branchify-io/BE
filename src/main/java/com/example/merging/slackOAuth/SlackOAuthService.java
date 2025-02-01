package com.example.merging.slackOAuth;

import com.example.merging.assistantlist.AssistantList;
import com.example.merging.assistantlist.AssistantListRepository;
import com.slack.api.Slack;
import com.slack.api.methods.MethodsClient;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;

@Service
public class SlackOAuthService {

    @Value("${slack.client.secret}")
    private String clientSecret;

    @Value("${slack.redirect.uri}")
    private String redirectUrl;

    private final SlackOAuthRepository slackOAuthRepository;
    private final AssistantListRepository assistantListRepository;

    public SlackOAuthService(SlackOAuthRepository slackOAuthRepository, AssistantListRepository assistantListRepository) {
        this.slackOAuthRepository = slackOAuthRepository;
        this.assistantListRepository = assistantListRepository;
    }

    // Slack OAuth Token 교환
    public OAuthV2AccessResponse exchangeSlackToken(String code) throws IOException, SlackApiException {
        MethodsClient methods = Slack.getInstance().methods();
        return methods.oauthV2Access(OAuthV2AccessRequest.builder()
                .clientId("7867953200945.8391102662993")
                .clientSecret(clientSecret)
                .code(code)
                .redirectUri(redirectUrl)
                .build());
    }

    // Slack 사용자 등록 또는 업데이트
    @Transactional
    public SlackOAuthDTO saveOrUpdateSlackOAuth(OAuthV2AccessResponse response, String userEmail, String assistantName) {
        // Assistant 존재 여부 확인
        AssistantList assistant = assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
                .orElseThrow(() -> new RuntimeException("Assistant not found for user: " + userEmail));

        // Slack OAuth 정보 확인 (기존 정보가 있으면 업데이트, 없으면 새로 생성)
        SlackOAuth slackOAuth = slackOAuthRepository
                .findByAssistant_AssistantNameAndAssistant_User_Email(assistantName, userEmail)
                .orElse(new SlackOAuth());

        // AssistantList 설정 (FK 관계)
        slackOAuth.setAssistant(assistant);

        // Slack API 응답 값 저장
        slackOAuth.setAccessToken(response.getAccessToken());
        slackOAuth.setWorkspaceId(response.getTeam().getId());
        slackOAuth.setWorkspaceName(response.getTeam().getName());

        slackOAuth = slackOAuthRepository.save(slackOAuth);

        // SlackUser 엔티티를 DTO로 변환하여 반환
        SlackOAuthDTO dto = new SlackOAuthDTO();
        dto.setWorkspaceId(slackOAuth.getWorkspaceId());
        dto.setWorkspaceName(slackOAuth.getWorkspaceName());
        dto.setAccessToken(slackOAuth.getAccessToken());
        return dto;
    }
}
