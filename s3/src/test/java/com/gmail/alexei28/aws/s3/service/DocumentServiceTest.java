package com.gmail.alexei28.aws.s3.service;

import com.gmail.alexei28.aws.s3.config.DocumentProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class DocumentServiceTest {

    private S3DocumentStorage storage;
    private DocumentService service;

    @BeforeEach
    void setUp() {
        storage = mock(S3DocumentStorage.class);

        DocumentProperties properties = new DocumentProperties(
                "test-bucket",
                "eu-central-1",
                10 * 1024 * 1024,
                List.of(
                        "application/pdf",
                        "image/png"
                )
        );

        service = new DocumentService(storage, properties);
    }

    @Test
    void shouldUploadValidPdf() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "document.pdf",
                "application/pdf",
                "test content".getBytes()
        );

        var response = service.upload(file);

        assertThat(response.id()).isNotNull();
        assertThat(response.fileName()).isEqualTo("document.pdf");
        assertThat(response.contentType())
                .isEqualTo("application/pdf");
        assertThat(response.size()).isEqualTo(12);

        verify(storage).upload(
                eq(response.id()),
                eq("document.pdf"),
                eq("application/pdf"),
                eq(12L),
                any()
        );
    }

    @Test
    void shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.pdf",
                "application/pdf",
                new byte[0]
        );

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");

        verifyNoInteractions(storage);
    }

    @Test
    void shouldRejectUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "script.exe",
                "application/octet-stream",
                new byte[]{1, 2, 3}
        );

        assertThatThrownBy(() -> service.upload(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported");

        verifyNoInteractions(storage);
    }
}
