package com.example.merging.assistantlist;

import com.example.merging.notionOAuth.NotionOAuth;
import com.example.merging.slackOAuth.SlackOAuth;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.merging.user.User;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssistantList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", nullable = false)
    private User user;

    @Column(nullable = false)
    private String assistantName;

    private String actionTag;
    private String modelName;
    private String status;
    
    private String prompt;
    private String openaiApiKey;
    
    private String notionPageList;  // JSON 형태로 페이지 리스트 저장

    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    private NotionOAuth notionOAuth;

    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    private SlackOAuth slackOAuth;

}
