package com.gmail.alexei28.aws.lambda.service;

import com.gmail.alexei28.aws.lambda.domain.DocumentJob;
import com.gmail.alexei28.aws.lambda.processor.DocumentProcessor;
import com.gmail.alexei28.aws.lambda.processor.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DocumentProcessingService implements DocumentProcessor {
    private final DocumentStorage documentStorage;

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    public DocumentProcessingService(DocumentStorage documentStorage) {
        this.documentStorage = Objects.requireNonNull(documentStorage);
    }

    @Override
    public void process(DocumentJob job) {
        logger.info("process, job: {}", job);
        Objects.requireNonNull(job, "job must not be null");
        if (job.getBucket() == null || job.getBucket().isBlank()) {
            throw new IllegalArgumentException("Bucket must not be blank");
        }
        if (job.getObjectKey() == null || job.getObjectKey().isBlank()) {
            throw new IllegalArgumentException("Object key must not be blank");
        }
        // Важно!
        // objectKey должен существовать в AWS S3
        byte[] content = documentStorage.download(job.getBucket(), job.getObjectKey());
        if (content == null || content.length == 0) {
            throw new IllegalStateException("Document is empty");
        }

        // Здесь размещается бизнес-логика обработки документа.
        // Не логируем содержимое документа или секретные данные.
        logger.info("process, successfully processed job(id) {} ", job.getId());
    }
}
