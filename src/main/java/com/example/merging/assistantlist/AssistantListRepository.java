package com.example.merging.assistantlist;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssistantListRepository extends JpaRepository<AssistantList, String> {

    List<AssistantList> findByUser_Email(String userEmail); // 특정 유저의 모든 Assistant 조회

    Optional<AssistantList> findByAssistantNameAndUser_Email(String assistantName, String userEmail);  // 복합 키 기반 조회
}

