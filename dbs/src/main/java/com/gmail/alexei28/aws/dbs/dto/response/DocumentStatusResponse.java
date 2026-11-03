package com.gmail.alexei28.aws.dbs.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentStatusResponse(
        UUID documentId,
        String status,
        int progress,
        String currentStep,
        int attempts,
        Instant updatedAt
) {
}