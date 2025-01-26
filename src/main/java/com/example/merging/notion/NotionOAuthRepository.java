package com.example.merging.notion;

import com.example.merging.assistantlist.AssistantId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotionOAuthRepository extends JpaRepository<NotionOAuth, AssistantId> {
}
