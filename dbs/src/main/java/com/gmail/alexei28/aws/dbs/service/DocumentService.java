package com.gmail.alexei28.aws.dbs.service;

import com.gmail.alexei28.aws.dbs.dto.request.CreateDocumentRequest;
import com.gmail.alexei28.aws.dbs.dto.response.DocumentAnalysisResponse;
import com.gmail.alexei28.aws.dbs.dto.response.DocumentCreatedResponse;
import com.gmail.alexei28.aws.dbs.dto.response.DocumentResponse;
import com.gmail.alexei28.aws.dbs.dto.response.DocumentStatusResponse;
import com.gmail.alexei28.aws.dbs.entity.DocumentEntity;
import com.gmail.alexei28.aws.dbs.repo.AnalysisRepository;
import com.gmail.alexei28.aws.dbs.repo.DocumentRepository;
import com.gmail.alexei28.aws.dbs.storage.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

// Это основной бизнес-сервис. Он координирует работу трёх баз данных и S3.
@Service
public class DocumentService {
    private static final Duration UPLOAD_URL_EXPIRATION = Duration.ofMinutes(15);
    private final DocumentRepository documentRepository;
    private final ProcessingService processingService;
    private final AnalysisRepository analysisRepository;
    private final S3StorageService s3StorageService;
    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    public DocumentService(
            DocumentRepository documentRepository,
            ProcessingService processingService,
            AnalysisRepository analysisRepository,
            S3StorageService s3StorageService
    ) {
        this.documentRepository = documentRepository;
        this.processingService = processingService;
        this.analysisRepository = analysisRepository;
        this.s3StorageService = s3StorageService;
    }

    // POST /api/v1/documents
    @Transactional
    public DocumentCreatedResponse create(UUID userId, CreateDocumentRequest request) {
        UUID documentId = UUID.randomUUID();
        String s3Key = "documents/%s/%s".formatted(userId, documentId);
        Instant createdAt = Instant.now();
        DocumentEntity document = new DocumentEntity(
                documentId,
                userId,
                request.filename(),
                request.contentType(),
                s3Key,
                createdAt
        );
        documentRepository.save(document);
        logger.info("create, saved, document: {}", document);
        processingService.initializeStatus(documentId);
        String uploadUrl = s3StorageService.generateUploadUrl(s3Key, request.contentType(), UPLOAD_URL_EXPIRATION);
        logger.info("create, created uploadUrl");
        return new DocumentCreatedResponse(
                documentId,
                request.filename(),
                uploadUrl,
                createdAt
        );
    }

    // GET /api/v1/documents
    @Transactional(readOnly = true)
    public Page<DocumentResponse> findAll(UUID userId, Pageable pageable) {
        return documentRepository.findAllByUserId(userId, pageable).map(this::toResponse);
    }

    // GET /api/v1/documents/{id}
    @Transactional(readOnly = true)
    public DocumentResponse findById(
            UUID userId,
            UUID documentId
    ) {
        DocumentEntity document = getDocument(userId, documentId);
        return toResponse(document);
    }

    // GET /api/v1/documents/{id}/status
    @Transactional(readOnly = true)
    public DocumentStatusResponse getStatus(
            UUID userId,
            UUID documentId
    ) {
        getDocument(userId, documentId);
        return processingService.getStatus(documentId);
    }

    // GET /api/v1/documents/{id}/analysis
    @Transactional(readOnly = true)
    public DocumentAnalysisResponse getAnalysis(
            UUID userId,
            UUID documentId
    ) {
        getDocument(userId, documentId);
        return analysisRepository
                .findByDocumentId(documentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document analysis not found"
                ));
    }

    // POST /api/v1/documents/{id}/retry
    public void retry(UUID userId, UUID documentId) {
        getDocument(userId, documentId);
        processingService.requestRetry(documentId);
    }

    private DocumentEntity getDocument(UUID userId, UUID documentId) {
        return documentRepository.findByIdAndUserId(documentId, userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Document not found"
                ));
    }

    private DocumentResponse toResponse(
            DocumentEntity document
    ) {
        return new DocumentResponse(
                document.getId(),
                document.getFilename(),
                document.getContentType(),
                document.getS3Key(),
                document.getCreatedAt()
        );
    }
}