package com.gmail.alexei28.aws.task01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@Validated
// Имена полей в record (region, bucketName, endpointOverride) автоматически сопоставляются с ключами конфигурации
// Spring Boot ищет в файле application.yml блок, который начинается с aws: и внутри него s3:
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        // aws.s3.region -> запишется в поле region
        @NotBlank String region,

        // aws.s3.bucket-name -> запишется в поле bucketName (Spring автоматически преобразует kebab-case bucket-name в camelCase bucketName)
        @NotBlank String bucketName,

        String endpointOverride // aws.s3.endpoint-override -> запишется в поле endpointOverride
) {}