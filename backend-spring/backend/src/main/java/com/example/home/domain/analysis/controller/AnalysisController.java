package com.example.home.domain.analysis.controller;

import com.example.home.domain.analysis.dto.EventWindowRequest;
import com.example.home.domain.analysis.dto.EventWindowResponse;
import com.example.home.domain.analysis.service.AnalysisService;
import com.example.home.global.util.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analysis", description = "부동산 정책 충격 분석 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(summary = "이벤트 윈도우 분석", description = "정책 이벤트 기준 풍선효과 분석을 요청합니다. 동일 조건은 캐시에서 반환됩니다.")
    @PostMapping("/event-window")
    public ResponseEntity<BaseResponse<EventWindowResponse>> analyze(
            @RequestBody @Valid EventWindowRequest request) {
        return ResponseEntity.ok(BaseResponse.success("분석 완료", analysisService.analyze(request)));
    }

    @Operation(summary = "이벤트 목록 조회", description = "분석 가능한 정책 이벤트 목록을 반환합니다.")
    @GetMapping("/events")
    public ResponseEntity<BaseResponse<Map<String, Object>>> getEvents() {
        return ResponseEntity.ok(BaseResponse.success("이벤트 목록 조회 성공", analysisService.getEvents()));
    }
}
