package com.example.merging.assistantlist;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.example.merging.user.User;
@Entity
@Getter
@Setter
public class AssistantList {

    @EmbeddedId
    private AssistantId id; // 복합 키 (user_email + assistant_name)

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userEmail") // 복합 키의 user_email 부분을 매핑
    @JoinColumn(name = "user_email")
    private User user;

    private String notionUserId;
    private String modelName;
    private String notionPageList;
    private String slackWorkspaceId;
    private String status;
    private String actionTag;
    
}
