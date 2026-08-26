package com.nhnacademy.userauthapi.message;

import com.nhnacademy.userauthapi.dto.message.RoleChangeMessage;
import com.nhnacademy.userauthapi.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RoleChangeMessageListener {

    private final AuthService authService;

    @RabbitListener(queues = "${rabbitmq.account.role-change.queue:4iren.auth.account.role-change.queue}")
    public void handleAccountRoleChangeEvent(RoleChangeMessage message) {
        String jti = message.jti();
        Long userId = message.userId();
        String role = message.role();

        log.info("Account 권한 변경 이벤트 수신 - userId: {}, role: {}, jti: {}", userId, role, jti);

        // 서비스 계층으로 JTI 블랙리스트 처리 위임
        authService.invalidateJti(jti);
    }
}
