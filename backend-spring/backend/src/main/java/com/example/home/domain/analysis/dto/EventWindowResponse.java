package com.example.home.domain.analysis.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventWindowResponse(
        String status,
        @JsonProperty("analysis_cache_id")      Long analysisCacheId,
        Map<String, Object> event,
        Map<String, Object> analysis,
        @JsonProperty("data_range")       Map<String, Object> dataRange,
        Map<String, Object> summary,
        Map<String, Object> rankings,
        List<Map<String, Object>> regions,
        @JsonProperty("result_count")           Integer resultCount,
        @JsonProperty("complete_window_count")  Integer completeWindowCount,
        @JsonProperty("requested_region_count") Integer requestedRegionCount
) {
    public EventWindowResponse withAnalysisCacheId(Long analysisCacheId) {
        return new EventWindowResponse(
                status,
                analysisCacheId,
                event,
                analysis,
                dataRange,
                summary,
                rankings,
                regions,
                resultCount,
                completeWindowCount,
                requestedRegionCount);
    }
}
