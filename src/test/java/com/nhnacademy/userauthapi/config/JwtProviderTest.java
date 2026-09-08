package com.nhnacademy.userauthapi.config;

import com.nhnacademy.userauthapi.config.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtProviderTest{

    @Mock
    private JwtProperties jwtProperties;

    private JwtProvider jwtProvider;

    private final  String testSecretKey="dGVzdF9zZWNyZXRfa2V5X2Zvcl9qd3RfcHJvdmlkZXJfdGVzdF8xMjM0NTY3ODkwMQ==";

    @BeforeEach
    void setup(){
        given(jwtProperties.getSecretKey()).willReturn(testSecretKey);
        given(jwtProperties.getAccessTokenExpiration()).willReturn(3600000L); //1시간
        given(jwtProperties.getRefreshTokenExpiration()).willReturn(1209600000L); //14일

        jwtProvider=new JwtProvider(jwtProperties);
    }

    @Test
    @DisplayName("Access Token 생성 및 파싱 검증- userId, loginId, role, JTI가 정상 추출된다")
    void createAccessToken(){
        //given
        Long userId=1L;
        String loginId="user1";
        String role="ROLE_NORMAL";

        //when : 실제 JwtProvider의 createAccessToken() 메서드를 호출하여 토큰 문자열 생성
        String accessToken= jwtProvider.createAccessToken(userId,loginId,role);

        //then
        assertThat(accessToken).isNotNull();
        assertThat(jwtProvider.validateToken(accessToken)).isTrue();
        assertThat(jwtProvider.getUserIdFromToken(accessToken)).isEqualTo(userId);
        assertThat(jwtProvider.getLoginIdFromToken(accessToken)).isEqualTo(loginId);
        assertThat(jwtProvider.getRoleFromToken(accessToken)).isEqualTo(role);
        assertThat(jwtProvider.getJtiFromToken(accessToken)).isNotNull();
    }

    @Test
    @DisplayName("Refresh Token 생성 및 파싱 검증- userId가 정상 추출된다")
    void createRefreshToken(){
        //given: Refresh 토큰 생성에 필요한 사용자 식별자 준비
        Long userId=1L;

        //when: Refresh토큰 생성 메서드 호출(Refresh 토큰은 용량을 줄이기 위해 loginId나 role 없이 userId만 담음)
        String refreshToken= jwtProvider.createRefreshToken(userId);

        //then
        assertThat(refreshToken).isNotNull(); //토큰 생성 확인
        assertThat(jwtProvider.validateToken(refreshToken)).isTrue(); //서명 검증 성공 확인
        assertThat(jwtProvider.getUserIdFromToken(refreshToken)).isEqualTo(userId); // 토큰 내 파싱 확인
    }

    @Test
    @DisplayName("토큰 남은 만료시간 계산- 정상 토큰은 0보다 큰 남은 시간을 반환한다")
    void getRemainingTime_Success(){
        //given: 만료시간이 1시간으로 설정된 유효한 Access 토큰 생성
        String accessToken= jwtProvider.createAccessToken(1L,"user1","ROLE_NORMAL");

        //when: 방금 만든 토큰의 남은 수명(밀리초) 계산
        Long remainingTime=jwtProvider.getRemainingTime(accessToken);

        //then: 방금 만들었으므로 남은 시간이 0보다 커야함
        assertThat(remainingTime).isGreaterThan(0L);
    }

    @Test
    @DisplayName("유효하지 않은 토큰 남은시간 조회- 0L을 반환한다")
    void getRemainingTime_InvalidToken(){
        //given: 토큰 형태가 아닌 엉터리 가짜 문자열 준비
        String invalidToken= "invalid.jwt.token.string";

        //when: 엉터리 문자열로 남은 수명 계산 시도
        Long remainingTime=jwtProvider.getRemainingTime(invalidToken);

        //then: JwtProvider 내부의 catch(JwtException) 블록에 의해 에러가 튀어나오지 않고 안전하게 0L이 반환되어야함
        assertThat(remainingTime).isEqualTo(0L);
    }

    @Test
    @DisplayName("토큰 검증 실패- 훼손되거나 잘못된 문자열 토큰은 validateToken이 false를 반환한다")
    void validateToken(){
        //given: 위변조되거나 엉터리인 가짜 토큰 문자열 준비
        String invalidToken="invalid.jwt.token.string";

        //when: 토큰 서명 및 유효성 검증 메서드 호출
        boolean isValid=jwtProvider.validateToken(invalidToken);

        //then: 파싱 실패로 인해 catch 블록으로 빠져 false를 반환해야함
        assertThat(isValid).isFalse();
    }

}