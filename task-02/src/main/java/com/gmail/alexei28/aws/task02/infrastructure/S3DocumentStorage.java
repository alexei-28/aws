package com.gmail.alexei28.aws.task02.infrastructure;

import com.gmail.alexei28.aws.task02.processor.DocumentStorage;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@Component
public class S3DocumentStorage implements DocumentStorage {
    private final S3Client s3Client;

    public S3DocumentStorage(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public byte[] download(String bucket, String objectKey) {
        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();

        ResponseBytes<GetObjectResponse> response =
                s3Client.getObjectAsBytes(request);

        return response.asByteArray();
    }
}