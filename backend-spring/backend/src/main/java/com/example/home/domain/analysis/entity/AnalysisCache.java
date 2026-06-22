package com.example.home.domain.analysis.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisCache {

    private Long cacheId;
    private Long eventId;
    private int windowMonths;
    private String regionSignature;
    private String resultJson;
    private LocalDateTime createdAt;
}
