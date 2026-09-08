package com.nhnacademy.userauthapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.userauthapi.config.WebConfig;
import com.nhnacademy.userauthapi.config.properties.JwtProperties;
import com.nhnacademy.userauthapi.dto.login.LoginRequest;
import com.nhnacademy.userauthapi.dto.token.TokenResponse;
import com.nhnacademy.userauthapi.service.AuthService;
import jakarta.servlet.http.Cookie;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class) // AuthController 계층만 격리시켜 슬라이스 테스트 진행
@Import(WebConfig.class) // WebConfig의 /api/auth 경로 접두어 설정 적용
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc; // 가짜 HTTP 요청을 전송하고 응답을 검증하는 도구

    @Autowired
    private ObjectMapper objectMapper; // 자바 객체 <-> JSON 변환 유틸리티

    @MockitoBean
    private AuthService authService; // 가짜 비즈니스 로직 서비스 빈 주입

    @MockitoBean
    private JwtProperties jwtProperties; // 가짜 JWT 프로퍼티 주입

    // ==================== 1. 로그인 POST /api/auth/login ====================

    @Test
    @DisplayName("1. 로그인 성공 - 200 OK, AccessToken은 본문에, RefreshToken은 HttpOnly 쿠키로 전달됨을 검증")
    void login_Success() throws Exception {
        // given [준비]: 요청 DTO 및 서비스가 반환할 가짜 토큰 응답 세팅
        LoginRequest request = new LoginRequest("user1", "pw1234");
        TokenResponse tokenResponse = new TokenResponse("mockAccessToken", "mockRefreshToken", true);

        given(authService.login(any(LoginRequest.class))).willReturn(tokenResponse);
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(1209600000L); // 14일

        // when & then [실행 및 검증]
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // 1) HTTP 200 OK 응답 확인
                .andExpect(jsonPath("$.accessToken").value("mockAccessToken")) // 2) JSON 응답 본문에 AccessToken 포함 확인
                .andExpect(jsonPath("$.firstLogin").value(true)) // 3) 최초 로그인 여부 플래그 확인
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refreshToken=mockRefreshToken"))) // 4) Set-Cookie 헤더에 RefreshToken 전달 확인
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("HttpOnly"))); // 5) 보안을 위한 HttpOnly 설정 포함 확인
    }

    @Test
    @DisplayName("2. 로그인 실패 - 필수 입력값(loginId) 누락 시 500 Server Error 응답")
    void login_ValidationError_Returns500() throws Exception {
        // given: loginId가 빈값("")인 유효하지 않은 요청 데이터
        LoginRequest request = new LoginRequest("", "pw1234");

        // when & then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError()); // 500 확인
    }

    // ==================== 2. 로그아웃 POST /api/auth/logout ====================

    @Test
    @DisplayName("3. 로그아웃 성공 - Authorization Bearer 헤더 전달 시 200 OK 및 쿠키 삭제(Max-Age=0) 헤더 반환")
    void logout_Success() throws Exception {
        // given [준비]: Authorization 헤더로 전달될 AccessToken 문자열
        String accessToken = "mockAccessToken";

        // when & then [실행 및 검증]
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)) // Authorization: Bearer mockAccessToken 헤더 첨부
                .andExpect(status().isOk()) // 1) HTTP 200 OK 응답 확인
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0"))); // 2) 브라우저 쿠키 삭제를 위해 Max-Age=0 헤더 전달 확인

        verify(authService).logout(accessToken); // 3) authService.logout("mockAccessToken")이 실제로 호출되었는지 검증
    }

    @Test
    @DisplayName("4. 로그아웃 실패 - Authorization 헤더가 없거나 'Bearer '로 시작하지 않으면 400 Bad Request 응답")
    void logout_InvalidHeader_Returns400() throws Exception {
        // when & then [Bearer 접두어가 빠진 엉터리 헤더로 요청 전송]
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "InvalidHeaderToken"))
                .andExpect(status().isBadRequest()); // HTTP 400 BAD_REQUEST 응답 확인
    }

    // ==================== 3. 토큰 재발급 POST /api/auth/refresh ====================

    @Test
    @DisplayName("5. 토큰 재발급 성공 - HttpOnly 쿠키의 RefreshToken 전달 시 200 OK 및 새 AccessToken 반환")
    void refresh_Success() throws Exception {
        // given [준비]: 브라우저 쿠키에 담겨온 oldRefreshToken & 서비스가 반환할 새로운 토큰 응답
        String oldRefreshToken = "mockRefreshToken";
        TokenResponse tokenResponse = new TokenResponse("newAccessToken", oldRefreshToken, false);

        given(authService.refresh(oldRefreshToken)).willReturn(tokenResponse);

        // when & then [실행 및 검증]
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", oldRefreshToken))) // HTTP 쿠키 첨부
                .andExpect(status().isOk()) // 1) HTTP 200 OK 응답 확인
                .andExpect(jsonPath("$.accessToken").value("newAccessToken")); // 2) 새롭게 재발급된 AccessToken 반환 확인
    }

    @Test
    @DisplayName("6. 토큰 재발급 실패 - refreshToken 쿠키가 존재하지 않을 경우 401 Unauthorized 응답")
    void refresh_NoCookie_Returns401() throws Exception {
        // when & then [refreshToken 쿠키를 아예 안 넣고 요청 전송]
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized()); // HTTP 401 UNAUTHORIZED 응답 확인
    }
}