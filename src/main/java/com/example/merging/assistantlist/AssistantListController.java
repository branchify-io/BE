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
    public List<AssistantList> getAssistantList() {
        return assistantListService.getAssistantList();
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
    public ResponseEntity<String> updateAssistantAction(
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

        return ResponseEntity.ok("Assistant action updated");
    }

    @GetMapping("/connect")
    public ResponseEntity<?> getNotionPages(@RequestParam Long assistantId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        
        String userEmail = authentication.getName();
        System.out.println("userEmail: " + userEmail);
        try {
            Object pageList = assistantListService.getNotionPages(assistantId, userEmail);
            return ResponseEntity.ok(pageList);
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
