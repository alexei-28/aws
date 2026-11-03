package com.gmail.alexei28.aws.dbs.dto.response;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        String contentType,
        String s3Key,
        Instant createdAt
) {
}