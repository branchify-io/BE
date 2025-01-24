package com.example.merging.assistantlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AssistantListRepository extends JpaRepository<AssistantList, AssistantId> {
    Optional<AssistantList> findByNotionUserId(String notionUserId);
}

