package com.example.merging.assistantlist;

import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;


@RestController
@RequestMapping("/api/assistantlist")
public class AssistantListController {

    private final AssistantListService assistantListService;

    @Autowired
    public AssistantListController(AssistantListService assistantListService) {
        this.assistantListService = assistantListService;
    }

    @GetMapping
    public List<AssistantList> getAssistantList(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }
        String userEmail = authentication.getName();

        return assistantListService.getAssistantList(userEmail);
    }

    @PostMapping()
    public ResponseEntity<Map<String, String>> createAssistant(@RequestBody AssistantList assistantList, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }
        String userEmail = authentication.getName();

        assistantListService.createAssistant(assistantList, userEmail);

        Map<String, String> response = new HashMap<>();
        response.put("userEmail", userEmail);
        response.put("assistantName", assistantList.getAssistantName());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{assistantName}")
    public ResponseEntity<String> updateAssistant(
            @PathVariable String assistantName,
            @RequestBody Map<String, List<String>> request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }
        String userEmail = authentication.getName();
        List<String> actionTag = request.get("actionTag");

        assistantListService.updateActionTag(userEmail, assistantName, actionTag);

        assistantListService.sendAssistantToAIServer(userEmail, assistantName);

        return ResponseEntity.ok("Assistant action updated");
    }

    @PatchMapping("/{assistantName}/update-s3-url")
    public ResponseEntity<String> updateAssistantS3Url(
            @PathVariable String assistantName,
            @RequestBody Map<String, String> request,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }

        String userEmail = authentication.getName();
        String s3FileUrl = request.get("s3FileUrl");

        if (s3FileUrl == null || s3FileUrl.isEmpty()) {
            return ResponseEntity.badRequest().body("s3FileUrl is required");
        }

        assistantListService.updateAssistantS3Url(userEmail, assistantName, s3FileUrl);

        return ResponseEntity.ok("Assistant S3 URL updated successfully");
    }

    @GetMapping("/notionPages")
    public ResponseEntity<?> getNotionPages(
        @RequestParam("assistantName") String assistantName, 
        Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        String userEmail = authentication.getName();
        System.out.println("userEmail: " + userEmail);
        try {
            Object pageList = assistantListService.getNotionPages(assistantName, userEmail);
            return ResponseEntity.ok(pageList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/notionPages")
    public ResponseEntity<?> saveNotionPages(
            @RequestBody String notionPages,
            @RequestParam String assistantName,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String userEmail = authentication.getName();
        System.out.println("notionPages: " + notionPages);
        try {
            assistantListService.saveNotionPages(assistantName, userEmail, notionPages);
            return ResponseEntity.ok("Notion pages saved successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/notionContent")
    public ResponseEntity<?> getNotionContent(
        @RequestParam("assistantName") String assistantName, 
        Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        String userEmail = authentication.getName();
        System.out.println("userEmail: " + userEmail);
        try {
            Object pageList = assistantListService.getNotionContent(assistantName, userEmail);
            return ResponseEntity.ok(pageList);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/notionUpdate")
    public ResponseEntity<?> updateNotionPages(
            @RequestParam String assistantName,
            Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }

        String userEmail = authentication.getName();
        try {
            String a = assistantListService.updateNotionPages(assistantName, userEmail);
            return ResponseEntity.ok(a);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }


    @GetMapping("/search")
    public ResponseEntity<AssistantList> getAssistant(@RequestParam String userEmail, @RequestParam String assistantName) {

        return Optional.ofNullable(assistantListService.searchAssistant(userEmail, assistantName))
                .map(ResponseEntity::ok) // 값이 있으면 200 OK
                .orElseGet(() -> ResponseEntity.notFound().build()); // 값이 없으면 404 응답
    }

}
