package com.gmail.alexei28.aws.task01.service;


import com.gmail.alexei28.aws.task01.config.DocumentProperties;
import com.gmail.alexei28.aws.task01.dto.DocumentResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/*
    Сервис проверяет входные данные, создаёт идентификатор, вызывает S3 storage и формирует DTO.
    В этом примере документ определяется UUID, а его метаданные читаются из S3.
*/
@Service
public class DocumentService {
    private final S3DocumentStorage s3DocumentStorage;
    private final DocumentProperties properties;

    /*
      Инжектирует DocumentProperties как Spring bean, потому что S3ClientConfig -> @EnableConfigurationProperties(DocumentProperties.class)
    */
    public DocumentService(S3DocumentStorage s3DocumentStorage, DocumentProperties properties) {
        this.s3DocumentStorage = s3DocumentStorage;
        this.properties = properties;
    }

    public DocumentResponse upload(MultipartFile file) {
        validate(file);

        String fileName = sanitizeFileName(file.getOriginalFilename());
        String contentType = file.getContentType().toLowerCase(Locale.ROOT);
        UUID id = UUID.randomUUID();

        try (InputStream inputStream = file.getInputStream()) {
            s3DocumentStorage.upload(id, fileName, contentType, file.getSize(), inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read uploaded file", exception);
        }
        return new DocumentResponse(
                id,
                fileName,
                contentType,
                file.getSize(),
                Instant.now()
        );
    }

    public DocumentResponse get(UUID id) {
        HeadObjectResponse metadata = s3DocumentStorage.getMetadata(id);
        Map<String, String> userMetadata = metadata.metadata();
        return new DocumentResponse(
                id,
                userMetadata.get("original-filename"),
                metadata.contentType(),
                metadata.contentLength(),
                Instant.parse(userMetadata.get("uploaded-at"))
        );
    }

    public ResponseInputStream<GetObjectResponse> download(UUID id) {
        return s3DocumentStorage.download(id);
    }

    public void delete(UUID id) {
        s3DocumentStorage.delete(id);
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uploaded file must not be empty"
            );
        }

        if (file.getSize() > properties.maxFileSize()) {
            throw new IllegalArgumentException(
                    "File exceeds the maximum allowed size"
            );
        }

        String contentType = file.getContentType();

        if (contentType == null || !properties.allowedContentTypes().contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Unsupported file type");
        }

        sanitizeFileName(file.getOriginalFilename());
    }

    private String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            throw new IllegalArgumentException("File name must not be blank");
        }

        // Normalize both Unix and Windows path separators.
        String normalized = originalName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1);

        if (fileName.isBlank()
                || fileName.length() > 200
                || fileName.chars().anyMatch(
                c -> Character.isISOControl(c))) {
            throw new IllegalArgumentException("Invalid file name");
        }
        return fileName;
    }
}
