package com.gmail.alexei28.aws.lambda.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentRequest(
        @NotBlank
        @Size(max = 500)
        String objectKey) {
}