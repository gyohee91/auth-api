package com.okbank.fintech.global.security;

import com.okbank.fintech.domain.user.entity.Member;
import com.okbank.fintech.domain.user.entity.UserRole;
import com.okbank.fintech.domain.user.entity.UserStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final Member member;

    public static CustomUserDetails from(Member member) {
        return new CustomUserDetails(member);
    }

    /**
     * 사용자 권한 목록 반환
     *
     * @return
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton(
                new SimpleGrantedAuthority("ROLE_" + member.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    /**
     * 사용자 이름(휴대폰 번호) 반환
     *
     * @return
     */
    @Override
    public String getUsername() {
        return member.getMobile();
    }

    /**
     * 계정 만료 여부 반환
     *
     * @return
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * 계정 잠금 여부 반환
     *
     * @return
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * 자격 증명 만료 여부 반환
     *
     * @return
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 계정 활성화 여부 반환
     *
     * @return
     */
    @Override
    public boolean isEnabled() {
        return Objects.equals(member.getStatus(), UserStatus.ACTIVE) && !member.isDeleted();
    }

    public String getName() {
        return member.getName();
    }

    public UserRole getUserRole() {
        return member.getRole();
    }

    /**
     * 사용자 상태 반환
     *
     * @return
     */
    public UserStatus getUserStatus() {
        return member.getStatus();
    }
}
