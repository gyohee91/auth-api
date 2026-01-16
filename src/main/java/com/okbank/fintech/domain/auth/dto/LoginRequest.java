package com.okbank.fintech.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Schema(description = "로그인(요청)")
@Getter
public class LoginRequest {
    @Schema(description = "휴대폰번호", example = "01056677055")
    @NotBlank(message = "휴대폰번호는 필수입니다")
    private String mobile;

    @Schema(description = "비밀번호", example = "1234")
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
