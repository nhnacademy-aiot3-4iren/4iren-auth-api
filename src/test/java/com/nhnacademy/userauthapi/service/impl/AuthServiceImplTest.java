package com.nhnacademy.userauthapi.service.impl;

import com.nhnacademy.userauthapi.client.AccountClient;
import com.nhnacademy.userauthapi.config.JwtProperties;
import com.nhnacademy.userauthapi.config.JwtProvider;
import com.nhnacademy.userauthapi.dto.token.TokenResponse;
import com.nhnacademy.userauthapi.dto.login.LoginRequest;
import com.nhnacademy.userauthapi.dto.login.LoginResponse;
import com.nhnacademy.userauthapi.dto.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private AccountClient accountClient;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String,String> valueOperations;

    @Mock
    private JwtProperties jwtProperties;

    @InjectMocks
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        // redisTemplate.opsForValue()가 호출되면 가짜 valueOperations를 던져주도록 미리 세팅
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);


    }


    @Test
    void login() {
        //***Given . 테스트에 필요한 데이터와 가짜 객체들의 동작 미리 세팅

        //가짜 요청 데이터 준비
        Long userId= 123L;
        String userLoginId ="testUser";
        String password="password123";
        String userName="홍길동";
        String role="USER";
        LoginRequest loginRequest=new LoginRequest(userLoginId,password);


        //가짜 응답 데이터 준비
        // AccountClient가 넘겨줄 가짜 LoginResponse 객체 생성
        LoginResponse loginResponse=new LoginResponse(
                userId,
                userLoginId,
                userName,
                role);

        // [Mock 동작 정의 A] AccountClient.login()이 실행되면 성공(200 OK)과 함께 loginResponse를 반환하도록 세팅
        given(accountClient.login(any(LoginRequest.class)))
                .willReturn(ResponseEntity.ok(loginResponse));

        // [Mock 동작 정의 B] JwtProvider가 토큰을 생성할 때 가짜 토큰 문자열을 반환하도록 세팅
        given(jwtProvider.createAccessToken(eq(userId), eq(userLoginId),anyString())).willReturn("mockAccessToken");
        given(jwtProvider.createRefreshToken(eq(userId))).willReturn("mockRefreshToken");
        // [Mock 동작 정의 C] JwtProperties에서 만료 시간을 물어보면 1209600000ms (2주)를 반환하도록 세팅
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(1209600000L);
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");


        //***When. 실행. 실제 테스트 대상인 authservice.login()을 호출함
        TokenResponse tokenResponse=authService.login(loginRequest);


        //***Then. 검증단계 . 실행했던 결과가 기대했던 대로 나오는지 확인
        // [검증 1] 반환된 TokenResponse에 가짜 토큰들이 제대로 들어있는지 확인
        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.accessToken()).isEqualTo("mockAccessToken");
        assertThat(tokenResponse.refreshToken()).isEqualTo("mockRefreshToken");

        // [검증 2] Redis에 "refreshToken:123" 키로 리프레시 토큰이 정상 저장(set)되었는지 확인
        verify(valueOperations).set(
                eq("refreshToken:123" ),
                eq("mockRefreshToken"),
                eq(1209600000L),
                eq(TimeUnit.MILLISECONDS)
        );

}

    @Test
    void logout() {
        //given
        String accessToken="mockAccessToken";
        Long userId=123L;

        //Jwt provider가 토큰에서 유저 ID 및 남은 만료 시간을 제대로 반환하도록 Mocking 설정
        given(jwtProvider.getUserIdFromToken(accessToken)).willReturn(userId);
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");

        //when
        authService.logout(accessToken);

        //then. redisTemplate.delete()가 해당 키로 호출되었는지 검증
        verify(redisTemplate).delete("refreshToken:" +userId);
    }

    @Test
    void refresh() {
        //given. 테스트에 필요한 데이터와 가짜 객체들의 동작 미리 세팅
        String oldRefreshToken="mockOldRefreshToken";
        String newAccessToken="mockNewAccessToken";
        Long userId=123L;
        String userLoginId="testUser";
        String userName="홍길동";
        String role="USER";
        String email="test@example.com";
        LocalDateTime date=LocalDateTime.now();

        UserResponse userResponse=new UserResponse(userId, userLoginId, role, email, userName, "ACTIVE", date);

        //JwtProvider 토큰 검증 및 유저 ID 추출 세팅
        given(jwtProvider.validateToken(oldRefreshToken)).willReturn(true);
        given(jwtProvider.getUserIdFromToken(oldRefreshToken)).willReturn(userId);
        given(jwtProperties.getRefreshPrefix()).willReturn("refreshToken:");

        //redis에서 기존 리프레시 토큰 조회 세팅
        given(valueOperations.get("refreshToken:" +userId)).willReturn(oldRefreshToken);

        //AccountClient에서 최신 유저 정보 조회 세팅
        given(accountClient.getUser(userId, userId)).willReturn(ResponseEntity.ok(userResponse));

        //새로운 토큰 생성 및 만료 시간 세팅
        given(jwtProvider.createAccessToken(eq(userId), eq(userLoginId), eq("ROLE_"+role))).willReturn(newAccessToken);

        //when. 실행. 서비스의 refresh() 호출
        TokenResponse tokenResponse=authService.refresh(oldRefreshToken);

        //then. 검증. 실행했던 결과가 기대했던 대로 나오는지 확인
        //새롭게 재발급된 토큰들이 응답에 정상 수록되었는지 확인
        assertThat(tokenResponse).isNotNull();
        assertThat(tokenResponse.accessToken()).isEqualTo(newAccessToken);
        assertThat(tokenResponse.refreshToken()).isEqualTo(oldRefreshToken);

    }
}