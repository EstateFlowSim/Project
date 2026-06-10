package com.example.home.domain.member.dto;

public record MemberRequest(
        String email,
        String password,
        String nickname
) {
}
