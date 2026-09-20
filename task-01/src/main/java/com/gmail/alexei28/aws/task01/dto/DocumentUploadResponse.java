package com.gmail.alexei28.aws.task01.dto;

public record DocumentUploadResponse(
        String fileKey,
        String fileName,
        long fileSize,
        String contentType,
        String message
) {}