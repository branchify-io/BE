package com.example.merging.jiraOAuth;

import lombok.Data;

@Data
public class SaveJiraProjectRequestDTO {
    private JiraProjectInfoDTO project;
    private String accessToken;
    private String refreshToken;
}
