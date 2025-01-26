package com.example.merging.assistantlist;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

// 복합키 클래스
@Embeddable
@Getter
@Setter
public class AssistantId implements Serializable {

    @Column(name = "user_email", nullable = false)
    private String userEmail; // 사용자 이메일

    @Column(name = "assistant_name", nullable = false)
    private String assistantName; // Assistant 이름

    // 기본 생성자
    public AssistantId() {}

    public AssistantId(String userEmail, String assistantName) {
        this.userEmail = userEmail;
        this.assistantName = assistantName;
    }
}
