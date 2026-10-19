
package com.gmail.alexei28.aws.task01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

// Имена полей в record (bucket, region, allowedContentTypes) автоматически сопоставляются с ключами конфигурации
// Spring Boot ищет в файле application.yml блок, который начинается с app: и внутри него storage:
@ConfigurationProperties(prefix = "app.storage")
public record DocumentProperties(
        String bucket,
        String region,
        long maxFileSize,
        List<String> allowedContentTypes) {

    public DocumentProperties {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 bucket must be configured");
        }

        if (region == null || region.isBlank()) {
            throw new IllegalArgumentException("AWS region must be configured");
        }

        if (maxFileSize <= 0) {
            throw new IllegalArgumentException("Maximum file size must be positive");
        }
        allowedContentTypes = List.copyOf(allowedContentTypes);
    }
}
