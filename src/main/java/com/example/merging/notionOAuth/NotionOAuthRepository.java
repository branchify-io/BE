package com.example.merging.notionOAuth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotionOAuthRepository extends JpaRepository<NotionOAuth, Long> {

    Optional<NotionOAuth> findByAssistant_AssistantNameAndAssistant_User_Email(String assistantName, String userEmail);
}
