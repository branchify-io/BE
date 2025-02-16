package com.example.merging.assistantlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssistantListRepository extends JpaRepository<AssistantList, Long> {

    Optional<AssistantList> findByAssistantNameAndUser_Email(String assistantName, String userEmail);

    Optional<AssistantList> findByAssistantName(String assistantName);

    List<AssistantList> findByUser_Email(String userEmail);
}