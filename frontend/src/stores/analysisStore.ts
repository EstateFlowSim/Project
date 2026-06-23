import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getEvents, postEventWindow } from '@/api/analysisApi'
import type { ApiEvent, EventWindowResponse } from '@/types/analysis'

export const useAnalysisStore = defineStore('analysis', () => {
  // ── 이벤트 목록 ─────────────────────────────────────────────────────────────
  const events = ref<ApiEvent[]>([])

  // ── 요청 파라미터 ──────────────────────────────────────────────────────────
  const selectedEventId      = ref<number | null>(null)
  const windowMonths         = ref<3 | 6 | 12>(6)
  const regionCodes          = ref<string[]>([])   // 빈 배열 = 전체

  // ── 타임라인 상태 ─────────────────────────────────────────────────────────
  const currentRelativeMonth = ref<number>(0)      // -3 ~ +windowMonths

  // ── 분석 결과 ─────────────────────────────────────────────────────────────
  const analysisResult       = ref<EventWindowResponse | null>(null)

  // ── 로딩 / 에러 ───────────────────────────────────────────────────────────
  const loading = ref(false)
  const error   = ref<string | null>(null)

  // ── Actions ───────────────────────────────────────────────────────────────
  async function fetchEvents(): Promise<void> {
    try {
      const res    = await getEvents()
      events.value = res.detail.events
    } catch (e) {
      error.value = e instanceof Error ? e.message : '이벤트 목록 조회 실패'
    }
  }

  async function fetchEventWindow(): Promise<void> {
    if (selectedEventId.value === null) return
    loading.value = true
    error.value   = null
    try {
      const res            = await postEventWindow({
        event_id:      selectedEventId.value,
        window_months: windowMonths.value,
        region_codes:  regionCodes.value.length > 0 ? regionCodes.value : undefined,
      })
      analysisResult.value = res.detail
    } catch (e) {
      error.value = e instanceof Error ? e.message : '분석 요청 실패'
    } finally {
      loading.value = false
    }
  }

  function selectEvent(id: number): void {
    selectedEventId.value      = id
    currentRelativeMonth.value = 0
    void fetchEventWindow()
  }

  function setWindowMonths(w: 3 | 6 | 12): void {
    windowMonths.value = w
    void fetchEventWindow()
  }

  function setRegionCodes(codes: string[]): void {
    regionCodes.value = codes
  }

  return {
    // state
    events,
    selectedEventId,
    windowMonths,
    regionCodes,
    currentRelativeMonth,
    analysisResult,
    loading,
    error,
    // actions
    fetchEvents,
    fetchEventWindow,
    selectEvent,
    setWindowMonths,
    setRegionCodes,
  }
})
