<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import type { District, MapEvent } from '@/types/analysis'
import { useAnalysisStore } from '@/stores/analysisStore'
import { LAWD_CD_TO_NAME } from '@/constants/regionCodes'
import ShockMap from '@/components/map/ShockMap.vue'
import EventSelector from '@/components/analysis/EventSelector.vue'
import MetricsPanel from '@/components/analysis/MetricsPanel.vue'
import TimelineSlider from '@/components/analysis/TimelineSlider.vue'
import '@/assets/styles/analysis.css'

const MONTHS = ['T-3', 'T-2', 'T-1', 'T+0', 'T+1', 'T+2', 'T+3', 'T+4', 'T+5', 'T+6']

// API 미연결 시 fallback — 기준금리 인상 이벤트 기준 단일 값
const MOCK_DISTRICTS: District[] = [
  { name: '강남구',   change: 18.2, lag: 1, vol: -18 },
  { name: '서초구',   change: 15.8, lag: 1, vol: -16 },
  { name: '송파구',   change: 14.1, lag: 2, vol: -15 },
  { name: '강동구',   change: 11.3, lag: 2, vol: -12 },
  { name: '용산구',   change: 10.2, lag: 3, vol: -11 },
  { name: '성동구',   change:  9.1, lag: 3, vol: -10 },
  { name: '마포구',   change:  8.5, lag: 3, vol:  -9 },
  { name: '광진구',   change:  7.8, lag: 4, vol:  -8 },
  { name: '서대문구', change:  6.2, lag: 4, vol:  -7 },
  { name: '양천구',   change:  6.0, lag: 4, vol:  -6 },
  { name: '강서구',   change:  5.5, lag: 5, vol:  -6 },
  { name: '은평구',   change:  5.8, lag: 4, vol:  -6 },
  { name: '강북구',   change:  4.2, lag: 5, vol:  -5 },
  { name: '도봉구',   change:  4.6, lag: 5, vol:  -5 },
  { name: '노원구',   change:  5.1, lag: 5, vol:  -6 },
  { name: '중구',     change:  7.2, lag: 3, vol:  -8 },
  { name: '종로구',   change:  6.8, lag: 3, vol:  -7 },
  { name: '성북구',   change:  5.5, lag: 4, vol:  -6 },
  { name: '동대문구', change:  6.0, lag: 4, vol:  -6 },
  { name: '중랑구',   change:  5.3, lag: 5, vol:  -5 },
  { name: '구로구',   change:  5.8, lag: 4, vol:  -6 },
  { name: '금천구',   change:  5.2, lag: 5, vol:  -5 },
  { name: '동작구',   change:  7.5, lag: 3, vol:  -8 },
  { name: '관악구',   change:  6.5, lag: 4, vol:  -7 },
  { name: '영등포구', change:  7.8, lag: 3, vol:  -8 },
]

const store = useAnalysisStore()

// ── UI 상태 ────────────────────────────────────────────────────────────────
const selectedEvIdx = ref(0)
const curMonth      = ref(3)    // MONTHS 배열 인덱스 (T+0 = 3)
const playing       = ref(false)
const showLabels    = ref(true)

// ── API 이벤트 → MapEvent 변환 ─────────────────────────────────────────────
// event_ym "202505" → short "2025.05"
const toShort = (ym: string) => ym.slice(0, 4) + '.' + ym.slice(4, 6)

const events = computed<MapEvent[]>(() =>
  store.events.map(e => ({
    id:    e.id,
    name:  e.name,
    short: toShort(e.event_ym),
  }))
)

const currentEvent = computed(() => events.value[selectedEvIdx.value])

// ── API 결과 → District[] 변환 ────────────────────────────────────────────
const apiDistricts = computed((): District[] => {
  const result = store.analysisResult
  if (!result) return []
  return result.regions
    .map(r => {
      const name = LAWD_CD_TO_NAME[r.dong_code]
      if (!name) return null
      return {
        name,
        change: r.window_summary.final_price_change_pct,
        lag:    r.window_summary.lag_months ?? 0,
        vol:    r.window_summary.final_volume_change_pct,
      } satisfies District
    })
    .filter((d): d is District => d !== null)
})

// API 결과가 있으면 사용, 없으면 mock fallback
const districts = computed<District[]>(() =>
  apiDistricts.value.length > 0 ? apiDistricts.value : MOCK_DISTRICTS
)

// curMonth ↔ store.currentRelativeMonth 동기화
watch(curMonth, m => { store.currentRelativeMonth = m - 3 })

// ── 이벤트 선택 ────────────────────────────────────────────────────────────
function onEventSelect(i: number) {
  const ev = events.value[i]
  if (!ev) return
  selectedEvIdx.value = i
  curMonth.value      = 3        // T+0 리셋
  store.selectEvent(ev.id)
}

// ── 초기 로드 ──────────────────────────────────────────────────────────────
onMounted(async () => {
  await store.fetchEvents()
  // 이벤트 목록 로드 후 첫 이벤트로 분석 요청
  const first = events.value[0]
  if (first) {
    store.selectedEventId = first.id
    void store.fetchEventWindow()
  }
})
</script>

<template>
  <!-- 지도 초기화 로딩 (ShockMap이 id로 직접 제어) -->
  <div id="loading">
    <div class="ld-title">SHOCKPROP</div>
    <div class="ld-bar"><div class="ld-fill" id="ldFill" style="width:0%"></div></div>
    <div class="ld-txt" id="ldTxt">행정구역 데이터 로딩 중...</div>
  </div>

  <!-- API 상태 표시 -->
  <div v-if="store.loading" class="api-loading">분석 데이터 로딩 중...</div>
  <div v-if="store.error" class="api-error">{{ store.error }}</div>

  <ShockMap
    :districts="districts"
    :cur-month="curMonth"
    :show-labels="showLabels"
  />

  <div class="ui">
    <div class="hdr">
      <div class="logo-sq">SP</div>
      <div>
        <div class="logo-nm">ShockProp</div>
        <div class="logo-sb">부동산 정책 충격 전파 분석</div>
      </div>
      <div class="hdiv"></div>
      <EventSelector
        :events="events"
        :model-value="selectedEvIdx"
        @update:model-value="onEventSelect"
      />
      <div class="hdr-r">
        <div class="ltog" :class="{ on: showLabels }" @click="showLabels = !showLabels">
          <div class="tog-dot"></div>지역 라벨
        </div>
        <div class="live"><div class="ldot"></div>LIVE</div>
      </div>
    </div>

    <MetricsPanel
      :districts="districts"
      :cur-month="curMonth"
    />

    <TimelineSlider
      :months="MONTHS"
      :model-value="curMonth"
      :playing="playing"
      :current-event="currentEvent"
      @update:model-value="curMonth = $event"
      @update:playing="playing = $event"
    />
  </div>
</template>
