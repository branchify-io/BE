package com.example.merging.s3;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class S3Service {

    private final S3Presigner presigner;
    private final RestTemplate restTemplate;
    private static final String AI_SERVER_URL = "http://localhost:8000/api/process-pdf";

    public S3Service(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.presigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2) // AWS 리전
                .credentialsProvider(DefaultCredentialsProvider.create()) // IAM 역할 자동 인증
                .build();

    }

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    // S3 Presigned URL 생성
    public String generatePresignedUrl(String fileName) {
        String objectKey = "uploads/" + fileName; // 파일 저장 경로 지정

        // Presigned URL 요청 생성
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(10)) // Presigned URL 유효시간 10분
                .putObjectRequest(req -> req.bucket(bucketName).key(objectKey))
                .build();

        // Presigned URL 생성
        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        URL presignedUrl = presignedRequest.url();

        return presignedUrl.toString();
    }

    // S3에 업로드될 파일의 URL
    public String getS3FileUrl(String fileName) {
        return "https://" + bucketName + ".s3" + Region.AP_NORTHEAST_2 + ".amazonaws.com/uploads/" + fileName;
    }
}
