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
import java.util.HashSet;
import java.util.Set;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.util.concurrent.TimeUnit;

@Service
public class AssistantListService {

    private final AssistantListRepository assistantListRepository;
    private final UserRepository userRepository;
    private final NotionOAuthRepository notionOAuthRepository;
    private final RestTemplate restTemplate;

    @Value("${notion.api.base-url}")
    private String notionApiBaseUrl;

    @Value("${spring.web.resources.static-locations}")
    private String resourceLocation;

    public AssistantListService(AssistantListRepository assistantListRepository, UserRepository userRepository, NotionOAuthRepository notionOAuthRepository, RestTemplate restTemplate) {
        this.assistantListRepository = assistantListRepository;
        this.userRepository = userRepository;
        this.notionOAuthRepository = notionOAuthRepository;
        this.restTemplate = restTemplate;
    }

    public List<AssistantList> getAssistantList(String userEmail) {
        return assistantListRepository.findByUser_Email(userEmail);
    }
    
    public void createAssistant(AssistantList assistantList, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new RuntimeException("User not found"));

        assistantList.setUser(user);
        assistantListRepository.save(assistantList);
    }

    public void updateActionTag(String userEmail, String assistantName, List<String> actionTag) {
        AssistantList assistant = assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
                .orElseThrow(() -> new RuntimeException("Assistant not found or unauthorized"));

        assistant.setActionTag(actionTag);
        assistantListRepository.save(assistant);
    }

    public void updateAssistantS3Url(String userEmail, String assistantName, String s3FileUrl) {
        AssistantList assistant = assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
                .orElseThrow(() -> new RuntimeException("Assistant not found or unauthorized"));

        assistant.setS3FileUrl(s3FileUrl);
        assistantListRepository.save(assistant);
    }

    // 생성된 assistant를 AI 서버로 전송
    public void sendAssistantToAIServer(String userEmail, String assistantName) {
        // assistant 정보 조회
        AssistantList assistant = assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
                .orElseThrow(() -> new RuntimeException("Assistant not found"));

        // AI 서버로 전송
        String aiServerUrl = "http://localhost:8000/api/process-assistant";
        restTemplate.postForEntity(aiServerUrl, assistant, String.class);
    }

    private List<Map<String, Object>> findChangedPages(JSONArray oldJsonArray, JSONArray newJsonArray) {
        List<Map<String, Object>> changedPages = new ArrayList<>();
        
        // 재귀적으로 페이지와 그 하위 페이지들을 비교
        for (int i = 0; i < oldJsonArray.length(); i++) {
            JSONObject oldPage = oldJsonArray.getJSONObject(i);
            String pageId = oldPage.getString("pageId");
            
            // 새 배열에서 같은 pageId를 가진 페이지 찾기
            JSONObject newPage = findPageById(newJsonArray, pageId);
            Map<String, Object> changedPage = null;
            
            if (oldPage.optBoolean("isChecked", false)) {
                if (newPage == null) {
                    // 페이지가 삭제된 경우
                    changedPage = createChangedPageMap(oldPage, true);
                    changedPages.add(changedPage);
                } else if (!oldPage.getString("lastEditedTime").equals(newPage.getString("lastEditedTime"))) {
                    // lastEditedTime이 변경된 경우
                    changedPage = createChangedPageMap(newPage, false);
                    changedPage.put("previousEditedTime", oldPage.getString("lastEditedTime"));
                    changedPages.add(changedPage);
                }
            }
            
            // 하위 페이지들 처리 - isChecked와 관계없이 항상 처리
            if (oldPage.has("children") && newPage != null) {
                JSONArray oldChildren = oldPage.getJSONArray("children");
                JSONArray newChildren = newPage.has("children") ? 
                    newPage.getJSONArray("children") : new JSONArray();
                
                List<Map<String, Object>> childChanges = findChangedPages(oldChildren, newChildren);
                if (!childChanges.isEmpty()) {
                    if (changedPage == null) {
                        changedPage = createChangedPageMap(oldPage, false);
                        changedPages.add(changedPage);
                    }
                    changedPage.put("children", childChanges);
                }
            }
        }
        
        // 새로운 페이지 확인
        for (int i = 0; i < newJsonArray.length(); i++) {
            JSONObject newPage = newJsonArray.getJSONObject(i);
            String pageId = newPage.getString("pageId");
            
            if (findPageById(oldJsonArray, pageId) == null) {
                Map<String, Object> changedPage = createChangedPageMap(newPage, false);
                changedPage.put("previousEditedTime", "");
                
                // 새 페이지의 하위 페이지들도 새로운 페이지로 처리
                if (newPage.has("children")) {
                    List<Map<String, Object>> childChanges = processNewPages(newPage.getJSONArray("children"));
                    if (!childChanges.isEmpty()) {
                        changedPage.put("children", childChanges);
                    }
                }
                
                changedPages.add(changedPage);
            }
        }
        
        return changedPages;
    }

    private JSONObject findPageById(JSONArray array, String pageId) {
        for (int i = 0; i < array.length(); i++) {
            JSONObject page = array.getJSONObject(i);
            if (page.getString("pageId").equals(pageId)) {
                return page;
            }
        }
        return null;
    }

    private Map<String, Object> createChangedPageMap(JSONObject page, boolean isDeleted) {
        Map<String, Object> changedPage = new HashMap<>();
        changedPage.put("pageId", page.getString("pageId"));
        changedPage.put("lastEditedTime", page.getString("lastEditedTime"));
        changedPage.put("title", page.getString("title"));
        changedPage.put("url", isDeleted ? "" : page.getString("url"));
        return changedPage;
    }

    private List<Map<String, Object>> processNewPages(JSONArray pages) {
        List<Map<String, Object>> newPages = new ArrayList<>();
        for (int i = 0; i < pages.length(); i++) {
            JSONObject page = pages.getJSONObject(i);
            Map<String, Object> newPage = createChangedPageMap(page, false);
            newPage.put("previousEditedTime", "");
            
            if (page.has("children")) {
                List<Map<String, Object>> childChanges = processNewPages(page.getJSONArray("children"));
                if (!childChanges.isEmpty()) {
                    newPage.put("children", childChanges);
                }
            }
            
            newPages.add(newPage);
        }
        return newPages;
    }

    public String updateNotionPages(String assistantName, String userEmail) {
        AssistantList assistant = assistantListRepository.findByAssistantNameAndUser_Email(assistantName, userEmail)
            .orElseThrow(() -> new RuntimeException("Assistant not found or unauthorized"));

        String oldNotionPages = assistant.getNotionPages();
        JSONArray oldJsonArray = null;

        if(oldNotionPages == null) {
            oldJsonArray = new JSONArray();
        } else {
            oldJsonArray = new JSONArray(oldNotionPages);
        }
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> newNotionPagesList = (List<Map<String, Object>>) getNotionPages(assistantName, userEmail);
        JSONArray newJsonArray = new JSONArray(newNotionPagesList);
        
        List<Map<String, Object>> changedPages = findChangedPages(oldJsonArray, newJsonArray);
        return new JSONArray(changedPages).toString();
    }

    public String getNotionPages(String assistantName, String userEmail) {
        try {
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

            // 현재 프로젝트의 루트 디렉토리 경로 가져오기
            String projectRoot = System.getProperty("user.dir");
            String scriptPath = projectRoot + "/src/main/resources/scripts/notion/getPages.js";
            String workingDir = projectRoot + "/src/main/resources/scripts/notion";

            System.out.println("Project Root: " + projectRoot);  // 디버깅용
            System.out.println("Script path: " + scriptPath);    // 디버깅용
            System.out.println("Working directory: " + workingDir);  // 디버깅용

            ProcessBuilder processBuilder = new ProcessBuilder("node", 
                scriptPath,
                notionOAuth.getAccessToken(),
                assistant.getNotionPageId(),
                "1");
            
            // 작업 디렉토리 설정
            processBuilder.directory(new File(workingDir));
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // 표준 출력을 읽기 위한 BufferedReader
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            
            // 출력을 먼저 읽음
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }

            // 프로세스 종료를 기다림 (타임아웃 설정)
            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new RuntimeException("프로세스 실행 시간이 초과되었습니다.");
            }

            // 프로세스가 정상 종료되었는지 확인
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                // 에러 스트림 확인
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                StringBuilder errorOutput = new StringBuilder();
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    errorOutput.append(errorLine).append("\n");
                }
                throw new RuntimeException("프로세스가 비정상 종료되었습니다. Exit code: " + exitCode + "\nError: " + errorOutput.toString());
            }

            return output.toString();

        } catch (IOException | InterruptedException e) {
            // InterruptedException이 발생하면 현재 스레드의 인터럽트 상태를 복원
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new RuntimeException("Notion 페이지 정보를 가져오는데 실패했습니다: " + e.getMessage());
        }
    }

    public Object getNotionContent(String assistantName, String userEmail) {
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

        try {
            // 현재 프로젝트의 루트 디렉토리 경로 가져오기
            String projectRoot = System.getProperty("user.dir");
            String scriptPath = projectRoot + "/src/main/resources/scripts/notion/getPages.js";
            String workingDir = projectRoot + "/src/main/resources/scripts/notion";

            System.out.println("Project Root: " + projectRoot);  // 디버깅용
            System.out.println("Script path: " + scriptPath);    // 디버깅용
            System.out.println("Working directory: " + workingDir);  // 디버깅용

            // JSON 문자열 이스케이프 처리
            String notionPages = assistant.getNotionPages();
            
            // 줄바꿈 문자 제거 및 이스케이프 처리
            String escapedStructure = notionPages
                .replace("\r\n", " ")
                .replace("\n", " ")
                .replace("\r", " ")
                .replace("\"", "\\\"");
            
            // 명령행 인자로 전달할 때 따옴표로 감싸기
            escapedStructure = "\"" + escapedStructure + "\"";

            ProcessBuilder processBuilder = new ProcessBuilder("node", 
                scriptPath,
                notionOAuth.getAccessToken(),
                assistant.getNotionPageId(),
                "2",
                escapedStructure
            );
            
            // 디버깅을 위한 명령어 출력
            System.out.println("Executing command: " + String.join(" ", processBuilder.command()));
            
            // 작업 디렉토리 설정
            processBuilder.directory(new File(workingDir));
            
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            // 프로세스의 출력을 읽습니다
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Script output: " + line);  // 로그 추가
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Node script execution failed with exit code: " + exitCode + "\nOutput: " + output.toString());
            }

            // JSON 문자열을 Object로 변환
            return output.toString();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to execute Node.js script: " + e.getMessage(), e);
        }
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
        
        Set<String> processedPages = new HashSet<>();  // 처리된 페이지 추적
        
        // 2. 각 페이지를 순회하면서 부모-자식 관계 설정
        for (Map<String, Object> page : pageMap.values()) {
            Map<String, Object> parent = (Map<String, Object>) page.get("parent");
            String parentType = (String) parent.get("type");
            String pageId = (String) page.get("pageId");
            
            if (!processedPages.contains(pageId)) {
                if (parentType.equals("workspace")) {
                    // 최상위 페이지는 결과 리스트에 직접 추가
                    result.add(page);
                    processedPages.add(pageId);
                } else if (parentType.equals("page_id") || parentType.equals("database_id") || parentType.equals("block_id")) {
                    String parentId = (String) parent.get(parentType);
                    Map<String, Object> parentPage = pageMap.get(parentId);
                    
                    if (parentPage == null) {
                        // 부모 페이지가 없는 경우 최상위 레벨에 추가
                        result.add(page);
                    } else {
                        // 부모 페이지의 children에 현재 페이지 추가
                        List<Map<String, Object>> children = (List<Map<String, Object>>) parentPage.get("children");
                        children.add(page);
                    }
                    processedPages.add(pageId);
                }
            }
        }
        
        // 3. 아직 처리되지 않은 페이지들을 최상위 레벨에 추가
        for (Map<String, Object> page : pageMap.values()) {
            String pageId = (String) page.get("pageId");
            if (!processedPages.contains(pageId)) {
                result.add(page);
            }
        }
        
        // 4. parent 정보 제거
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

