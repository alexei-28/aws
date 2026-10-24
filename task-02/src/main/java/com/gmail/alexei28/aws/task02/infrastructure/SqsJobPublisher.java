
package com.gmail.alexei28.aws.task02.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.alexei28.aws.task02.Task2App;
import com.gmail.alexei28.aws.task02.config.DocumentProperties;
import com.gmail.alexei28.aws.task02.domain.DocumentJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.util.Objects;

@Component
public class SqsJobPublisher {
    private final SqsClient sqsClient;
    private final ObjectMapper objectMapper;
    private final String queueUrl;

    private static final Logger logger = LoggerFactory.getLogger(SqsJobPublisher.class);

    public SqsJobPublisher(
            SqsClient sqsClient,
            ObjectMapper objectMapper,
            DocumentProperties properties
    ) {
        this.sqsClient = Objects.requireNonNull(sqsClient);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.queueUrl = properties.queueUrl();
    }

    public void publish(DocumentJob job) {
        try {
            String body = objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(job);
            sqsClient.sendMessage(SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(body)
                    .build());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "Could not serialize document job", exception);
        }
    }
}
