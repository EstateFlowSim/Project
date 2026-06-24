package com.example.home.domain.house.controller;

import com.example.home.domain.house.dto.AptDealResponse;
import com.example.home.domain.house.service.DealService;
import com.example.home.global.util.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Deal", description = "아파트 실거래가 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/deals")
public class DealController {

    private final DealService dealService;

    @Operation(summary = "아파트 실거래가 조회")
    @GetMapping("/apt")
    public ResponseEntity<BaseResponse<List<AptDealResponse>>> getAptDeals(
            @Parameter(description = "법정동 코드 5자리 (예: 11680)") @RequestParam String regionCode,
            @Parameter(description = "거래 연월 YYYYMM (예: 202401)") @RequestParam String yearMonth) {
        List<AptDealResponse> deals = dealService.getAptDeals(regionCode, yearMonth);
        return ResponseEntity.ok(BaseResponse.success("아파트 실거래가 조회 성공", deals));
    }
}
