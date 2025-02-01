package com.example.merging.assistantlist;

import com.example.merging.notionOAuth.NotionOAuth;
import com.example.merging.slackOAuth.SlackOAuth;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.example.merging.user.User;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AssistantList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", nullable = false)
    private User user;

    @Column(nullable = false)
    private String assistantName;

    private String actionTag;
    private String modelName;
    private String notionPageList;
    private String status;

    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    private NotionOAuth notionOAuth;

    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    private SlackOAuth slackOAuth;
    
}
