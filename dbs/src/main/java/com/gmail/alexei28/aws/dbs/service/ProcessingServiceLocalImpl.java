package com.gmail.alexei28.aws.dbs.service;

import com.gmail.alexei28.aws.dbs.dto.response.DocumentStatusResponse;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProcessingServiceImpl implements ProcessingService {

    @Override
    public DocumentStatusResponse getStatus(UUID documentId) {
        throw new UnsupportedOperationException(
                "DynamoDB integration is not implemented yet"
        );
    }

    @Override
    public void requestRetry(UUID documentId) {
        throw new UnsupportedOperationException(
                "SQS integration is not implemented yet"
        );
    }
}
