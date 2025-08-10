package com.example.merging.jiraOAuth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JiraOAuthRequestDTO {
    private String code; // Authorization Code
    private String slackWorkspaceId; // Slack 워크스페이스 ID
}
