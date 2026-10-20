package com.gmail.alexei28.aws.task02.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Имена полей в record (bucket, region, queueUrl) автоматически сопоставляются с ключами конфигурации

// Spring Boot ищет в файле application.yml блок, который начинается с app: и внутри него storage:
@ConfigurationProperties(prefix = "app.aws")
public record DocumentProperties(
        String bucket,
        String region,
        String queueUrl) {

    public DocumentProperties {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 bucket must be configured");
        }

        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("AWS region must be configured");
        }

        if (queueUrl == null || queueUrl.isBlank()) {
            throw new IllegalArgumentException("AWS queueUrl must be configured");
        }
    }
}