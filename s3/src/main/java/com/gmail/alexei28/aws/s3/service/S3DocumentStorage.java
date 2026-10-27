package com.gmail.alexei28.aws.s3.service;

import com.gmail.alexei28.aws.s3.config.DocumentProperties;
import com.gmail.alexei28.aws.s3.exception.DocumentNotFoundException;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

// Инкапсулирует операции с S3. Использует UUID документа как часть ключа и хранит метаданные в самом объекте
@Component
public class S3DocumentStorage {
    private final S3Client s3Client;
    private final DocumentProperties properties;

    public S3DocumentStorage(S3Client s3Client, DocumentProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public String createKey(UUID id) {
        return "documents/" + id;
    }

    public void upload(
            UUID id,
            String originalFileName,
            String contentType,
            long size,
            InputStream inputStream
    ) {
        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(properties.bucket())
                .key(createKey(id))
                .contentType(contentType)
                .contentLength(size)
                .metadata(Map.of(
                        "original-filename", originalFileName,
                        "document-id", id.toString(),
                        "uploaded-at", Instant.now().toString()
                ))
                .build();

        s3Client.putObject(request, RequestBody.fromInputStream(inputStream, size));
    }

    public HeadObjectResponse getMetadata(UUID id) {
        try {
            HeadObjectResponse response = s3Client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(createKey(id))
                            .build()
            );

            if (response.metadata() == null
                    || !id.toString().equals(
                    response.metadata().get("document-id"))) {
                throw new DocumentNotFoundException();
            }

            return response;

        } catch (NoSuchKeyException exception) {
            throw new DocumentNotFoundException();
        }
    }

    public ResponseInputStream<GetObjectResponse> download(UUID id) {
        getMetadata(id);
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(createKey(id))
                        .build()
        );
    }

    public void delete(UUID id) {
        getMetadata(id);

        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(properties.bucket())
                        .key(createKey(id))
                        .build()
        );
    }

    public List<String> listObjectKeys() {
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(properties.bucket())
                .prefix("documents/")
                .build();

        ListObjectsV2Response response = s3Client.listObjectsV2(request);
        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }
}
