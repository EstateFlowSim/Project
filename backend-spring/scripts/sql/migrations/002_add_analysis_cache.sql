-- analysis_cache: FastAPI 분석 결과 캐시
-- event_id + window_months + region_signature 조합이 동일하면 재사용
CREATE TABLE IF NOT EXISTS analysis_cache (
    cache_id         BIGINT       NOT NULL AUTO_INCREMENT,
    event_id         BIGINT       NOT NULL,
    window_months    INT          NOT NULL,
    region_signature VARCHAR(500) NOT NULL DEFAULT 'ALL',
    result_json      JSON         NOT NULL,
    created_at       DATETIME     NOT NULL DEFAULT NOW(),

    PRIMARY KEY (cache_id),
    UNIQUE KEY uk_analysis_cache (event_id, window_months, region_signature)
);
