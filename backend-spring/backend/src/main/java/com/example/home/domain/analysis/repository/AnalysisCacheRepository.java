package com.example.home.domain.analysis.repository;

import com.example.home.domain.analysis.entity.AnalysisCache;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AnalysisCacheRepository {

    AnalysisCache findByKey(
            @Param("eventId") Long eventId,
            @Param("windowMonths") int windowMonths,
            @Param("regionSignature") String regionSignature);

    void save(AnalysisCache cache);
}
