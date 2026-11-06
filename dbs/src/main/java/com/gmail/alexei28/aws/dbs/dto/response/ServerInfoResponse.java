package com.gmail.alexei28.aws.dbs.dto.response;
import java.time.LocalDateTime;

public record ServerInfoResponse(
        LocalDateTime date,
        String hostname,
        String javaVersion,
        String os,
        String environment
) {
}
