package com.nhnacademy.userauthapi.exception;

import com.nhnacademy.userauthapi.dto.error.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.result.StatusResultMatchersExtensionsKt.isEqualTo;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler exceptionHandler=new GlobalExceptionHandler();

    @Test
    @DisplayName("1. LoginFailException 발생 시 404 Not Found 응답 반환")
    void handleLoginFailException() {
        LoginFailException ex= new LoginFailException("아이디 또는 비밀번호가 올바르지 않습니다");
        ResponseEntity<ErrorResponse> response=exceptionHandler.handleLoginFailException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().message()).isEqualTo("아이디 또는 비밀번호가 올바르지 않습니다");
    }

    @Test
    @DisplayName("2.RefreshTokenValidateException 발생 시 401 Unauthorized 응답 반환")
    void handleRefreshTokenValidateException() {
        RefreshTokenValidateException ex=new RefreshTokenValidateException("유효하지 않은 리프레시 토큰입니다.");
        ResponseEntity<ErrorResponse> response= exceptionHandler.handleRefreshTokenValidateException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().message()).isEqualTo("유효하지 않은 리프레시 토큰입니다.");
    }

    @Test
    @DisplayName("3.기타 예기치 못한 Exception 발생 시 500 Internal Server Error 응답 반환")
    void handleException() {
        Exception ex=new Exception("알 수 없는 오류");
        ResponseEntity<ErrorResponse> response=exceptionHandler.handleException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).isEqualTo("서버 오류가 발생했습니다.");
    }
}