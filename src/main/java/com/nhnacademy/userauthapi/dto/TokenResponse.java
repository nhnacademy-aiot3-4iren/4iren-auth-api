package com.nhnacademy.userauthapi.dto;

public record TokenResponse (
    String accessToken,
    String refreshToken
){}

