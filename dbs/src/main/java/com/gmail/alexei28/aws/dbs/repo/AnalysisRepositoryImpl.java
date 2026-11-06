package com.gmail.alexei28.aws.dbs.repo;

import com.gmail.alexei28.aws.dbs.dto.response.DocumentAnalysisResponse;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class AnalysisRepositoryImpl implements AnalysisRepository {

    @Override
    public Optional<DocumentAnalysisResponse> findByDocumentId(UUID documentId) {
        return Optional.empty();
    }
}
