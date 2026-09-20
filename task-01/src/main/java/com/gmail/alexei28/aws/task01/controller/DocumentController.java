package com.gmail.alexei28.aws.task01.controller;

import com.gmail.alexei28.aws.task01.dto.DocumentUploadResponse;
import com.gmail.alexei28.aws.task01.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService documentService;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        logger.info("uploadDocument, file {} ...", file.getOriginalFilename());
        DocumentUploadResponse response = documentService.uploadDocument(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}