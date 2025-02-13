package com.example.merging.slackOAuth;

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
@Table(name = "slack_oauth")
public class SlackOAuth {

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

    private String workspaceId; // Slack 워크스페이스 ID
    private String workspaceName; // Slack 워크스페이스 이름
    private String accessToken;
}
