package com.example.demo.domain.s3.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/s3")
@RequiredArgsConstructor
public class S3Controller {

    private final S3Presigner s3Presigner;

    @Value(value = "${aws.s3.bucket}")
    private String bucketName;

    @Value(value = "${aws.region}")
    private String region;

    @GetMapping("/presigned-url")
    public ResponseEntity<Map<String, String>> getPresignedUrl (@RequestParam String filename){
        //1. method makes an unique file name to prevent overwriting -> Unique-UID_imageName.png
        String uniqueFileName = UUID.randomUUID().toString() + "_" + filename;

        //2. method will create a new Put request which the browser/service/app will execute eventually
        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(uniqueFileName)
                .build();

        // 3. method for generating a Presigned URL -> valid for only 5 minutes
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(5))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String presignedUrl = presignedRequest.url().toString();

        //4. method for calculating the final public url where the image will live/be saved in
        String publicUrl = String.format("https://%s.s3.%s.amazonaws.com/%s", bucketName, region, uniqueFileName);

        //5. returns both presigned URL and public URL to React
        return ResponseEntity.ok(Map.of(
                "presignedUrl", presignedUrl,
                "publicUrl", publicUrl
        ));
    }
}
