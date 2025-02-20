package com.example.merging.s3;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/s3")
public class S3Controller {

    private final S3Service s3Service;

    public S3Controller(S3Service s3Service) {
        this.s3Service = s3Service;
    }

    @PostMapping("/generate-presigned-url")
    public Map<String, String> generatePresignedUrl(@RequestBody Map<String, String> request) {
        String fileName = request.get("fileName");
        String presignedUrl = s3Service.generatePresignedUrl(fileName);
        String fileUrl = s3Service.getS3FileUrl(fileName);

        return Map.of("presignedUrl", presignedUrl, "fileUrl", fileUrl);
    }
}
