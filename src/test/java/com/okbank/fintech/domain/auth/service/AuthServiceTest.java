package com.okbank.fintech.domain.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    private static final List<String> CODE_LIST = List.of(
            "/h2-console/",
            "/actuator",
            "/api/vi/auth/login"
    );
///h2-console
    @BeforeEach
    void setUp() {
    }

    @Test
    void login() {
        String requestId = "/h2-console";
        boolean flag = CODE_LIST.stream()
                .anyMatch(requestId::startsWith);

        System.out.println("flag: " + flag);
    }

    @Test
    void refreshToken() {
    }

    @Test
    void logout() {
    }
}