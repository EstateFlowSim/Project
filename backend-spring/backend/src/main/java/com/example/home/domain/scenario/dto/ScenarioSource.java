package com.example.home.domain.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ScenarioSource(
        @JsonProperty("analysis_cache_id") Long analysisCacheId,
        @JsonProperty("event_id") Long eventId,
        @JsonProperty("window_months") Integer windowMonths,
        @JsonProperty("requested_region_codes") List<String> requestedRegionCodes,
        @JsonProperty("selected_region_codes") List<String> selectedRegionCodes,
        @JsonProperty("agents_per_region") Integer agentsPerRegion
) {}
