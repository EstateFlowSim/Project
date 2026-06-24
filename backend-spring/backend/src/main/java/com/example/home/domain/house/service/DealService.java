package com.example.home.domain.house.service;

import com.example.home.domain.house.dto.AptDealResponse;

import java.util.List;

public interface DealService {
    List<AptDealResponse> getAptDeals(String regionCode, String yearMonth);
}
