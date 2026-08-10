package com.nhnacademy.userauthapi.dto.error;

import java.time.LocalDateTime;
import java.time.ZoneId;

public record ErrorResponse(
        String message,
        int status,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(message, status, LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }
}
