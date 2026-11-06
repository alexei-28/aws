package com.gmail.alexei28.aws.dbs.service;

import com.gmail.alexei28.aws.dbs.dto.response.DocumentStatusResponse;

import java.util.UUID;

// Чтобы DocumentService не зависел от конкретной реализации AWS SDK, используем отдельные интерфейсы.
// Реализация будет использовать DynamoDB и SQS.
public interface ProcessingService {

    DocumentStatusResponse getStatus(UUID documentId);

    void requestRetry(UUID documentId);

    void initializeStatus(UUID documentId);
}