package com.gmail.alexei28.aws.task02.service;

import com.gmail.alexei28.aws.task02.domain.DocumentJob;
import com.gmail.alexei28.aws.task02.processor.DocumentProcessor;
import com.gmail.alexei28.aws.task02.processor.DocumentStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class DocumentProcessingService implements DocumentProcessor {
    private final DocumentStorage storage;

    private static final Logger logger = LoggerFactory.getLogger(DocumentProcessingService.class);

    public DocumentProcessingService(DocumentStorage storage) {
        this.storage = Objects.requireNonNull(storage);
    }

    @Override
    public void process(DocumentJob job) {
        logger.info("process, job: {}", job);
        Objects.requireNonNull(job, "job must not be null");
        if (job.bucket() == null || job.bucket().isBlank()) {
            throw new IllegalArgumentException("Bucket must not be blank");
        }
        if (job.objectKey() == null || job.objectKey().isBlank()) {
            throw new IllegalArgumentException("Object key must not be blank");
        }
        byte[] content = storage.download(job.bucket(), job.objectKey());
        if (content == null || content.length == 0) {
            throw new IllegalStateException("Document is empty");
        }

        // Здесь размещается бизнес-логика обработки документа.
        // Не логируем содержимое документа или секретные данные.

        logger.info("process, successfully processed job(id) {} ", job.id());
    }
}
