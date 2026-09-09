package com.nhnacademy.userauthapi.config;

import io.micrometer.observation.ObservationPredicate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.observation.ServerRequestObservationContext;

@Configuration
public class TracingConfig {

    @Bean
    public ObservationPredicate noActuatorObservations() {
        return (name, context) -> {
            if (context instanceof ServerRequestObservationContext) {
                ServerRequestObservationContext serverContext = (ServerRequestObservationContext) context;
                String path = serverContext.getCarrier().getRequestURI();
                return !path.startsWith("/actuator");
            }
            return true;
        };
    }
}