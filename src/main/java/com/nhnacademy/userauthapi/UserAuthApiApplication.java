package com.nhnacademy.userauthapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class UserAuthApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserAuthApiApplication.class, args);
    }

}
