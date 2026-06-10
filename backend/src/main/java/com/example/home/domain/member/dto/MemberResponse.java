package com.example.home.domain.member.dto;

import com.example.home.domain.member.entity.Member;
import com.example.home.global.enums.MemberRole;
import com.example.home.global.enums.MemberStatus;

public record MemberResponse(
        String email,
        String nickname,
        MemberStatus memberStatus,
        MemberRole memberRole
) {
    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getEmail(),
                member.getNickname(),
                member.getMemberStatus(),
                member.getMemberRole()
        );
    }
}
