package com.okbank.fintech.domain.user.entity;

import com.okbank.fintech.global.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;
import org.springframework.security.core.userdetails.User;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Member extends BaseEntity {
    @Column(nullable = false, length = 50)
    @Comment("휴대폰번호")
    private String mobile;

    @Column(nullable = false)
    @Comment("비밀번호")
    private String password;

    @Comment("이름")
    private String name;

    @Comment("사용 여부")
    @Builder.Default
    private Boolean enable = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    @Comment("탈퇴일")
    private LocalDateTime withdrawAt;

    /**
     * 탈퇴 처리
     */
    public void withdraw() {
        this.enable = false;
        this.status = UserStatus.WITHDRAWN;
        this.withdrawAt = LocalDateTime.now();
    }
}

