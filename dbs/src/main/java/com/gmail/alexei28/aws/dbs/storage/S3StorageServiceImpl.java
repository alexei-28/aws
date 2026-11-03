package com.gmail.alexei28.aws.dbs.storage;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Profile("local") // только для локального окружения
public class S3StorageServiceImpl implements S3StorageService {

    @Override
    public String generateUploadUrl(
            String s3Key,
            String contentType,
            Duration expiration
    ) {
        return "http://localhost:9000/mock-upload/" + s3Key;
    }
}