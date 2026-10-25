package com.gmail.alexei28.aws.s3.exception;

import com.gmail.alexei28.aws.s3.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiError> notFound(HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "Document not found", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> badRequest(IllegalArgumentException exception, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> fileTooLarge(HttpServletRequest request) {
        return error(HttpStatus.PAYLOAD_TOO_LARGE, "Uploaded file exceeds the allowed size", request);
    }

    @ExceptionHandler(S3Exception.class)
    public ResponseEntity<ApiError> s3Error(S3Exception exception, HttpServletRequest request) {
        if (exception.statusCode() == HttpStatus.NOT_FOUND.value()) {
            return error(HttpStatus.NOT_FOUND, "Document not found", request);
        }
        log.error("S3 request failed with status {}", exception.statusCode());
        return error(HttpStatus.BAD_GATEWAY, "Document storage operation failed", request);
    }

    @ExceptionHandler(SdkClientException.class)
    public ResponseEntity<ApiError> awsClientError(SdkClientException exception, HttpServletRequest request) {
        log.error("AWS SDK client error", exception);
        return error(HttpStatus.SERVICE_UNAVAILABLE, "Document storage is temporarily unavailable", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception exception, HttpServletRequest request) {
        log.error("Unexpected application error", exception);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(
                new ApiError(
                        Instant.now(),
                        status.value(),
                        status.getReasonPhrase(),
                        message,
                        request.getRequestURI()
                )
        );
    }
}
