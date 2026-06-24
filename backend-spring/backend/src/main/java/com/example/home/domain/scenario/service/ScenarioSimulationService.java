package com.example.home.domain.scenario.service;

import com.example.home.domain.analysis.dto.EventWindowResponse;
import com.example.home.domain.report.service.RegionNameResolver;
import com.example.home.domain.scenario.dto.ScenarioDocument;
import com.example.home.domain.scenario.dto.ScenarioFinalSummary;
import com.example.home.domain.scenario.dto.ScenarioPersonaSnapshot;
import com.example.home.domain.scenario.dto.ScenarioRegionProfile;
import com.example.home.domain.scenario.dto.ScenarioRound;
import com.example.home.domain.scenario.dto.ScenarioRoundRegion;
import com.example.home.domain.scenario.dto.ScenarioSource;
import com.example.home.domain.scenario.model.AgentStance;
import com.example.home.domain.scenario.model.PersonaType;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ScenarioSimulationService {

    public ScenarioDocument simulate(
            Long analysisCacheId,
            Long eventId,
            Integer windowMonths,
            List<String> requestedRegionCodes,
            int agentsPerRegion,
            int maxRegions,
            EventWindowResponse analysisResult) {
        Map<String, Map<String, Object>> regionByCode = indexRegions(analysisResult.regions());
        List<String> selectedRegionCodes = selectReactiveRegions(analysisResult, regionByCode, maxRegions);
        if (selectedRegionCodes.isEmpty()) {
            selectedRegionCodes = regionByCode.keySet().stream().limit(maxRegions).toList();
        }

        List<RegionContext> contexts = selectedRegionCodes.stream()
                .map(code -> buildRegionContext(code, regionByCode.get(code), agentsPerRegion))
                .toList();

        List<Integer> roundAxis = collectRoundAxis(contexts);
        List<ScenarioRound> rounds = new ArrayList<>();
        List<String> moodTimeline = new ArrayList<>();

        for (Integer relativeMonth : roundAxis) {
            List<ScenarioRoundRegion> regionRounds = new ArrayList<>();
            List<String> dominantStances = new ArrayList<>();

            for (RegionContext context : contexts) {
                MonthlySignal signal = context.monthlyByRelativeMonth.getOrDefault(relativeMonth, MonthlySignal.EMPTY);
                List<ScenarioPersonaSnapshot> personaStates = simulatePersonaStates(context, signal, relativeMonth);
                String dominant = dominantStance(personaStates);
                dominantStances.add(dominant);
                regionRounds.add(new ScenarioRoundRegion(
                        context.regionCode,
                        context.regionName,
                        signal.priceChangePct,
                        signal.volumeChangePct,
                        context.impactScore,
                        personaStates,
                        dominant));
            }

            String mood = marketMood(dominantStances);
            moodTimeline.add(mood);
            rounds.add(new ScenarioRound(
                    relativeMonth,
                    monthLabel(relativeMonth),
                    mood,
                    narrativeForRound(relativeMonth, mood, regionRounds),
                    regionRounds));
        }

        List<ScenarioRegionProfile> selectedRegions = contexts.stream()
                .map(context -> new ScenarioRegionProfile(
                        context.regionCode,
                        context.regionName,
                        context.speculationScore,
                        context.stabilityScore,
                        context.migrationScore,
                        context.personaDistribution.entrySet().stream()
                                .collect(Collectors.toMap(entry -> entry.getKey().name(), Map.Entry::getValue,
                                        (a, b) -> a, LinkedHashMap::new)),
                        context.selectionReasons))
                .toList();

        ScenarioFinalSummary finalSummary = new ScenarioFinalSummary(
                selectedRegions.size(),
                rounds.size(),
                finalTakeaways(contexts, rounds),
                contexts.stream().max(Comparator.comparingDouble(RegionContext::reactivityScore))
                        .map(context -> context.regionName)
                        .orElse("정보 없음"),
                dominantMood(moodTimeline));

        return new ScenarioDocument(
                UUID.randomUUID().toString(),
                "COMPLETED",
                OffsetDateTime.now().toString(),
                new ScenarioSource(
                        analysisCacheId,
                        eventId,
                        windowMonths,
                        requestedRegionCodes,
                        selectedRegionCodes,
                        agentsPerRegion),
                selectedRegions,
                rounds,
                finalSummary);
    }

    private Map<String, Map<String, Object>> indexRegions(List<Map<String, Object>> regions) {
        Map<String, Map<String, Object>> indexed = new LinkedHashMap<>();
        if (regions == null) {
            return indexed;
        }
        for (Map<String, Object> region : regions) {
            String code = stringValue(region.get("dong_code"));
            if (RegionNameResolver.isScenarioSupportedRegionCode(code)) {
                indexed.put(code, region);
            }
        }
        return indexed;
    }

    private List<String> selectReactiveRegions(
            EventWindowResponse analysisResult,
            Map<String, Map<String, Object>> regionByCode,
            int maxRegions) {
        Set<String> selected = new LinkedHashSet<>();
        Map<String, Object> rankings = analysisResult.rankings() == null ? Map.of() : analysisResult.rankings();
        for (String key : List.of("highest_impact", "fastest_reaction", "top_price_rise", "top_price_drop", "top_volume_rise")) {
            for (Map<String, Object> ranked : mapList(rankings.get(key))) {
                String code = stringValue(ranked.get("dong_code"));
                if (regionByCode.containsKey(code)) {
                    selected.add(code);
                }
                if (selected.size() >= maxRegions) {
                    return new ArrayList<>(selected);
                }
            }
        }

        regionByCode.values().stream()
                .sorted(Comparator.comparingDouble(this::reactivityScore).reversed())
                .map(region -> stringValue(region.get("dong_code")))
                .forEach(selected::add);
        return selected.stream().limit(maxRegions).toList();
    }

    private RegionContext buildRegionContext(String regionCode, Map<String, Object> region, int agentsPerRegion) {
        Map<String, Object> summary = mapValue(region.get("window_summary"));
        double impactScore = numberValue(summary.get("impact_score"));
        double finalPriceChange = numberValue(summary.get("final_price_change_pct"));
        double finalVolumeChange = numberValue(summary.get("final_volume_change_pct"));
        double lagMonths = numberValue(summary.get("lag_months"));
        String reactionRole = stringValue(summary.get("reaction_role"));

        double speculationScore = clamp01(
                Math.min(impactScore / 10.0, 1.0) * 0.45
                        + (lagMonths <= 1 ? 0.20 : lagMonths <= 2 ? 0.12 : 0.05)
                        + Math.min(Math.abs(finalPriceChange) / 20.0, 0.25)
                        + ("leader".equalsIgnoreCase(reactionRole) ? 0.10 : 0));
        double stabilityScore = clamp01(
                0.90
                        - Math.min(Math.abs(finalPriceChange) / 25.0, 0.35)
                        - Math.min(Math.abs(finalVolumeChange) / 30.0, 0.30)
                        - (lagMonths <= 1 ? 0.10 : 0)
                        + ("follower".equalsIgnoreCase(reactionRole) ? 0.05 : 0));
        double migrationScore = clamp01(
                Math.min(Math.abs(finalVolumeChange) / 25.0, 0.35)
                        + ("follower".equalsIgnoreCase(reactionRole) ? 0.18 : 0.06)
                        + (lagMonths >= 2 ? 0.16 : 0.08)
                        + Math.min(Math.abs(finalPriceChange) / 18.0, 0.22));

        Map<PersonaType, Integer> personaDistribution = allocateAgents(agentsPerRegion, speculationScore, stabilityScore, migrationScore);
        Map<Integer, MonthlySignal> monthlyByRelativeMonth = mapMonthlySignals(mapList(region.get("monthly")));

        List<String> reasons = new ArrayList<>();
        reasons.add(impactScore >= 7.0 ? "충격 강도가 큰 지역" : "분석 구간 반응이 관측된 지역");
        if (lagMonths > 0 && lagMonths <= 2) {
            reasons.add("반응 시차가 짧아 선행 신호로 해석 가능한 지역");
        }
        if ("leader".equalsIgnoreCase(reactionRole)) {
            reasons.add("확산 흐름에서 선행 지역으로 분류된 지역");
        } else if ("follower".equalsIgnoreCase(reactionRole)) {
            reasons.add("후행 반응과 이동 수요 해석에 적합한 지역");
        }

        return new RegionContext(
                regionCode,
                RegionNameResolver.displayName(regionCode),
                impactScore,
                speculationScore,
                stabilityScore,
                migrationScore,
                personaDistribution,
                monthlyByRelativeMonth,
                reasons);
    }

    private Map<Integer, MonthlySignal> mapMonthlySignals(List<Map<String, Object>> monthlyRows) {
        Map<Integer, MonthlySignal> mapped = new LinkedHashMap<>();
        for (Map<String, Object> row : monthlyRows) {
            Integer relativeMonth = integerValue(row.get("relative_month"));
            if (relativeMonth == null) {
                continue;
            }
            mapped.put(relativeMonth, new MonthlySignal(
                    numberValue(row.get("price_change_from_event_pct")),
                    numberValue(row.get("volume_change_from_event_pct"))));
        }
        return mapped;
    }

    private List<Integer> collectRoundAxis(List<RegionContext> contexts) {
        return contexts.stream()
                .map(context -> context.monthlyByRelativeMonth.keySet())
                .flatMap(Collection::stream)
                .distinct()
                .sorted()
                .toList();
    }

    private List<ScenarioPersonaSnapshot> simulatePersonaStates(RegionContext context, MonthlySignal signal, int relativeMonth) {
        List<ScenarioPersonaSnapshot> snapshots = new ArrayList<>();
        for (Map.Entry<PersonaType, Integer> entry : context.personaDistribution.entrySet()) {
            PersonaType personaType = entry.getKey();
            int totalAgents = entry.getValue();
            EnumMap<AgentStance, Integer> stances = new EnumMap<>(AgentStance.class);
            for (AgentStance stance : AgentStance.values()) {
                stances.put(stance, 0);
            }

            double sumSignal = 0.0;
            for (int index = 0; index < totalAgents; index++) {
                double trait = traitModifier(context.regionCode, personaType, index);
                AgentStance stance = decideStance(personaType, context, signal, relativeMonth, trait);
                stances.put(stance, stances.get(stance) + 1);
                sumSignal += signalStrength(personaType, context, signal, relativeMonth, trait);
            }

            snapshots.add(new ScenarioPersonaSnapshot(
                    personaType.name(),
                    personaType.label(),
                    totalAgents,
                    stances.entrySet().stream().collect(Collectors.toMap(
                            entrySet -> entrySet.getKey().name(),
                            Map.Entry::getValue,
                            (a, b) -> a,
                            LinkedHashMap::new)),
                    round2(sumSignal / totalAgents)));
        }
        return snapshots;
    }

    private AgentStance decideStance(
            PersonaType personaType,
            RegionContext context,
            MonthlySignal signal,
            int relativeMonth,
            double trait) {
        double strength = signalStrength(personaType, context, signal, relativeMonth, trait);
        return switch (personaType) {
            case END_USER -> {
                if (strength >= 0.62) {
                    yield AgentStance.BUY;
                }
                if (strength <= 0.34) {
                    yield AgentStance.WATCH;
                }
                yield AgentStance.HOLD;
            }
            case INVESTOR -> {
                if (strength >= 0.68) {
                    yield AgentStance.BUY;
                }
                if (strength <= 0.28) {
                    yield AgentStance.SELL;
                }
                if (strength <= 0.42) {
                    yield AgentStance.WATCH;
                }
                yield AgentStance.HOLD;
            }
            case MOVER -> {
                if (strength >= 0.66) {
                    yield AgentStance.MOVE;
                }
                if (strength <= 0.32) {
                    yield AgentStance.WATCH;
                }
                yield AgentStance.HOLD;
            }
        };
    }

    private double signalStrength(
            PersonaType personaType,
            RegionContext context,
            MonthlySignal signal,
            int relativeMonth,
            double trait) {
        double priceFactor = signal.priceChangePct / 20.0;
        double volumeFactor = signal.volumeChangePct / 25.0;
        return switch (personaType) {
            case END_USER -> clamp01(0.42
                    + context.stabilityScore * 0.35
                    + (signal.priceChangePct <= -3 ? 0.08 : 0.0)
                    - Math.max(0, -volumeFactor) * 0.10
                    - Math.max(0, priceFactor) * 0.05
                    + phaseBias(relativeMonth) * 0.02
                    + trait);
            case INVESTOR -> clamp01(0.34
                    + context.speculationScore * 0.40
                    + Math.max(0, priceFactor) * 0.12
                    - Math.max(0, -priceFactor) * 0.18
                    - Math.max(0, -volumeFactor) * 0.08
                    + phaseBias(relativeMonth) * 0.03
                    + trait);
            case MOVER -> clamp01(0.36
                    + context.migrationScore * 0.42
                    + Math.max(0, volumeFactor) * 0.10
                    + (relativeMonth >= 1 ? 0.05 : 0.0)
                    - Math.max(0, -priceFactor) * 0.04
                    + trait);
        };
    }

    private String dominantStance(List<ScenarioPersonaSnapshot> snapshots) {
        Map<String, Integer> counts = new HashMap<>();
        for (ScenarioPersonaSnapshot snapshot : snapshots) {
            snapshot.stanceCounts().forEach((key, value) -> counts.merge(key, value, Integer::sum));
        }
        return counts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey())
                .orElse("HOLD");
    }

    private String marketMood(List<String> dominantStances) {
        long buyCount = dominantStances.stream().filter("BUY"::equals).count();
        long sellCount = dominantStances.stream().filter("SELL"::equals).count();
        long moveCount = dominantStances.stream().filter("MOVE"::equals).count();
        long watchCount = dominantStances.stream().filter("WATCH"::equals).count();

        if (watchCount >= Math.max(buyCount, Math.max(sellCount, moveCount))) {
            return "관망 우세";
        }
        if (sellCount > buyCount) {
            return "매도 압력 확대";
        }
        if (moveCount >= buyCount) {
            return "갈아타기 탐색 확대";
        }
        return "선별 매수 탐색";
    }

    private String narrativeForRound(int relativeMonth, String mood, List<ScenarioRoundRegion> regionRounds) {
        ScenarioRoundRegion topRegion = regionRounds.stream()
                .max(Comparator.comparingDouble(region -> Math.abs(region.impactScore())))
                .orElse(null);
        if (topRegion == null) {
            return monthLabel(relativeMonth) + " 구간에는 시뮬레이션할 지역 데이터가 없습니다.";
        }
        return "%s 구간에는 '%s' 분위기가 우세했고, %s에서 %s 성향이 가장 두드러졌습니다."
                .formatted(monthLabel(relativeMonth), mood, topRegion.regionName(), topRegion.dominantStance());
    }

    private List<String> finalTakeaways(List<RegionContext> contexts, List<ScenarioRound> rounds) {
        List<String> takeaways = new ArrayList<>();
        contexts.stream().max(Comparator.comparingDouble(RegionContext::reactivityScore))
                .ifPresent(context -> takeaways.add(
                        "%s은(는) 충격 강도 %.1f, 투기 점수 %.2f로 핵심 반응 지역으로 분류되었습니다."
                                .formatted(context.regionName, context.impactScore, context.speculationScore)));
        if (!rounds.isEmpty()) {
            takeaways.add("라운드는 분석에 사용한 relative_month 축을 그대로 따라가며 시장 참여자 반응을 해석했습니다.");
        }
        long watchMoodCount = rounds.stream().filter(round -> "관망 우세".equals(round.marketMood())).count();
        if (watchMoodCount > 0) {
            takeaways.add("이벤트 직후 구간에서는 관망 우세 라운드가 %d회 관측되어 실수요자 대기 성향이 반영되었습니다."
                    .formatted(watchMoodCount));
        }
        return takeaways;
    }

    private String dominantMood(List<String> moods) {
        return moods.stream()
                .collect(Collectors.groupingBy(mood -> mood, LinkedHashMap::new, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("정보 없음");
    }

    private Map<PersonaType, Integer> allocateAgents(
            int totalAgents,
            double speculationScore,
            double stabilityScore,
            double migrationScore) {
        Map<PersonaType, Integer> distribution = new EnumMap<>(PersonaType.class);
        distribution.put(PersonaType.END_USER, 2);
        distribution.put(PersonaType.INVESTOR, 2);
        distribution.put(PersonaType.MOVER, 2);

        int remaining = Math.max(0, totalAgents - 6);
        Map<PersonaType, Double> weights = Map.of(
                PersonaType.END_USER, 0.45 + stabilityScore * 0.90,
                PersonaType.INVESTOR, 0.45 + speculationScore * 0.90,
                PersonaType.MOVER, 0.45 + migrationScore * 0.90);

        for (int i = 0; i < remaining; i++) {
            PersonaType next = weights.entrySet().stream()
                    .max(Comparator.comparingDouble(entry -> entry.getValue() - distribution.get(entry.getKey()) * 0.08))
                    .map(Map.Entry::getKey)
                    .orElse(PersonaType.END_USER);
            distribution.put(next, distribution.get(next) + 1);
        }
        return distribution;
    }

    private double traitModifier(String regionCode, PersonaType personaType, int index) {
        int raw = Math.abs((regionCode + ":" + personaType.name() + ":" + index).hashCode());
        return ((raw % 23) - 11) / 100.0;
    }

    private double phaseBias(int relativeMonth) {
        if (relativeMonth < 0) {
            return -0.05;
        }
        if (relativeMonth == 0) {
            return -0.02;
        }
        if (relativeMonth <= 2) {
            return 0.04;
        }
        return 0.06;
    }

    private double reactivityScore(Map<String, Object> region) {
        Map<String, Object> summary = mapValue(region.get("window_summary"));
        double impactScore = numberValue(summary.get("impact_score"));
        double lagMonths = numberValue(summary.get("lag_months"));
        double finalPrice = Math.abs(numberValue(summary.get("final_price_change_pct")));
        return impactScore + finalPrice * 0.25 + (lagMonths <= 1 ? 1.0 : lagMonths <= 2 ? 0.5 : 0);
    }

    private String monthLabel(int relativeMonth) {
        if (relativeMonth == 0) {
            return "정책 시행월";
        }
        return relativeMonth < 0
                ? "시행 " + Math.abs(relativeMonth) + "개월 전"
                : "시행 " + relativeMonth + "개월 후";
    }

    private Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }

        Map<String, Object> mapped = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            mapped.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return mapped;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> mapList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> (Map<String, Object>) item)
                .toList();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private double numberValue(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return 0.0;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private double round2(double value) {
        return Double.parseDouble(String.format(Locale.US, "%.2f", value));
    }

    private record MonthlySignal(double priceChangePct, double volumeChangePct) {
        private static final MonthlySignal EMPTY = new MonthlySignal(0.0, 0.0);
    }

    private record RegionContext(
            String regionCode,
            String regionName,
            double impactScore,
            double speculationScore,
            double stabilityScore,
            double migrationScore,
            Map<PersonaType, Integer> personaDistribution,
            Map<Integer, MonthlySignal> monthlyByRelativeMonth,
            List<String> selectionReasons
    ) {
        private double reactivityScore() {
            return impactScore + speculationScore + migrationScore;
        }
    }
}
