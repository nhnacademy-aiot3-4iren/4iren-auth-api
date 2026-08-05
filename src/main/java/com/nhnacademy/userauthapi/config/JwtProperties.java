package com.nhnacademy.userauthapi.config;

//application.yml에서 jwt관련 설정을 읽어오는 클래스임

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter @Setter// yml값을 읽어서 필드에 대입하려면 Setter가 필요하고, 다른 클래스 JwtProvider등 에서 이 값들을 갖다 쓰려면 Setter가 필요함
@Configuration //이 클래스가 설정클래스임을 알려주는 어노테이션. 스프링 컨테이너가 이 클래스를 빈으로 관리하도록 등록함
@ConfigurationProperties(prefix = "jwt") //app.yml 설정 파일에서 접두사가 jwt로 시작하는 설정 파일들을 찾아 자동으로 이 클래스 변수들에 매핑 해줌
public class JwtProperties {
    private String secretKey;
    private long accessTokenExpiration;
    private long refreshTokenExpiration;

    @PostConstruct
    public void printProperties() {
        log.info("====== JWT Properties Loaded ======");
        log.info("SecretKey 셋팅 여부: {}", secretKey != null ? "정상 로드됨" : "NULL");
        log.info("Access 토큰 만료시간: {}", accessTokenExpiration);
        log.info("Refresh 토큰 만료시간: {}", refreshTokenExpiration);
        log.info("====================================");
    }
}
