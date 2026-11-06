package com.gmail.alexei28.aws.dbs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sqs.SqsClient;

@Configuration
@Profile("ec2") // Spring будет создавать DynamoDbClient только при активном профиле ec2
public class AwsProcessingConfig {
    @Bean
    public DynamoDbClient dynamoDbClient(@Value("${aws.region}") String region) {
        return DynamoDbClient.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public SqsClient sqsClient(@Value("${aws.region}") String region) {
        return SqsClient.builder()
                .region(Region.of(region))
                .build();
    }
}