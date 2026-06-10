package com.example.home.domain.member.service;

import com.example.home.domain.member.dto.MemberRequest;
import com.example.home.domain.member.dto.MemberResponse;
import com.example.home.domain.member.entity.Member;
import com.example.home.domain.member.repository.MemberRepository;
import com.example.home.global.enums.MemberRole;
import com.example.home.global.enums.MemberStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultMemberService implements MemberService {

    private final MemberRepository memberRepository;

    @Override
    public MemberResponse findById(Long id) {
        return MemberResponse.from(memberRepository.findById(id));
    }

    @Override
    public boolean existsById(Long id) {
        return memberRepository.findById(id) != null;
    }

    @Override
    public boolean existsByEmail(String email) {
        return memberRepository.existsByEmail(email);
    }

    @Override
    public void register(MemberRequest request) {
        Member member = Member.builder()
                .email(request.email())
                .password(request.password()) // TODO: JWT 연동 시 PasswordEncoder 암호화 적용
                .nickname(request.nickname())
                .memberStatus(MemberStatus.ACTIVE)
                .memberRole(MemberRole.ROLE_USER)
                .build();
        memberRepository.save(member);
    }

    @Override
    public void update(Long id, MemberRequest request) {
        Member member = Member.builder()
                .userId(id)
                .nickname(request.nickname())
                .build();
        memberRepository.update(member);
    }

    @Override
    public void delete(Long id) {
        memberRepository.deleteById(id);
    }
}
