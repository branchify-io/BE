package com.example.merging.notion;

import lombok.Data;

// Notion API에서 Access Token 요청 후 반환받는 데이터를 담는 DTO
@Data
public class NotionTokenResponseDTO {
    private String access_token;
    private String refresh_token;
    private String workspace_id;
    private String workspace_name;
    private String token_type;
    private String scope;
}
