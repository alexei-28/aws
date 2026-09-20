package com.gmail.alexei28.aws.task01.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.NotBlank;

@Validated
@ConfigurationProperties(prefix = "aws.s3")
public record S3Properties(
        @NotBlank String region,
        @NotBlank String bucketName,
        String endpointOverride
) {}