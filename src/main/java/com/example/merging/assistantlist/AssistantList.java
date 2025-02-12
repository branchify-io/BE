package com.example.merging.assistantlist;

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
    private String prompt;

    @Convert(converter = StringListConverter.class) // JSON 문자열 변환을 위한 컨버터
    @Column(columnDefinition = "TEXT") // 길이 제한을 피하기 위해 TEXT 사용
    private List<String> actionTag;

    private String modelName;
    private String openaiApiKey;
    private String status;
    private String notionPageList;  // JSON 형태로 페이지 리스트 저장

    @JsonManagedReference
    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    private NotionOAuth notionOAuth;

    @JsonManagedReference
    @OneToOne(mappedBy = "assistant", cascade = CascadeType.ALL, orphanRemoval = true)
    private SlackOAuth slackOAuth;

}
