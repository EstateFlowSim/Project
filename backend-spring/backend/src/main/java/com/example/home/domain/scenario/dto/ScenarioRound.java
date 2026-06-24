package com.example.home.domain.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ScenarioRound(
        @JsonProperty("relative_month") Integer relativeMonth,
        String label,
        @JsonProperty("market_mood") String marketMood,
        String narrative,
        List<ScenarioRoundRegion> regions
) {}
