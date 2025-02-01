package com.example.merging.slackOAuth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SlackOAuthRepository extends JpaRepository<SlackOAuth, Long> {

    Optional<SlackOAuth> findByAssistant_AssistantNameAndAssistant_User_Email(String assistantName, String userEmail);
}
