<script setup lang="ts">
import { computed } from 'vue'
import type { RegionResult } from '@/types/analysis'
import { LAWD_CD_TO_NAME } from '@/constants/regionCodes'
import { valToColor } from '@/utils/colorScale'

const props = defineProps<{
  regions: RegionResult[]
  currentRelativeMonth: number
}>()


// 해당 상대월의 실제 가격 변화율
function monthlyChange(r: RegionResult): number {
  const md = r.monthly.find(m => m.relative_month === props.currentRelativeMonth)
  return md?.price_change_from_event_pct ?? 0
}

function isActive(r: RegionResult): boolean {
  const lag = r.window_summary.lag_months
  return lag !== null && props.currentRelativeMonth >= lag
}

function regionName(r: RegionResult): string {
  return LAWD_CD_TO_NAME[r.dong_code] ?? r.dong_code
}

const activeCount = computed(() => props.regions.filter(isActive).length)

const maxRegion = computed(() =>
  props.regions.length === 0
    ? null
    : props.regions.reduce((mx, r) => monthlyChange(r) > monthlyChange(mx) ? r : mx)
)

const maxChange = computed(() => maxRegion.value ? monthlyChange(maxRegion.value) : 0)

// 시차 짧은 순 (빠른 반응 구부터)
const top5 = computed(() =>
  [...props.regions]
    .sort((a, b) => (a.window_summary.lag_months ?? 99) - (b.window_summary.lag_months ?? 99))
    .slice(0, 5)
)

const top5Max = computed(() =>
  Math.max(...top5.value.map(r => Math.abs(monthlyChange(r))), 0.1)
)
</script>

<template>
  <div class="rp">
    <div class="mc">
      <div class="mc-l">현재 최대 변화율</div>
      <div class="mc-v" :style="{ color: maxChange >= 0 ? '#ff4d6d' : '#4d9fff' }">
        {{ maxChange >= 0 ? '+' : '' }}{{ maxChange.toFixed(1) }}%
      </div>
      <div class="mc-s">{{ maxRegion ? regionName(maxRegion) : '—' }} 기준</div>
    </div>
    <div class="mc">
      <div class="mc-l">반응 지역 수</div>
      <div class="mc-v" style="color:rgba(255,255,255,.75)">
        {{ activeCount }}<span style="font-size:11px;color:var(--w3)"> / {{ regions.length }}</span>
      </div>
      <div class="mc-s">현재 시점 기준</div>
    </div>
    <div class="leg">
      <div class="mc-l">변화율</div>
      <div class="leg-r"><div class="leg-sw" style="background:#ff1133"></div>+15% 이상</div>
      <div class="leg-r"><div class="leg-sw" style="background:#ff6622"></div>+10~15%</div>
      <div class="leg-r"><div class="leg-sw" style="background:#ffcc22"></div>+5~10%</div>
      <div class="leg-r"><div class="leg-sw" style="background:#334466"></div>±5%</div>
      <div class="leg-r"><div class="leg-sw" style="background:#2255cc"></div>-5% 이하</div>
    </div>
    <div class="tp">
      <div class="tp-t">빠른 반응 TOP 5</div>
      <template v-if="regions.length > 0">
        <div v-for="r in top5" :key="r.dong_code" class="tp-row">
          <div class="tp-nm">{{ regionName(r) }}</div>
          <div class="tp-right">
            <div class="tp-bar">
              <div
                class="tp-bf"
                :style="{
                  width: (Math.abs(monthlyChange(r)) / top5Max * 100).toFixed(0) + '%',
                  background: valToColor(monthlyChange(r)),
                }"
              ></div>
            </div>
            <div class="tp-pct" :style="{ color: valToColor(monthlyChange(r)) }">
              {{ monthlyChange(r) >= 0 ? '+' : '' }}{{ monthlyChange(r).toFixed(1) }}%
            </div>
          </div>
        </div>
      </template>
      <div v-else class="tp-empty">데이터 로딩 중...</div>
    </div>
  </div>
</template>
