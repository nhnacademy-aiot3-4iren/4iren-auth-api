package com.nhnacademy.userauthapi.exception;

public class RefreshTokenValidateException extends RuntimeException {
    public RefreshTokenValidateException(String message) {
        super(message);
    }
}
