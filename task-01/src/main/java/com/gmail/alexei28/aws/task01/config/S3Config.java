package com.gmail.alexei28.aws.task01.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

@Configuration
public class S3Config {

    @Bean
    public S3Client s3Client(S3Properties properties) {
        S3ClientBuilder builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(DefaultCredentialsProvider.create());

        if (properties.endpointOverride() != null && !properties.endpointOverride().isBlank()) {
            builder.endpointOverride(URI.create(properties.endpointOverride()))
                    .forcePathStyle(true); // Требуется для MinIO / LocalStack
        }

        return builder.build();
    }
}