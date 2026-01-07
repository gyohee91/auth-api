package com.okbank.fintech.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {
    @NotBlank(message = "휴대폰번호는 필수입니다")
    private String mobile;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
