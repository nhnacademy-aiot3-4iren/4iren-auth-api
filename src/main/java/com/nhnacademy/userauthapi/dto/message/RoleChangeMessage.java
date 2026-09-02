package com.nhnacademy.userauthapi.dto.message;

import java.time.LocalDateTime;

public record RoleChangeMessage(
    Long userId,
    String role,
    String jti,
    LocalDateTime updateAt
) {}
