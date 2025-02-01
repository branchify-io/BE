package com.example.merging.slackOAuth;

import com.example.merging.assistantlist.AssistantList;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "slack_oauth")
public class SlackOAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "assistant_name", referencedColumnName = "assistantName", nullable = false),
            @JoinColumn(name = "user_email", referencedColumnName = "user_email", nullable = false)
    })
    private AssistantList assistant;

    private String workspaceId; // Slack 워크스페이스 ID
    private String workspaceName; // Slack 워크스페이스 이름
    private String accessToken;
}
