package com.nhnacademy.userauthapi.dto.login;

// 로그인 응답 DTO
public record LoginResponse (
        String userId,
        String userLoginId,
        String userName,
        String userRole
) {}
