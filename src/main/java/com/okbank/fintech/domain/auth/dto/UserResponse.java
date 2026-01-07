package com.okbank.fintech.domain.auth.dto;

import com.okbank.fintech.domain.user.entity.Member;
import com.okbank.fintech.domain.user.entity.UserRole;
import com.okbank.fintech.domain.user.entity.UserStatus;
import com.okbank.fintech.global.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    @Schema(description = "휴대폰번호", example = "01056677055")
    private String mobile;

    @Schema(description = "사용자명", example = "윤교희")
    private String name;

    @Schema(description = "권한", example = "USER")
    private UserRole role;

    @Schema(description = "상태", example = "ACTIVE")
    private UserStatus status;

    public static UserResponse from(Member member) {
        return UserResponse.builder()
                .mobile(member.getMobile())
                .name(member.getName())
                .role(member.getRole())
                .status(member.getStatus())
                .build();
    }

    public static UserResponse from(CustomUserDetails userDetails) {
        Member member = userDetails.getMember();
        return UserResponse.builder()
                .mobile(member.getMobile())
                .name(member.getName())
                .role(member.getRole())
                .status(member.getStatus())
                .build();
    }
}
