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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

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

    public String getNotionPageId() {
        if (this.notionOAuth == null) {
            throw new RuntimeException("Notion OAuth 연결이 필요합니다.");
        }

        try {
            // Notion API 호출을 위한 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(this.notionOAuth.getAccessToken());
            headers.set("Notion-Version", "2022-06-28");

            // search API 요청 본문 구성 - workspace root 페이지만 검색
            String requestBody = "{\"filter\": {\"value\": \"page\", \"property\": \"object\"}}";
            
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);
            
            // RestTemplate을 사용하여 API 호출
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.notion.com/v1/search",
                HttpMethod.POST,
                requestEntity,
                String.class
            );

            // 응답 파싱
            JSONObject jsonResponse = new JSONObject(response.getBody());
            JSONArray results = jsonResponse.getJSONArray("results");

            // workspace root 페이지 찾기
            for (int i = 0; i < results.length(); i++) {
                JSONObject page = results.getJSONObject(i);
                if (page.has("parent") && page.getJSONObject("parent").has("type")) {
                    String parentType = page.getJSONObject("parent").getString("type");
                    if ("workspace".equals(parentType)) {
                        String pageId = page.getString("id");
                        System.out.println("워크스페이스 루트 페이지 ID: " + pageId);
                        return pageId;
                    }
                }
            }

            throw new RuntimeException("워크스페이스 루트 페이지를 찾을 수 없습니다.");
            
        } catch (Exception e) {
            throw new RuntimeException("Notion 워크스페이스 루트 페이지 ID를 가져오는데 실패했습니다: " + e.getMessage());
        }
    }

    public void setNotionPages(String notionPages) {
        this.notionPages = notionPages;
    }

}
