package com.example.home.domain.scenario.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record ScenarioRoundRegion(
        @JsonProperty("region_code") String regionCode,
        @JsonProperty("region_name") String regionName,
        @JsonProperty("price_change_pct") Double priceChangePct,
        @JsonProperty("volume_change_pct") Double volumeChangePct,
        @JsonProperty("impact_score") Double impactScore,
        @JsonProperty("persona_states") List<ScenarioPersonaSnapshot> personaStates,
        @JsonProperty("dominant_stance") String dominantStance
) {}
