package com.okbank.fintech.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "회원가입 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCreateRequest {
    @Schema(description = "휴대폰번호", example = "01056677055")
    @NotBlank(message = "휴대폰번호는 필수입니다")
    @Pattern(
            regexp = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$",
            message = "올바른 휴대폰번호 형식이 아닙니다"
    )
    private String mobile;

    @Schema(description = "이름", example = "윤교희")
    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 50, message = "이름은 50자 이하여야 합니다")
    private String name;

    @Schema(description = "비밀번호")
    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
