package com.example.merging.jiraOAuth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JiraOAuthRepository extends JpaRepository<JiraOAuth, Long> {
    Optional<JiraOAuth> findByAssistant_Id(Long assistantId);
}