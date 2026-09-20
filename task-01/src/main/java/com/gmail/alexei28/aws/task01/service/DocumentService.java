package com.gmail.alexei28.aws.task01.service;

import com.gmail.alexei28.aws.task01.config.S3Properties;
import com.gmail.alexei28.aws.task01.dto.DocumentUploadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.util.UUID;

@Service
public class DocumentService {
    private final S3Client s3Client;
    private final S3Properties s3Properties;

    private static final Logger logger = LoggerFactory.getLogger(DocumentService.class);

    public DocumentService(S3Client s3Client, S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
    }

    public DocumentUploadResponse uploadDocument(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Cannot upload an empty file.");
        }
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFileKey = UUID.randomUUID() + (fileExtension.isEmpty() ? "" : "." + fileExtension);
        logger.info("Starting upload for file: '{}' as key: '{}' to bucket: '{}'",
                originalFilename, uniqueFileKey, s3Properties.bucketName());
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.bucketName())
                    .key(uniqueFileKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();
            s3Client.putObject(
                    putObjectRequest,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
            logger.info("Successfully uploaded file '{}' to S3", uniqueFileKey);
            return new DocumentUploadResponse(
                    uniqueFileKey,
                    originalFilename,
                    file.getSize(),
                    file.getContentType(),
                    "Document successfully uploaded!"
            );
        } catch (IOException e) {
            logger.error("Failed to read input stream for file: {}", originalFilename, e);
            throw new RuntimeException("Failed to process uploaded file", e);
        } catch (S3Exception e) {
            logger.error("AWS S3 error occurred while uploading file: {}", originalFilename, e);
            throw new RuntimeException("Failed to store file in cloud storage: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIdx = filename.lastIndexOf('.');
        return (lastDotIdx == -1) ? "" : filename.substring(lastDotIdx + 1);
    }
}