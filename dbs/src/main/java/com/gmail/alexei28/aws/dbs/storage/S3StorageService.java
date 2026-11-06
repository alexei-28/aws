package com.gmail.alexei28.aws.dbs.storage;

import java.time.Duration;

// Реализация будет использовать AWS SDK S3 Presigner.
public interface S3StorageService {

    String generateUploadUrl(
            String s3Key,
            String contentType,
            Duration expiration
    );
}