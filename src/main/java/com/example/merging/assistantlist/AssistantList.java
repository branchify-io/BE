package com.example.merging.assistantlist;

import java.util.List;
import java.util.Map;

import com.example.merging.converter.StringListConverter;
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
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.List;

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

    @Lob
    @Column(columnDefinition = "TEXT")
    private String promptDetail;

    private String prompt;

    @Convert(converter = StringListConverter.class) // JSON 문자열 변환을 위한 컨버터
    @Column(columnDefinition = "TEXT") // 길이 제한을 피하기 위해 TEXT 사용
    private List<String> actionTag;

    private String modelName;
    private String openaiApiKey;
    private String status;
    private Boolean isConnect;

    @Column(name = "s3_file_url")
    private String s3FileUrl;

    @Column(name = "notion_page_list", columnDefinition = "TEXT")
    private String notionPages;  // JSON 문자열로 저장

    @JsonManagedReference
    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE) // FK 연결된 엔티티 자동 삭제
    private NotionOAuth notionOAuth;

    @JsonManagedReference
    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    @OnDelete(action = OnDeleteAction.CASCADE) // FK 연결된 엔티티 자동 삭제
    private SlackOAuth slackOAuth;

    public String getNotionPages() {
        return notionPages;
    }

    public void setNotionPages(String notionPages) {
        this.notionPages = notionPages;
    }

}
