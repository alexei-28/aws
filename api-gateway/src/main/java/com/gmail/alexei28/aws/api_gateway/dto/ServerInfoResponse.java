package com.gmail.alexei28.aws.api_gateway.dto;

import java.time.LocalDateTime;

public record ServerInfoResponse(
        LocalDateTime date,
        String hostname,
        String javaVersion,
        String os,
        String environment
) {
}