package com.gmail.alexei28.aws.lambda.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

@Configuration
@EnableConfigurationProperties(DocumentProperties.class)
public class DynamoDbConfig {

    @Bean
    public DynamoDbClient dynamoDbClient(DocumentProperties properties) {
        return DynamoDbClient.builder()
                .region(Region.of(properties.region()))
                .build();
    }

    // Позволяет легко мапить объекты Java в элементы DynamoDB с помощью аннотаций.
    // DynamoDbEnhancedClient требует, чтобы поля класса были изменяемыми (мутабельными).
    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder()
                .dynamoDbClient(dynamoDbClient)
                .build();
    }
}