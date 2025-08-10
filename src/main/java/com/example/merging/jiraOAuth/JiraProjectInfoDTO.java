package com.example.merging.jiraOAuth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JiraProjectInfoDTO {
    private String id; // Jira 프로젝트 ID
    private String name; // Jira 프로젝트 이름
    private String url; // Jira 프로젝트 URL
    private String key; // Jira 프로젝트 Key
}
