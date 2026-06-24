package com.example.home.domain.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record ScenarioRegionProfile(
        @JsonProperty("region_code") String regionCode,
        @JsonProperty("region_name") String regionName,
        @JsonProperty("speculation_score") Double speculationScore,
        @JsonProperty("stability_score") Double stabilityScore,
        @JsonProperty("migration_score") Double migrationScore,
        @JsonProperty("persona_distribution") Map<String, Integer> personaDistribution,
        @JsonProperty("selection_reasons") java.util.List<String> selectionReasons
) {}
