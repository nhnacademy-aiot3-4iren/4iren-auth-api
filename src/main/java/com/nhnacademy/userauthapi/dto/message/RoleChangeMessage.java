package com.nhnacademy.userauthapi.dto.message;

public record RoleChangeMessage(
    Long userId,
    String role,
    String jti
) {}
