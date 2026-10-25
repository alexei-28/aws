package com.gmail.alexei28.aws.lambda.infrastructure;

import com.gmail.alexei28.aws.lambda.processor.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Component
public class S3DocumentStorage implements DocumentStorage {
    private final S3Client s3Client;
    private static final Logger logger = LoggerFactory.getLogger(S3DocumentStorage.class);

    public S3DocumentStorage(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public byte[] download(String bucket, String objectKey) {
        logger.info("download, AWS S3 Bucket: {}, objectKey: {}", bucket, objectKey);
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        logger.info("download, successfully downloaded(pseudo) from AWS S3 Bucket!");
        return response.asByteArray();
    }
}