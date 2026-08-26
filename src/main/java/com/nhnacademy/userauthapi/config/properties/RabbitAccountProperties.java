package com.nhnacademy.userauthapi.config.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * # rabbitmq queue 정보
 * rabbitmq:
 *   # account 유저 권한 변경
 *   account:
 *     role-change:
 *       exchange: account.events
 *       routing-key: account.role-change
 *       queue: auth.account.role-change.queue
 */

@Slf4j
@Getter
@Setter
@Configuration 
@ConfigurationProperties(prefix = "rabbitmq.account.role-change")
public class RabbitAccountProperties {
    private String exchange;
    private String routingKey;
    private String queue;
    private String deadLetterExchange;
    private String deadLetterRoutingKey;
    private String deadLetterQueue;

    @PostConstruct
    public void printProperties() {
        log.info("====== RabbitMQ Account Properties Loaded ======");
        log.info("exchange 셋팅 여부: {}", exchange != null ? "정상 로드됨" : "NULL");
        log.info("routingKey 셋팅 여부: {}", routingKey != null ? "정상 로드됨" : "NULL");
        log.info("queue 셋팅 여부: {}", queue != null ? "정상 로드됨" : "NULL");
        log.info("dlq 셋팅 여부: {}", deadLetterQueue != null ? "정상 로드됨" : "NULL");
        log.info("====================================");
    }
}
