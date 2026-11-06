package com.gmail.alexei28.aws.dbs.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class DocumentEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String filename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "s3_key", nullable = false, unique = true)
    private String s3Key;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DocumentEntity() {
    }

    public DocumentEntity(
            UUID id,
            UUID userId,
            String filename,
            String contentType,
            String s3Key,
            Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.filename = filename;
        this.contentType = contentType;
        this.s3Key = s3Key;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public String getS3Key() {
        return s3Key;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "\nDocumentEntity{" +
                "\nid = " + id +
                ",\nuserId = " + userId +
                ",\nfilename = '" + filename + '\'' +
                ",\ncontentType = '" + contentType + '\'' +
                ",\ns3Key = '" + s3Key + '\'' +
                ",\ncreatedAt = " + createdAt +
                '}';
    }
}