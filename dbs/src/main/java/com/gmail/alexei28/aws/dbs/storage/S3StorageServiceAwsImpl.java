package com.gmail.alexei28.aws.dbs.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;

@Service
@Profile("ec2")
public class S3StorageServiceAwsImpl implements S3StorageService {
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private static final Logger logger = LoggerFactory.getLogger(S3StorageServiceAwsImpl.class);

    public S3StorageServiceAwsImpl(S3Presigner s3Presigner, @Value("${aws.s3.bucket}") String bucketName) {
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }

    @Override
    public String generateUploadUrl(String s3Key, String contentType, Duration expiration) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(putObjectRequest)
                .build();
        String uploadUrl = s3Presigner.presignPutObject(presignRequest)
                .url()
                .toString();
        logger.info("generateUploadUrl\nbucketName = {}, s3Key = {}", bucketName, s3Key);
        return uploadUrl;
    }
}