package com.example.home.domain.analysis.service;

import com.example.home.domain.analysis.dto.EventWindowRequest;
import com.example.home.domain.analysis.dto.EventWindowResponse;
import java.util.Map;

public interface AnalysisService {

    EventWindowResponse analyze(EventWindowRequest request);

    EventWindowResponse getCachedResult(Long cacheId);

    Map<String, Object> getEvents();
}
