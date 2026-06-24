package com.example.home.domain.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ScenarioDocument(
        @JsonProperty("scenario_id") String scenarioId,
        String status,
        @JsonProperty("created_at") String createdAt,
        ScenarioSource source,
        @JsonProperty("selected_regions") List<ScenarioRegionProfile> selectedRegions,
        List<ScenarioRound> rounds,
        @JsonProperty("final_summary") ScenarioFinalSummary finalSummary
) {}
