package com.example.merging.s3;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("api/ai")
public class sendToAIController {

    private final sendToAIService sendToAIService;

    public sendToAIController(com.example.merging.s3.sendToAIService sendToAIService) {
        this.sendToAIService = sendToAIService;
    }

    @PostMapping("/notify-upload")
    public String notifyUpload(@RequestBody Map<String, String> request) {
        String fileUrl = request.get("fileUrl");
        sendToAIService.sendFileToAI(fileUrl);
        return "AI 서버에 파일 URL이 전달되었습니다.";
    }
}
