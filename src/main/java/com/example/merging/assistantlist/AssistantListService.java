package com.example.merging.assistantlist;

import com.example.merging.assistantlist.AssistantList;
import com.example.merging.assistantlist.AssistantListRepository;

import org.springframework.stereotype.Service;
import java.util.List;


@Service
public class AssistantListService {

    private final AssistantListRepository assistantListRepository;

    public AssistantListService(AssistantListRepository assistantListRepository) {
        this.assistantListRepository = assistantListRepository;
    }

    public List<AssistantList> getAssistantList() {
        return assistantListRepository.findAll();
    }
    
}

