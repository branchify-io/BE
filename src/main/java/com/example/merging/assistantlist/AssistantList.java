package com.example.merging.assistantlist;

import java.util.List;
import java.util.Map;

import com.example.merging.notionOAuth.NotionOAuth;
import com.example.merging.slackOAuth.SlackOAuth;
import com.fasterxml.jackson.annotation.JsonManagedReference;
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
    private String prompt;

    private String modelName;
    private String openaiApiKey;

    private String actionTag;
    private String status;

    @Column(name = "notion_page_list", columnDefinition = "TEXT")
    private String notionPages;  // JSON 문자열로 저장

    @JsonManagedReference
    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    private NotionOAuth notionOAuth;

    @JsonManagedReference
    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    private SlackOAuth slackOAuth;

    public String getNotionPages() {
        return notionPages;
    }

    public void setNotionPages(String notionPages) {
        this.notionPages = notionPages;
    }

}
