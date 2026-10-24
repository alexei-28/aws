
package com.gmail.alexei28.aws.task02.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.amazonaws.services.lambda.runtime.events.SQSBatchResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gmail.alexei28.aws.task02.domain.DocumentJob;
import com.gmail.alexei28.aws.task02.processor.DocumentProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DocumentSqsHandlerTest {
    @Mock
    private DocumentProcessor documentProcessor;

    @Mock
    private Context context;

    private ObjectMapper objectMapper;
    private DocumentSqsHandler handler;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        handler = new DocumentSqsHandler(
                documentProcessor,
                objectMapper
        );
    }

    @Test
    void shouldProcessMessageSuccessfully() throws Exception {
        // Arrange
        DocumentJob job = createJob();
        SQSEvent event = createEvent(createMessage("message-1", objectMapper.writeValueAsString(job)));

        // Act
        SQSBatchResponse response = handler.handleRequest(event, context);

        // Assert
        assertThat(response.getBatchItemFailures()).isEmpty();
        verify(documentProcessor).process(job);
    }

    @Test
    void shouldReturnEmptyFailuresWhenEventIsNull() {
        // Arrange
        SQSEvent event = null;

        // Act
        SQSBatchResponse response = handler.handleRequest(event, context);

        // Assert
        assertThat(response.getBatchItemFailures()).isEmpty();
        verifyNoInteractions(documentProcessor);
    }

    @Test
    void shouldReturnEmptyFailuresWhenRecordsAreNull() {
        // Arrange
        SQSEvent event = new SQSEvent();
        event.setRecords(null);

        // Act
        SQSBatchResponse response = handler.handleRequest(event, context);

        // Assert
        assertThat(response.getBatchItemFailures()).isEmpty();
        verifyNoInteractions(documentProcessor);
    }

    @Test
    void shouldReturnEmptyFailuresWhenRecordsAreEmpty() {
        // Arrange
        SQSEvent event = createEvent();

        // Act
        SQSBatchResponse response = handler.handleRequest(event, context);

        // Assert
        assertThat(response.getBatchItemFailures()).isEmpty();
        verifyNoInteractions(documentProcessor);
    }

    @Test
    void shouldReturnMessageIdWhenDeserializationFails() {
        // Arrange
        SQSEvent event = createEvent(
                createMessage("invalid-message", "{invalid-json")
        );

        // Act
        SQSBatchResponse response = handler.handleRequest(event, context);

        // Assert
        assertThat(response.getBatchItemFailures())
                .extracting(SQSBatchResponse.BatchItemFailure::getItemIdentifier)
                .containsExactly("invalid-message");

        verifyNoInteractions(documentProcessor);
    }

    @Test
    void shouldReturnMessageIdWhenProcessingFails() throws Exception {
        // Arrange
        DocumentJob job = createJob();
        SQSEvent event = createEvent(
                createMessage("failed-message",
                        objectMapper.writeValueAsString(job))
        );

        doThrow(new IllegalStateException("Processing failed"))
                .when(documentProcessor)
                .process(job);

        // Act
        SQSBatchResponse response = handler.handleRequest(event, context);

        // Assert
        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure::getItemIdentifier
                )
                .containsExactly("failed-message");

        verify(documentProcessor).process(job);
    }

    @Test
    void shouldContinueProcessingAfterOneMessageFails() throws Exception {
        // Arrange
        DocumentJob failedJob = createJob();
        DocumentJob successfulJob = createJob();

        SQSEvent event = createEvent(
                createMessage("failed-message",
                        objectMapper.writeValueAsString(failedJob)),
                createMessage("successful-message",
                        objectMapper.writeValueAsString(successfulJob))
        );

        doThrow(new IllegalStateException("Processing failed"))
                .when(documentProcessor)
                .process(failedJob);

        // Act
        SQSBatchResponse response = handler.handleRequest(event, context);

        // Assert
        assertThat(response.getBatchItemFailures())
                .extracting(
                        SQSBatchResponse.BatchItemFailure::getItemIdentifier
                )
                .containsExactly("failed-message");

        verify(documentProcessor).process(failedJob);
        verify(documentProcessor).process(successfulJob);
    }

    @Test
    void shouldProcessAllMessagesWhenSuccessful() throws Exception {
        // Arrange
        DocumentJob firstJob = createJob();
        DocumentJob secondJob = createJob();

        SQSEvent event = createEvent(
                createMessage("message-1",
                        objectMapper.writeValueAsString(firstJob)),
                createMessage("message-2",
                        objectMapper.writeValueAsString(secondJob))
        );

        // Act
        SQSBatchResponse response = handler.handleRequest(event, context);

        // Assert
        assertThat(response.getBatchItemFailures()).isEmpty();
        verify(documentProcessor).process(firstJob);
        verify(documentProcessor).process(secondJob);
    }

    private DocumentJob createJob() {
        return new DocumentJob(
                UUID.randomUUID(),
                "my_aws_s3_bucket_id",
                "my_object_key_in_aws_s3_bucket",
                Instant.parse("2026-10-24T10:00:00Z")
        );
    }

    private SQSEvent createEvent(SQSEvent.SQSMessage... messages) {
        SQSEvent event = new SQSEvent();
        event.setRecords(List.of(messages));
        return event;
    }

    private SQSEvent.SQSMessage createMessage(String messageId, String body) {
        SQSEvent.SQSMessage message = new SQSEvent.SQSMessage();
        message.setMessageId(messageId);
        message.setBody(body);
        return message;
    }
}
