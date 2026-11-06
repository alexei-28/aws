package com.gmail.alexei28.aws.dbs.storage;

import java.time.Duration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("local") // только для локального окружения
public class S3StorageServiceLocalImpl implements S3StorageService{

    @Override
    public String generateUploadUrl(
            String s3Key,
            String contentType,
            Duration expiration
    ) {
        return "http://localhost:9000/mock-upload/" + s3Key;
    }
}
