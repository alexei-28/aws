package com.gmail.alexei28.aws.lambda.domain;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

// DynamoDb Enhanced Client требует, чтобы поля класса были изменяемыми (мутабельными), поэтому для базы данных
// нельзя использовать Java record.
@DynamoDbBean
public class DocumentJob {
    private UUID id;
    private String bucket;
    private String objectKey;
    private Instant submittedAt;

    public DocumentJob() {
        // Требуется для DynamoDb Enhanced Client
    }

    public DocumentJob(UUID id, String bucket, String objectKey, Instant submittedAt) {
        this.id = id;
        this.bucket = bucket;
        this.objectKey = objectKey;
        this.submittedAt = submittedAt;
    }

    @DynamoDbPartitionKey
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant submittedAt) {
        this.submittedAt = submittedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DocumentJob that = (DocumentJob) o;
        return Objects.equals(id, that.id) &&
                Objects.equals(bucket, that.bucket) &&
                Objects.equals(objectKey, that.objectKey) &&
                Objects.equals(submittedAt, that.submittedAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, bucket, objectKey, submittedAt);
    }

    @Override
    public String toString() {
        return "\nDocumentJob {" +
                "\nid = " + id +
                ",\nbucket = '" + bucket + '\'' +
                ",\nobjectKey = '" + objectKey + '\'' +
                ",\nsubmittedAt = " + submittedAt + '}';
    }
}