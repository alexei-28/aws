package com.gmail.alexei28.aws.task02.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DocumentRequest(
        @NotBlank
        @Size(max = 500)
        String objectKey) {
}