package com.nhnacademy.userauthapi.service.impl;

import com.nhnacademy.userauthapi.client.AccountClient;
import com.nhnacademy.userauthapi.config.JwtProvider;
import com.nhnacademy.userauthapi.config.properties.JwtProperties;
import com.nhnacademy.userauthapi.dto.login.LoginRequest;
import com.nhnacademy.userauthapi.dto.login.LoginResponse;
import com.nhnacademy.userauthapi.dto.token.TokenResponse;
import com.nhnacademy.userauthapi.dto.user.UserResponse;
import com.nhnacademy.userauthapi.exception.LoginFailException;
import com.nhnacademy.userauthapi.exception.RefreshTokenValidateException;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AccountClient accountClient;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== 1. 로그인 테스트 ====================

    @Test
    @DisplayName("1. 로그인 성공 - JWT 토큰 발급 및 Redis에 Refresh Token이 정상 저장된다")
    void login_Success() {
        // given
        Long userId = 123L;
        String userLoginId = "testUser";
        String password = "password123";
        String userName = "홍길동";
        String role = "USER";
        LoginRequest loginRequest = new LoginRequest(userLoginId, password);

        LoginResponse loginResponse = new LoginResponse(userId, userLoginId, userName, role, true);

        given(accountClient.login(any(LoginRequest.class))).willReturn(ResponseEntity.ok(loginResponse));
        given(jwtProvider.createAccessToken(eq(userId), eq(userLoginId), anyString())).willReturn("mockAccessToken");
        given(jwtProvider.createRefreshToken(eq(userId))).willReturn("mockRefreshToken");
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(1209600000L);
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");

        // when
        TokenResponse tokenResponse = authService.login(loginRequest);

        // then
        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.accessToken()).isEqualTo("mockAccessToken");
        assertThat(tokenResponse.refreshToken()).isEqualTo("mockRefreshToken");

        verify(valueOperations).set(
                eq("refreshToken:123"),
                eq("mockRefreshToken"),
                eq(1209600000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("2. 로그인 실패 - AccountClient 통신 에러(401/404) 발생 시 LoginFailException 예외가 발생한다")
    void login_Fail_FeignException() {
        // given
        LoginRequest loginRequest = new LoginRequest("wrongUser", "wrongPw");
        Request request = Request.create(Request.HttpMethod.POST, "/login", Map.of(), null, null, null);
        given(accountClient.login(any(LoginRequest.class)))
                .willThrow(new FeignException.Unauthorized("Unauthorized", request, null, null));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(LoginFailException.class)
                .hasMessage("아이디 또는 비밀번호가 올바르지 않습니다.");
    }

    // ==================== 2. 로그아웃 테스트 ====================

    @Test
    @DisplayName("3. 로그아웃 성공 - Redis에서 RefreshToken이 삭제되고 AccessToken JTI가 블랙리스트에 등록된다")
    void logout_Success() {
        // given
        String accessToken = "mockAccessToken";
        Long userId = 123L;
        String jti = "mockJti";

        given(jwtProvider.getUserIdFromToken(accessToken)).willReturn(userId);
        given(jwtProvider.getJtiFromToken(accessToken)).willReturn(jti);
        given(jwtProvider.getRemainingTime(accessToken)).willReturn(300000L); // 5분 남음
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");
        given(jwtProperties.getBlacklistPrefix()).willReturn("blacklist:");

        // when
        authService.logout(accessToken);

        // then
        verify(redisTemplate).delete("refreshToken:123");
        verify(valueOperations).set(
                eq("blacklist:mockJti"),
                eq("logout"),
                eq(300000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("4. 로그아웃 - AccessToken 남은 시간이 0 이하인 경우 블랙리스트에 등록하지 않는다")
    void logout_ExpiredAccessToken_DoesNotBlacklist() {
        // given
        String accessToken = "expiredAccessToken";
        Long userId = 123L;

        given(jwtProvider.getUserIdFromToken(accessToken)).willReturn(userId);
        given(jwtProvider.getJtiFromToken(accessToken)).willReturn("jti");
        given(jwtProvider.getRemainingTime(accessToken)).willReturn(0L); // 만료됨
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");

        // when
        authService.logout(accessToken);

        // then
        verify(redisTemplate).delete("refreshToken:123");
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    // ==================== 3. JTI 즉시 무효화 (invalidateJti) 테스트 ====================

    @Test
    @DisplayName("5. JTI 무효화 성공 - 권한 변경 등으로 발생한 JTI가 블랙리스트에 정상 등록된다")
    void invalidateJti_Success() {
        // given
        String jti = "targetJti";
        given(jwtProperties.getBlacklistPrefix()).willReturn("blacklist:");
        given(jwtProperties.getAccessTokenExpiration()).willReturn(1800000L);

        // when
        authService.invalidateJti(jti);

        // then
        verify(valueOperations).set(
                eq("blacklist:targetJti"),
                eq("role-changed"),
                eq(1800000L),
                eq(TimeUnit.MILLISECONDS)
        );
    }

    @Test
    @DisplayName("6. JTI 무효화 건너뜀 - JTI가 null이거나 빈 문자열이면 등록하지 않는다")
    void invalidateJti_NullOrEmpty_DoesNothing() {
        // when
        authService.invalidateJti(null);
        authService.invalidateJti("");

        // then
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    // ==================== 4. 토큰 재발급(refresh) 테스트 ====================

    @Test
    @DisplayName("7. 토큰 재발급 성공 - 유효한 RefreshToken으로 새로운 AccessToken이 재발급된다")
    void refresh_Success() {
        // given
        String oldRefreshToken = "mockOldRefreshToken";
        String newAccessToken = "mockNewAccessToken";
        Long userId = 123L;
        UserResponse userResponse = new UserResponse(userId, "testUser", "USER", "test@example.com", "홍길동", "ACTIVE", LocalDateTime.now());

        given(jwtProvider.validateToken(oldRefreshToken)).willReturn(true);
        given(jwtProvider.getUserIdFromToken(oldRefreshToken)).willReturn(userId);
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");
        given(valueOperations.get("refreshToken:123")).willReturn(oldRefreshToken);
        given(accountClient.getUser(userId, userId)).willReturn(ResponseEntity.ok(userResponse));
        given(jwtProvider.createAccessToken(eq(userId), eq("testUser"), eq("USER"))).willReturn(newAccessToken);

        // when
        TokenResponse tokenResponse = authService.refresh(oldRefreshToken);

        // then
        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.accessToken()).isEqualTo(newAccessToken);
        assertThat(tokenResponse.refreshToken()).isEqualTo(oldRefreshToken);
    }

    @Test
    @DisplayName("8. 토큰 재발급 실패 - 서명이 손상되었거나 만료된 RefreshToken인 경우 예외 발생")
    void refresh_Fail_InvalidJwtToken() {
        // given
        String invalidRefreshToken = "invalidToken";
        given(jwtProvider.validateToken(invalidRefreshToken)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.refresh(invalidRefreshToken))
                .isInstanceOf(RefreshTokenValidateException.class)
                .hasMessage("유효하지 않은 리프레시 토큰 입니다.");
    }

    @Test
    @DisplayName("9. 토큰 재발급 실패 - Redis에 토큰이 없거나 요청 토큰과 불일치 시 예외 발생")
    void refresh_Fail_RedisTokenMismatch() {
        // given
        String refreshToken = "myRefreshToken";
        Long userId = 123L;

        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");
        given(valueOperations.get("refreshToken:123")).willReturn(null); // Redis에 없음

        // when & then
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(RefreshTokenValidateException.class)
                .hasMessage("리프레시 토큰이 일치하지 않거나, 이미 만료되었습니다.");
    }

    @Test
    @DisplayName("10. 토큰 재발급 실패 - 회원 조회 실패(FeignException) 발생 시 예외 발생")
    void refresh_Fail_AccountClientFeignException() {
        // given
        String refreshToken = "validToken";
        Long userId = 123L;
        Request request = Request.create(Request.HttpMethod.GET, "/users/123", Map.of(), null, null, null);

        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");
        given(valueOperations.get("refreshToken:123")).willReturn(refreshToken);
        given(accountClient.getUser(userId, userId))
                .willThrow(new FeignException.NotFound("User Not Found", request, null, null));

        // when & then
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(RefreshTokenValidateException.class)
                .hasMessage("유효하지 않거나 탈퇴한 회원입니다. 다시 로그인 해주세요.");
    }

    @Test
    @DisplayName("11. 토큰 재발급 실패 - 회원 상태가 ACTIVE가 아닌 경우(WITHDRAWN 등) 예외 발생")
    void refresh_Fail_UserNotActive() {
        // given
        String refreshToken = "validToken";
        Long userId = 123L;
        UserResponse withdrawnUser = new UserResponse(userId, "testUser", "USER", "test@example.com", "홍길동", "WITHDRAWN", LocalDateTime.now());

        given(jwtProvider.validateToken(refreshToken)).willReturn(true);
        given(jwtProvider.getUserIdFromToken(refreshToken)).willReturn(userId);
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");
        given(valueOperations.get("refreshToken:123")).willReturn(refreshToken);
        given(accountClient.getUser(userId, userId)).willReturn(ResponseEntity.ok(withdrawnUser));

        // when & then
        assertThatThrownBy(() -> authService.refresh(refreshToken))
                .isInstanceOf(RefreshTokenValidateException.class)
                .hasMessage("비활성화되거나 탈퇴한 계정입니다.");
    }
}