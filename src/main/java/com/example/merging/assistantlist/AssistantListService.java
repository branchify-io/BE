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
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashMap;

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


    public Object getNotionPages(String assistantName, String userEmail) {
        // Assistant 정보 조회
        AssistantList assistant = assistantListRepository.findByAssistantName(assistantName)
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

        ResponseEntity<String> response = restTemplate.exchange(
            notionApiBaseUrl + "/search",
            HttpMethod.POST,
            request,
            String.class
        );

        JSONObject jsonObject = new JSONObject(response.getBody());
        JSONArray results = jsonObject.getJSONArray("results");
        JSONArray newResults = new JSONArray();
        for (int i = 0; i < results.length(); i++) {
            System.out.println(i);
            String lastEditedTime = "";
            String title = "";
            String url = "";
            String pageId = "";
            JSONObject parent = null;
            JSONObject page = results.getJSONObject(i);
            if(page.getString("object").equals("page")) {
                System.out.println("page");
                lastEditedTime = page.getString("last_edited_time");
                url = page.getString("url");
                pageId = page.getString("id");
                parent = page.getJSONObject("parent");
                // properties 객체 가져오기
                JSONObject properties = page.getJSONObject("properties");
                // properties의 모든 키를 순회하면서 title type 찾기
                for (String key : properties.keySet()) {
                    JSONObject property = properties.getJSONObject(key);
                    if (property.getString("type").equals("title")) {
                        JSONArray titleArray = property.getJSONArray("title");
                        if (titleArray.length() > 0) {
                            title = titleArray.getJSONObject(0).getString("plain_text");
                        }
                        break;
                    }
                }
            } else if (page.getString("object").equals("database")) {
                System.out.println("database");
                lastEditedTime = page.getString("last_edited_time");
                url = page.getString("url");
                pageId = page.getString("id");
                parent = page.getJSONObject("parent");
                if(page.has("title")) {
                    JSONArray titleArray = page.getJSONArray("title");
                    if(titleArray.length() > 0) {
                        title = titleArray.getJSONObject(0).getString("plain_text");
                    }
                } else {
                    JSONObject properties = page.getJSONObject("properties");
                    for(String key : properties.keySet()) {
                        JSONObject property = properties.getJSONObject(key);
                        if(property.getString("type").equals("title")) {
                            JSONArray titleArray = property.getJSONArray("title");
                            if(titleArray.length() > 0) {
                                title = titleArray.getJSONObject(0).getString("plain_text");
                            }
                        }
                    }
                }
            }
            JSONObject pageInfo = new JSONObject();
            pageInfo.put("lastEditedTime", lastEditedTime);
            pageInfo.put("title", title); 
            pageInfo.put("url", url);
            pageInfo.put("pageId", pageId);
            pageInfo.put("parent", parent);
            newResults.put(i, pageInfo);
        }

        List<Map<String, Object>> restructuredPages = restructureNotionPages(newResults.toString());
        
        return restructuredPages;
    }

    public void saveNotionPages(String assistantName, String userEmail, String notionPages) {
        AssistantList assistant = assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
            .orElseThrow(() -> new RuntimeException("Assistant not found"));

        assistant.setNotionPages(notionPages);
        assistantListRepository.save(assistant);
    }


    // Assistant 검색 (AI 개발 검색용도)
    public AssistantList searchAssistant(String userEmail, String assistantName) {
        return assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
                .orElseThrow(() -> new RuntimeException("Assistant not found"));
    }

    public List<Map<String, Object>> restructureNotionPages(String notionPagesJson) {
        JSONArray originalArray = new JSONArray(notionPagesJson);
        List<Map<String, Object>> result = new ArrayList<>();
        Map<String, Map<String, Object>> pageMap = new HashMap<>();
        
        // 1. 모든 페이지를 Map으로 변환하여 pageId를 키로 저장
        for (int i = 0; i < originalArray.length(); i++) {
            JSONObject page = originalArray.getJSONObject(i);
            Map<String, Object> pageData = new HashMap<>();
            pageData.put("lastEditedTime", page.getString("lastEditedTime"));
            pageData.put("title", page.getString("title"));
            pageData.put("url", page.getString("url"));
            pageData.put("pageId", page.getString("pageId"));
            pageData.put("parent", page.getJSONObject("parent").toMap());
            pageData.put("children", new ArrayList<Map<String, Object>>());
            
            pageMap.put(page.getString("pageId"), pageData);
        }
        
        // 2. 각 페이지를 순회하면서 부모-자식 관계 설정
        for (Map<String, Object> page : pageMap.values()) {
            Map<String, Object> parent = (Map<String, Object>) page.get("parent");
            String parentType = (String) parent.get("type");
            
            if (parentType.equals("workspace")) {
                // 최상위 페이지는 결과 리스트에 직접 추가
                result.add(page);
            } else if (parentType.equals("page_id")) {
                // 부모 페이지의 children에 현재 페이지 추가
                String parentId = (String) parent.get("page_id");
                Map<String, Object> parentPage = pageMap.get(parentId);
                if (parentPage != null) {
                    List<Map<String, Object>> children = (List<Map<String, Object>>) parentPage.get("children");
                    children.add(page);
                }
            }
        }
        
        // 3. parent 정보 제거
        removeParentInfo(result);
        
        return result;
    }

    // 재귀적으로 모든 페이지와 하위 페이지에서 parent 정보 제거
    private void removeParentInfo(List<Map<String, Object>> pages) {
        for (Map<String, Object> page : pages) {
            page.remove("parent");
            List<Map<String, Object>> children = (List<Map<String, Object>>) page.get("children");
            if (children != null && !children.isEmpty()) {
                removeParentInfo(children);
            }
        }
    }
}

