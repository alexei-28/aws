package com.gmail.alexei28.aws.lambda.domain;

import java.time.Instant;
import java.util.UUID;

// Сообщение, которое будет передаваться через AWS SQS.
public record DocumentJob(
        UUID id,
        String bucket,
        String objectKey,
        Instant submittedAt
) {
    @Override
    public String toString() {
        return "\nDocumentJob {" +
                "\nid = " + id +
                ",\nbucket = '" + bucket + '\'' +
                ",\nobjectKey = '" + objectKey + '\'' +
                ",\nsubmittedAt = " + submittedAt + '}';
    }
}