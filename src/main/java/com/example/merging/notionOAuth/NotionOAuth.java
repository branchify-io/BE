package com.example.merging.notionOAuth;

import com.example.merging.assistantlist.AssistantList;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notion_oauth")
public class NotionOAuth {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonBackReference
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "assistant_name", referencedColumnName = "assistantName", nullable = false),
            @JoinColumn(name = "user_email", referencedColumnName = "user_email", nullable = false)
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
