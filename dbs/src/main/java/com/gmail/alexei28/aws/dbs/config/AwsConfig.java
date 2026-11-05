package com.gmail.alexei28.aws.dbs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
@Profile("ec2") // Spring будет создавать DynamoDbClient только при активном профиле ec2
public class AwsConfig {

    @Bean
    public S3Presigner s3Presigner(@Value("${aws.region}") String region) {
        return S3Presigner.builder()
                .region(Region.of(region))
                // Если нужно использовать кастомные креды или эндпоинт (например, MinIO), их можно указать здесь
                .build();
    }
}