package com.example.merging.jiraOAuth;

import com.example.merging.assistantlist.AssistantList;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "jira_oauth")
public class JiraOAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE) // AssistantList 삭제 시 자동 삭제
    @JoinColumns({
            @JoinColumn(name = "assistant_name", referencedColumnName = "assistantName", nullable = false),
            @JoinColumn(name = "user_email", referencedColumnName = "user_email", nullable = false)
    })
    private AssistantList assistant;

    @Column(name = "jira_project_id")
    private String projectId;

    @Column(name = "jira_project_name")
    private String projectName;

    @Column(name = "jira_project_url")
    private String projectUrl;

    @Column(name = "jira_access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;
    
    @Column(name = "jira_refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "jira_email")
    private String email;

    @Column(name = "jira_project_key")
    private String projectKey;
}