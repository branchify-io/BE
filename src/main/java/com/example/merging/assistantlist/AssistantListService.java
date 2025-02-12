package com.example.merging.assistantlist;

import com.example.merging.user.UserRepository;
import com.example.merging.user.User;
import com.example.merging.notionOAuth.NotionOAuthRepository;
import com.example.merging.notionOAuth.NotionOAuth;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class AssistantListService {

    private final AssistantListRepository assistantListRepository;
    private final UserRepository userRepository;
    private final NotionOAuthRepository notionOAuthRepository;
    private final RestTemplate restTemplate;

    @Value("${notion.api.base-url}")
    private String notionApiBaseUrl;

    public AssistantListService(AssistantListRepository assistantListRepository, UserRepository userRepository, NotionOAuthRepository notionOAuthRepository, RestTemplate restTemplate) {
        this.assistantListRepository = assistantListRepository;
        this.userRepository = userRepository;
        this.notionOAuthRepository = notionOAuthRepository;
        this.restTemplate = restTemplate;
    }

    public List<AssistantList> getAssistantList() {
        return assistantListRepository.findAll();
    }
    
    public void createAssistant(AssistantList assistantList, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        assistantList.setUser(user);
        assistantListRepository.save(assistantList);
    }

    public void updateActionTag(String userEmail, String assistantName, String actionTag) {
        AssistantList assistant = assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
                .orElseThrow(() -> new RuntimeException("Assistant not found or unauthorized"));

        assistant.setActionTag(actionTag);
        assistantListRepository.save(assistant);
    }


    public Object getNotionPages(Long assistantId, String userEmail) {
        // Assistant 정보 조회
        AssistantList assistant = assistantListRepository.findById(assistantId)
            .orElseThrow(() -> new RuntimeException("Assistant not found"));

        // 사용자 검증
        if (!assistant.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("Unauthorized access");
        }

        // Notion OAuth 토큰 조회
        NotionOAuth notionOAuth = notionOAuthRepository
            .findByAssistant_AssistantNameAndAssistant_User_Email(
                assistant.getAssistantName(), userEmail)
            .orElseThrow(() -> new RuntimeException("Notion connection not found"));

        // Notion API 호출
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(notionOAuth.getAccessToken());
        headers.set("Notion-Version", "2022-06-28");
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<Object> response = restTemplate.exchange(
            notionApiBaseUrl + "/search",
            HttpMethod.POST,
            request,
            Object.class
        );

        return response.getBody();
    }

    // Assistant 검색 (AI 개발 검색용도)
    public AssistantList searchAssistant(String userEmail, String assistantName) {
        return assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
                .orElseThrow(() -> new RuntimeException("Assistant not found"));
    }
}

