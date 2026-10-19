package com.gmail.alexei28.aws.task01.controller;

import com.gmail.alexei28.aws.task01.dto.DocumentResponse;
import com.gmail.alexei28.aws.task01.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.UUID;

/*
    Примеры запросов:

    Загрузить документ
    curl -X POST http://localhost:8081/api/v1/documents  -F "file=@/home/alexei/dev/temp/temp.txt"

    Пример ответа 201 Created:
    {
      "id": "8bbcb92b-a966-4718-834a-3023fd5a4179",
      "fileName": "example.pdf",
      "contentType": "application/pdf",
      "size": 24576,
      "uploadedAt": "2026-10-19T11:30:00Z"
    }

    Получить метаданные
    curl http://localhost:8081/api/v1/documents/8bbcb92b-a966-4718-834a-3023fd5a4179

    Скачать документ
    curl -OJ http://localhost:8081/api/v1/documents/8bbcb92b-a966-4718-834a-3023fd5a4179/download


    Удалить документ
    curl -X DELETE http://localhost:8081/api/v1/documents/8bbcb92b-a966-4718-834a-3023fd5a4179

    Успешное удаление возвращает 204 No Content.
*/
@RestController
@RequestMapping("/documents")
public class DocumentController {
    private final DocumentService documentService;
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(DocumentController.class);

    public DocumentController(DocumentService documentService, ObjectMapper objectMapper) {
        this.documentService = documentService;
        this.objectMapper = objectMapper;
    }

    // Возвращает 201 Created потому что запрос создаёт новый ресурс.
    // Ответ содержит идентификатор документа и URI, по которому можно получить его метаданные.
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentResponse> upload(@RequestPart("file") MultipartFile file) {
        DocumentResponse documentResponse = documentService.upload(file);
        ResponseEntity<DocumentResponse>  responseEntity = ResponseEntity
                .created(java.net.URI.create("/api/v1/documents/" + documentResponse.id()))
                .body(documentResponse);
        logger.info("Successfully uploaded file. Response:\n{}",
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(documentResponse));
        return responseEntity;
    }

    @GetMapping("/{id}")
    public DocumentResponse get(@PathVariable UUID id) {
        DocumentResponse response = documentService.get(id);
        logger.info("Successfully get document. Response:\n{}",
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(response));
        return response;
    }

    /*
        Скачивание использует StreamingResponseBody, чтобы не загружать целый документ в массив байтов и не держать весь файл в heap.
        Однако production-конфигурация также должна ограничивать количество параллельных скачиваний и настраивать executor
        для асинхронных MVC-запросов.
        В примере имя скачиваемого файла задано как document. В завершённом варианте его следует
        получать из проверенных метаданных, безопасно кодировать в Content-Disposition и передавать корректный Content-Type
    */
    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable UUID id) {
        ResponseInputStream<GetObjectResponse> s3Stream = documentService.download(id);
        GetObjectResponse metadata = s3Stream.response();
        StreamingResponseBody body = outputStream -> {
            try (s3Stream) {
                s3Stream.transferTo(outputStream);
            } catch (IOException exception) {
                throw new IllegalStateException("Failed to stream document", exception);
            }
        };
        String fileName = "document";
        logger.info("Successfully download file: '{}' by id: {}", fileName, id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(metadata.contentLength())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(fileName)
                                .build()
                                .toString()
                )
                .body(body);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        documentService.delete(id);
        logger.info("Successfully document by id: {}", id);
        return ResponseEntity.noContent().build();
    }
}
