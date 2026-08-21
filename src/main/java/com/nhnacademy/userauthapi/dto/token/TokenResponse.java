package com.nhnacademy.userauthapi.dto.token;

public record TokenResponse (
    String accessToken,
    String refreshToken,
    Boolean firstLogin
){}

