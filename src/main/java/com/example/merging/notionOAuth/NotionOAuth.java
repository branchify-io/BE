package com.example.merging.notionOAuth;

import com.example.merging.assistantlist.AssistantId;
import com.example.merging.assistantlist.AssistantList;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "notion_oauth")
public class NotionOAuth {

    @EmbeddedId
    private AssistantId id; // 복합 키

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({@JoinColumn(name = "user_email", referencedColumnName = "user_email"),
            @JoinColumn(name = "assistant_name", referencedColumnName = "assistant_name")
    })
    private AssistantList assistant;

    private String accessToken;
    private String refreshToken;
    private String tokenType; // 토큰 타입 (Bearer)
    private String scope; // 권한 범위
    private String workspaceId;
    private String workspaceName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void setUpdatedAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
