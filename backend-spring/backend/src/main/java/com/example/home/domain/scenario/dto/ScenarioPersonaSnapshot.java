package com.example.home.domain.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record ScenarioPersonaSnapshot(
        @JsonProperty("persona_type") String personaType,
        @JsonProperty("persona_label") String personaLabel,
        @JsonProperty("total_agents") Integer totalAgents,
        @JsonProperty("stance_counts") Map<String, Integer> stanceCounts,
        @JsonProperty("average_signal") Double averageSignal
) {}
