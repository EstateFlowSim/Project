package com.example.home.domain.house.dto;

public record AptDealResponse(
        String regionCode,
        String dong,
        String aptName,
        String jibun,
        int dealYear,
        int dealMonth,
        int dealDay,
        double area,
        int floor,
        long dealAmount,
        int buildYear
) {}
