package com.gmail.alexei28.aws.task01.dto;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String fileName,
        String contentType,
        long size,
        Instant uploadedAt
) {
}
