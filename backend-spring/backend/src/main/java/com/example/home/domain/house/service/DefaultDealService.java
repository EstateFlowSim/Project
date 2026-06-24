package com.example.home.domain.house.service;

import com.example.home.domain.house.client.MolitApiClient;
import com.example.home.domain.house.dto.AptDealResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultDealService implements DealService {

    private final MolitApiClient molitApiClient;

    @Override
    public List<AptDealResponse> getAptDeals(String regionCode, String yearMonth) {
        return molitApiClient.fetchDeals(regionCode, yearMonth);
    }
}
