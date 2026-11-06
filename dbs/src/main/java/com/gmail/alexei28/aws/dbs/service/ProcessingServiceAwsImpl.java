
package com.gmail.alexei28.aws.dbs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.alexei28.aws.dbs.dto.response.DocumentStatusResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Profile("ec2") // Spring будет создавать DynamoDbClient только при активном профиле ec2
public class ProcessingServiceAwsImpl implements ProcessingService {
    private final DynamoDbClient dynamoDbClient;
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;

    private final String processingTable;
    private final String retryQueueUrl;
    private static final Logger logger = LoggerFactory.getLogger(ProcessingServiceAwsImpl.class);

    public ProcessingServiceAwsImpl(
            DynamoDbClient dynamoDbClient,
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            @Value("${aws.dynamodb.processing-table}")
            String processingTable,
            @Value("${aws.sqs.retry-queue-url}")
            String retryQueueUrl
    ) {
        this.dynamoDbClient = dynamoDbClient;
        this.sqsClient = sqsClient;
        this.objectMapper = objectMapper;
        this.processingTable = processingTable;
        this.retryQueueUrl = retryQueueUrl;
    }

    /**
     * Retrieves document processing status from DynamoDB.
     */
    @Override
    public DocumentStatusResponse getStatus(UUID documentId) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(processingTable)
                .key(Map.of(
                        "documentId",
                        AttributeValue.builder()
                                .s(documentId.toString())
                                .build()
                ))
                .consistentRead(true)
                .build();
        GetItemResponse response = dynamoDbClient.getItem(request);
        if (!response.hasItem() || response.item().isEmpty()) {
            throw new IllegalArgumentException("Processing state not found for document: " + documentId);
        }

        Map<String, AttributeValue> item = response.item();
        String status = getString(item, "status");
        int progress = getInt(item, "progress", 0);
        String currentStep = getString(item, "currentStep");
        int attempts = getInt(item, "attempts", 0);

        String updatedAtValue = getString(item, "updatedAt");

        Instant updatedAt = updatedAtValue != null
                ? Instant.parse(updatedAtValue)
                : null;

        return new DocumentStatusResponse(
                documentId,
                status,
                progress,
                currentStep,
                attempts,
                updatedAt
        );
    }

    /**
     * Publishes a document retry request to Amazon SQS.
     */
    @Override
    public void requestRetry(UUID documentId) {
        Map<String, String> message = Map.of(
                "documentId", documentId.toString(),
                "action", "RETRY"
        );

        final String messageBody;

        try {
            messageBody = objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Failed to serialize retry message",
                    exception
            );
        }

        SendMessageRequest request = SendMessageRequest.builder()
                .queueUrl(retryQueueUrl)
                .messageBody(messageBody)
                .build();

        sqsClient.sendMessage(request);
    }

    @Override
    public void initializeStatus(UUID documentId) {
        Map<String, AttributeValue> item = Map.of(
                "documentId", AttributeValue.builder().s(documentId.toString()).build(),
                "status", AttributeValue.builder().s("PENDING").build(),
                "progress", AttributeValue.builder().n("0").build(),
                "currentStep", AttributeValue.builder().s("WAITING_FOR_UPLOAD").build(),
                "attempts", AttributeValue.builder().n("0").build(),
                "updatedAt", AttributeValue.builder().s(Instant.now().toString()).build()
        );
        PutItemRequest request = PutItemRequest.builder()
                        .tableName(processingTable)
                        .item(item)
                        .build();
        logger.info("initializeStatus, dynamoDbClient putItem: request: {}", request);
        dynamoDbClient.putItem(request);
    }

    private String getString(
            Map<String, AttributeValue> item,
            String attribute
    ) {
        AttributeValue value = item.get(attribute);

        return value != null && value.s() != null
                ? value.s()
                : null;
    }

    private int getInt(
            Map<String, AttributeValue> item,
            String attribute,
            int defaultValue
    ) {
        AttributeValue value = item.get(attribute);

        return value != null && value.n() != null
                ? Integer.parseInt(value.n())
                : defaultValue;
    }
}
