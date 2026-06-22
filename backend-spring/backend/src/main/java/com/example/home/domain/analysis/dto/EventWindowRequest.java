package com.example.home.domain.analysis.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record EventWindowRequest(
        @NotNull Long eventId,
        @NotNull Integer windowMonths,
        List<String> regionCodes
) {}
