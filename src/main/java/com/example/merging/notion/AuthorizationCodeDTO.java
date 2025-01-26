package com.example.merging.notion;

import lombok.Data;

// Notion 인증 과정에서 필요한 Authorization Code 정보를 캡처
@Data
public class AuthorizationCodeDTO {
    private String code; // 노션에서 전달받은 Authorization Code
    private String state; // 사용자 상태값 (userEmail 및 assistantName)
}
