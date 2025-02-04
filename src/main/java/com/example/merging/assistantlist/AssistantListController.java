package com.example.merging.assistantlist;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
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
    public ResponseEntity<String> createAssistant(@RequestBody AssistantList assistantList, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new RuntimeException("User not authenticated");
        }
        String userEmail = authentication.getName();
        assistantListService.createAssistant(assistantList, userEmail);
        return ResponseEntity.ok("ok");
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

}
