package com.nhnacademy.userauthapi.config.properties;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JwtPropertiesTest {

    @Test
    @DisplayName("JwtProperties Getter/Setter 및 printProperties 정상 동작 검증")
    void testJwtProperties(){
        //given
        JwtProperties properties= new JwtProperties();
        properties.setSecretKey("testSecretKey");
        properties.setAccessTokenExpiration(3600000L);
        properties.setRefreshTokenExpiration(1209600000L);
        properties.setRefreshPrefix("refreshToken:");
        properties.setBlacklistPrefix("blacklist:");

        //then
        assertThat(properties.getSecretKey()).isEqualTo("testSecretKey");
        assertThat(properties.getAccessTokenExpiration()).isEqualTo(3600000L);
        assertThat(properties.getRefreshTokenExpiration()).isEqualTo(1209600000L);
        assertThat(properties.getRefreshPrefix()).isEqualTo("refreshToken:");
        assertThat(properties.getBlacklistPrefix()).isEqualTo("blacklist:");

        //@PostConstruct 로깅 메서드 호출 커버리지 채우기
        properties.printProperties();
    }

}