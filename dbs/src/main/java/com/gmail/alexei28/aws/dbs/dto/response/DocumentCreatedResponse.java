package com.gmail.alexei28.aws.dbs.dto.response;

// После регистрации возвращаем ID документа и pre-signed URL для загрузки в S3.
import java.time.Instant;
import java.util.UUID;

public record DocumentCreatedResponse(
        UUID id,
        String filename,
        String uploadUrl,
        Instant createdAt
) {
}