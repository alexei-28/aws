package com.gmail.alexei28.aws.dbs.dto.response;

import java.util.Map;
import java.util.UUID;

public record DocumentAnalysisResponse(
        UUID documentId,
        String type,
        Map<String, Object> analysis,
        Map<String, Object> metadata
) {
}