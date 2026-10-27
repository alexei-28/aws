package com.gmail.alexei28.aws.lambda.controller;

import com.gmail.alexei28.aws.lambda.api.DocumentRequest;
import com.gmail.alexei28.aws.lambda.config.DocumentProperties;
import com.gmail.alexei28.aws.lambda.domain.DocumentJob;
import com.gmail.alexei28.aws.lambda.infrastructure.SqsJobPublisher;
import com.gmail.alexei28.aws.lambda.service.JobStorageService; // Сервис для хранения/поиска задач
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
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
        logger.info("submit, \nSuccessfully saved in AWS DynamoDb the next job: {}", job);
        // Если отправка в SQS завершится ошибкой, запись в DynamoDB уже будет существовать.
        // Это отдельный вопрос надёжности, который позже можно решить через Transactional Outbox.
        sqsJobPublisher.publish(job);
        logger.info("submit, \nSuccessfully sent to AWS SQS the next job: {}", job);
        return new SubmissionResponse(job.getId(), "ACCEPTED");
    }

    // Метод для запроса состояния по jobId
    @GetMapping("/jobs/{jobId}")
    public DocumentJob getJobStatus(@PathVariable UUID jobId) {
        DocumentJob job = jobStorageService.findById(jobId);
        if (job == null) {
            logger.warn("getJobStatus, Not found job by id: {}", jobId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Job not found");
        }
        logger.info("getJobStatus, \nSuccessfully find job: {}", job);
        return job;
    }

    public record SubmissionResponse(UUID jobId, String status) {
    }

    @GetMapping
    public List<DocumentJob> findAllJobs() {
        List<DocumentJob> list = jobStorageService.findAllJobs();
        logger.info("findAllJobs, list size: {}", list.size());
        return list;
    }

    @GetMapping("ids")
    public List<String> findAllJobIds() {
        List<String> list =  jobStorageService.findAllJobIds();
        logger.info("findAllJobIds, list size: {}", list.size());
        return list;
    }
}