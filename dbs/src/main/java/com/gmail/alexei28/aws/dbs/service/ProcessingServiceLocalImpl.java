package com.gmail.alexei28.aws.dbs.service;

import com.gmail.alexei28.aws.dbs.dto.response.DocumentStatusResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@Profile("local") // только для локального окружения
public class ProcessingServiceLocalImpl implements ProcessingService {

    @Override
    public DocumentStatusResponse getStatus(UUID documentId) {
        throw new UnsupportedOperationException(
                "DynamoDB integration is not implemented yet"
        );
    }

    @Override
    public void requestRetry(UUID documentId) {
    }

    @Override
    public void initializeStatus(UUID documentId) {

    }
}
