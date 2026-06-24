package com.example.home.domain.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ScenarioFinalSummary(
        @JsonProperty("selected_region_count") Integer selectedRegionCount,
        @JsonProperty("round_count") Integer roundCount,
        @JsonProperty("key_takeaways") List<String> keyTakeaways,
        @JsonProperty("most_reactive_region") String mostReactiveRegion,
        @JsonProperty("dominant_market_mood") String dominantMarketMood
) {}
