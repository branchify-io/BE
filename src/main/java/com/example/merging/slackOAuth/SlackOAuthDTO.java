package com.example.merging.slackOAuth;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SlackOAuthDTO {
    private Long id;
    private String assistantName;
    private String userEmail;
    private String accessToken;
    private String workspaceId; // Slack 워크스페이스 ID
    private String workspaceName; // Slack 워크스페이스 이름
}
