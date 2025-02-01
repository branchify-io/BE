package com.example.merging.notionOAuth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotionOAuthDTO {
    private Long id;
    private String assistantName;
    private String userEmail;
    private String accessToken;
    private String refreshToken;
    private String workspaceId;
    private String workspaceName;
}
