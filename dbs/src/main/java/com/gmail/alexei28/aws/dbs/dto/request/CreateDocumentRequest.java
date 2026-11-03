package com.gmail.alexei28.aws.dbs.dto.request;

// Клиент передаёт имя файла и MIME type.
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDocumentRequest(
        @NotBlank
        @Size(max = 255)
        String filename,

        @NotBlank
        @Size(max = 100)
        String contentType

) {
        @Override
        public String toString() {
                return "\nCreateDocumentRequest{" +
                        "filename='" + filename + '\'' +
                        ", contentType='" + contentType + '\'' +
                        '}';
        }
}