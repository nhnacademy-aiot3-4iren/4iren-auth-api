package com.nhnacademy.userauthapi.dto.login;

// 로그인 응답 DTO
public record LoginResponse (
        Long userId,
        String userLoginId,
        String userName,
        String userRole
) {}
