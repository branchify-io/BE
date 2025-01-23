package com.example.merging.assistantlist;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import java.util.List;


@RestController
@RequestMapping("/api/assistantlist")
public class assistantListController {

    private final AssistantListService assistantListService;

    public assistantListController(AssistantListService assistantListService) {
        this.assistantListService = assistantListService;
    }
    

    @GetMapping
    public List<AssistantList> getAssistantList() {
        return assistantListService.getAssistantList();
    }
}
