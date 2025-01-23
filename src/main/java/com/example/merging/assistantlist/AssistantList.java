package com.example.merging.assistantlist;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import lombok.Getter;
import lombok.Setter;
import com.example.merging.user.User;
@Entity
@Getter
@Setter
public class AssistantList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email", referencedColumnName = "email")
    private User user;

    private String assistant_name;
    private String assistant_id;
    private String notion_user_id;
    private String model_name;
    private String notion_page_list;
    private String slack_workspace_id;
    private String status;
    private String action_tag;
    
}
