package com.gmail.alexei28.aws.dbs.repo;

import com.gmail.alexei28.aws.dbs.dto.response.DocumentAnalysisResponse;

import java.util.Optional;
import java.util.UUID;

// Реализация будет использовать DocumentDB.
public interface AnalysisRepository {

    Optional<DocumentAnalysisResponse> findByDocumentId(UUID documentId);
}