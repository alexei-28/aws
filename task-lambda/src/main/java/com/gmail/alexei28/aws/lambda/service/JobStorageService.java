package com.gmail.alexei28.aws.lambda.service;

import com.gmail.alexei28.aws.lambda.domain.DocumentJob;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobStorageService {
    // Потокобезопасное хранилище в памяти (для продакшна лучше заменить на БД / DynamoDB)
    private final Map<UUID, DocumentJob> storage = new ConcurrentHashMap<>();

    public void save(DocumentJob job) {
        storage.put(job.id(), job);
    }

    public DocumentJob findById(UUID jobId) {
        return storage.get(jobId);
    }
}