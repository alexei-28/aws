package com.gmail.alexei28.aws.task02.controller;

import com.gmail.alexei28.aws.task02.api.DocumentRequest;
import com.gmail.alexei28.aws.task02.config.DocumentProperties;
import com.gmail.alexei28.aws.task02.domain.DocumentJob;
import com.gmail.alexei28.aws.task02.infrastructure.SqsJobPublisher;
import com.gmail.alexei28.aws.task02.service.JobStorageService; // Сервис для хранения/поиска задач
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("documents")
public class DocumentController {
    private final SqsJobPublisher sqsJobPublisher;
    private final DocumentProperties properties;
    private final JobStorageService jobStorageService; // Внедряем хранилище

    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(
            SqsJobPublisher publisher,
            DocumentProperties properties,
            JobStorageService jobStorageService
    ) {
        this.sqsJobPublisher = publisher;
        this.properties = properties;
        this.jobStorageService = jobStorageService;
    }

    // Post job to AWS SQS.
    // AWS console -> Simple Queue Service -> document-job -> Send and receive messages -> Poll for messages
    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public SubmissionResponse submit(@Valid @RequestBody DocumentRequest request) {
        logger.info("submit, request: {}", request);
        String objectKey = request.objectKey();
        if (objectKey.startsWith("/") || objectKey.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object key");
        }
        DocumentJob job = new DocumentJob(UUID.randomUUID(), properties.bucket(), objectKey, Instant.now());
        logger.info("submit, \nFrom request successfully created job: {}", job);
        jobStorageService.save(job);
        sqsJobPublisher.publish(job);
        logger.info("submit, \nSuccessfully sent to AWS SQS the next job: {}", job);
        return new SubmissionResponse(job.id(), "ACCEPTED");
    }

    // Новый метод для запроса состояния по jobId
    @GetMapping("/{jobId}")
    public DocumentJob getJobStatus(@PathVariable UUID jobId) {
        DocumentJob job = jobStorageService.findById(jobId);
        if (job == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        return job;
    }

    public record SubmissionResponse(UUID jobId, String status) {
    }
}