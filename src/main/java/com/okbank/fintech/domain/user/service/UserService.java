package com.okbank.fintech.domain.user.service;

import com.okbank.fintech.domain.auth.service.RefreshTokenService;
import com.okbank.fintech.domain.user.dto.UserCreateRequest;
import com.okbank.fintech.domain.auth.dto.UserResponse;
import com.okbank.fintech.domain.user.entity.Member;
import com.okbank.fintech.domain.user.repository.MemberRepository;
import com.okbank.fintech.global.exception.DuplicateException;
import com.okbank.fintech.global.exception.UserNotFoundException;
import com.okbank.fintech.global.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    private final RefreshTokenService refreshTokenService;

    public UserResponse getMe(String mobile) {
        Member member = memberRepository.findByMobile(mobile)
                .orElseThrow(() -> new IllegalArgumentException("User Not Found"));

        return UserResponse.from(member);
    }

    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        //사용자 중복 체크
        if(memberRepository.existsByMobile(request.getMobile()))
            throw new DuplicateException("이미 존재하는 사용자입니다");

        Member member = Member.builder()
                .mobile(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .build();

        Member saved = memberRepository.save(member);

        return UserResponse.from(saved);
    }

    /**
     * 탈퇴 처리
     *
     * @param mobile
     */
    public void withdraw(String mobile) {
        var member = memberRepository.findByMobile(mobile)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다"));

        //탈퇴 처리
        member.withdraw();

        refreshTokenService.delete(mobile);
    }
}
