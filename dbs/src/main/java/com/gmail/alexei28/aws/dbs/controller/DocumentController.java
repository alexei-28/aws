package com.gmail.alexei28.aws.dbs.controller;

import com.gmail.alexei28.aws.dbs.dto.request.CreateDocumentRequest;
import com.gmail.alexei28.aws.dbs.dto.response.DocumentAnalysisResponse;
import com.gmail.alexei28.aws.dbs.dto.response.DocumentCreatedResponse;
import com.gmail.alexei28.aws.dbs.dto.response.DocumentResponse;
import com.gmail.alexei28.aws.dbs.dto.response.DocumentStatusResponse;
import com.gmail.alexei28.aws.dbs.service.DocumentService;
import jakarta.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService documentService;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // POST /api/v1/documents
    @PostMapping
    public ResponseEntity<DocumentCreatedResponse> create(@Valid @RequestBody CreateDocumentRequest request, Principal principal) {
        UUID userId = getUserId(principal);
        logger.info("create, userId: {}\nrequest: {}", userId, request);
        DocumentCreatedResponse response = documentService.create(userId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // GET /api/v1/documents
    @GetMapping
    public ResponseEntity<Page<DocumentResponse>> findAll(
            Principal principal,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        UUID userId = getUserId(principal);
        logger.info("findAll, userId: {}", userId);
        return ResponseEntity.ok(
                documentService.findAll(userId, pageable)
        );
    }

    // GET /api/v1/documents/{id}
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> findById(
            @PathVariable UUID id,
            Principal principal
    ) {
        UUID userId = getUserId(principal);
        logger.info("findById, userId: {}", userId);
        return ResponseEntity.ok(
                documentService.findById(userId, id)
        );
    }

    // GET /api/v1/documents/{id}/status
    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentStatusResponse> getStatus(
            @PathVariable UUID id,
            Principal principal
    ) {
        UUID userId = getUserId(principal);
        logger.info("getStatus, userId: {}", userId);
        return ResponseEntity.ok(
                documentService.getStatus(userId, id)
        );
    }

    // GET /api/v1/documents/{id}/analysis
    @GetMapping("/{id}/analysis")
    public ResponseEntity<DocumentAnalysisResponse> getAnalysis(
            @PathVariable UUID id,
            Principal principal
    ) {
        UUID userId = getUserId(principal);
        logger.info("getAnalysis, userId: {}", userId);
        return ResponseEntity.ok(
                documentService.getAnalysis(userId, id)
        );
    }

    // POST /api/v1/documents/{id}/retry
    @PostMapping("/{id}/retry")
    public ResponseEntity<Void> retry(
            @PathVariable UUID id,
            Principal principal
    ) {
        UUID userId = getUserId(principal);
        logger.info("getAnalysis, retry: {}", userId);
        documentService.retry(userId, id);
        return ResponseEntity.accepted().build();
    }

    // Principal.getName() здесь предполагает, что Spring Security устанавливает в имя аутентифицированного
    // пользователя его UUID (например, из JWT sub).
    /*
    private UUID getUserId(Principal principal) {
        return UUID.fromString(principal.getName());
    }
     */

    // Для локального тестирования можно временно использовать фиксированный UUID
    private UUID getUserId(Principal principal) {
        if (principal == null) {
            return UUID.fromString(
                    "550e8400-e29b-41d4-a716-446655440000"
            );
        }
        return UUID.fromString(principal.getName());
    }
}