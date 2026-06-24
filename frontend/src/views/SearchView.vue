<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import AppFooter from '@/components/common/AppFooter.vue'
import { getAptDeals, type AptDeal } from '@/api/dealsApi'
import { LAWD_CD_TO_NAME } from '@/constants/regionCodes'

const YEARS  = Array.from({ length: 15 }, (_, i) => 2026 - i)
const MONTHS = Array.from({ length: 12 }, (_, i) => i + 1)
const PAGE_SIZE = 15

const route      = useRoute()
const regionCode = ref('11680')
const year       = ref(2026)
const month      = ref(new Date().getMonth() + 1)
const deals      = ref<AptDeal[]>([])
const loading    = ref(false)
const error      = ref<string | null>(null)
const page       = ref(1)

const yearMonth = computed(() =>
  `${year.value}${String(month.value).padStart(2, '0')}`,
)

async function search() {
  loading.value = true
  error.value   = null
  deals.value   = []
  page.value    = 1
  try {
    const res   = await getAptDeals(regionCode.value, yearMonth.value)
    deals.value = res.detail ?? []
  } catch (e: any) {
    error.value = e?.response?.data?.message ?? '조회 중 오류가 발생했습니다.'
  } finally {
    loading.value = false
  }
}

function fmtAmount(v: number) { return v.toLocaleString() + '만원' }
function fmtDate(d: AptDeal)  { return `${d.dealYear}.${String(d.dealMonth).padStart(2, '0')}.${String(d.dealDay).padStart(2, '0')}` }

const count   = computed(() => deals.value.length)
const avgAmt  = computed(() => count.value ? Math.round(deals.value.reduce((s, d) => s + d.dealAmount, 0) / count.value) : 0)
const maxAmt  = computed(() => count.value ? Math.max(...deals.value.map(d => d.dealAmount)) : 0)
const minAmt  = computed(() => count.value ? Math.min(...deals.value.map(d => d.dealAmount)) : 0)

const sortKey  = ref<keyof AptDeal>('dealAmount')
const sortDesc = ref(true)

function toggleSort(key: keyof AptDeal) {
  if (sortKey.value === key) sortDesc.value = !sortDesc.value
  else { sortKey.value = key; sortDesc.value = true }
  page.value = 1
}

const sorted = computed(() => {
  const arr = [...deals.value]
  arr.sort((a, b) => {
    const va = a[sortKey.value] as number | string
    const vb = b[sortKey.value] as number | string
    return sortDesc.value ? (va < vb ? 1 : -1) : (va > vb ? 1 : -1)
  })
  return arr
})

const totalPages = computed(() => Math.max(1, Math.ceil(sorted.value.length / PAGE_SIZE)))

const paddedRows = computed<(AptDeal | null)[]>(() => {
  const start = (page.value - 1) * PAGE_SIZE
  const rows: (AptDeal | null)[] = sorted.value.slice(start, start + PAGE_SIZE)
  while (rows.length < PAGE_SIZE) rows.push(null)
  return rows
})

const pageSlots = computed(() => {
  const cur = page.value
  return [cur - 2, cur - 1, cur, cur + 1, cur + 2]
})

function goTo(p: number) {
  page.value = Math.max(1, Math.min(totalPages.value, p))
}

onMounted(() => {
  const rc = route.query.regionCode as string | undefined
  const ym = route.query.yearMonth  as string | undefined
  if (rc && ym && ym.length === 6) {
    regionCode.value = rc
    year.value  = parseInt(ym.slice(0, 4))
    month.value = parseInt(ym.slice(4, 6))
    search()
  }
})
</script>

<template>
  <div class="sv">
    <AppHeader />

    <main class="sv-main">
      <div class="sv-inner">
        <h1 class="sv-title">아파트 매매 거래 조회</h1>

        <div class="sv-filter">
          <select v-model="regionCode" class="sv-sel">
            <option v-for="(name, code) in LAWD_CD_TO_NAME" :key="code" :value="code">{{ name }}</option>
          </select>
          <select v-model="year" class="sv-sel">
            <option v-for="y in YEARS" :key="y" :value="y">{{ y }}년</option>
          </select>
          <select v-model="month" class="sv-sel">
            <option v-for="m in MONTHS" :key="m" :value="m">{{ m }}월</option>
          </select>
          <button class="sv-btn" :disabled="loading" @click="search">
            {{ loading ? '조회 중...' : '조회' }}
          </button>
        </div>

        <p v-if="error" class="sv-err">{{ error }}</p>

        <!-- 통계 요약 (항상 표시) -->
        <div class="sv-stats">
          <div class="sv-stat">
            <span class="sv-stat-label">조회 건수</span>
            <strong class="sv-stat-value">{{ count }}건</strong>
          </div>
          <div class="sv-stat">
            <span class="sv-stat-label">평균 거래가</span>
            <strong class="sv-stat-value">{{ fmtAmount(avgAmt) }}</strong>
          </div>
          <div class="sv-stat">
            <span class="sv-stat-label">최고가</span>
            <strong class="sv-stat-value sv-stat-high">{{ fmtAmount(maxAmt) }}</strong>
          </div>
          <div class="sv-stat">
            <span class="sv-stat-label">최저가</span>
            <strong class="sv-stat-value sv-stat-low">{{ fmtAmount(minAmt) }}</strong>
          </div>
        </div>

        <!-- 테이블 (항상 표시) -->
        <div class="sv-table-wrap">
          <table class="sv-table">
            <thead>
              <tr>
                <th @click="toggleSort('aptName')">아파트명</th>
                <th @click="toggleSort('dong')">법정동</th>
                <th @click="toggleSort('dealAmount')">거래금액 {{ sortKey === 'dealAmount' ? (sortDesc ? '▼' : '▲') : '' }}</th>
                <th @click="toggleSort('area')">면적(㎡) {{ sortKey === 'area' ? (sortDesc ? '▼' : '▲') : '' }}</th>
                <th @click="toggleSort('floor')">층</th>
                <th @click="toggleSort('dealYear')">거래일</th>
                <th @click="toggleSort('buildYear')">건축년도</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(d, i) in paddedRows" :key="i" :class="{ 'sv-row-empty': !d }">
                <td>{{ d?.aptName ?? '' }}</td>
                <td>{{ d?.dong ?? '' }}</td>
                <td class="sv-amt">{{ d ? fmtAmount(d.dealAmount) : '' }}</td>
                <td>{{ d?.area ?? '' }}</td>
                <td>{{ d?.floor ?? '' }}</td>
                <td>{{ d ? fmtDate(d) : '' }}</td>
                <td>{{ d?.buildYear ?? '' }}</td>
              </tr>
            </tbody>
          </table>

          <!-- 페이지네이션 (항상 표시) -->
          <div class="sv-pager">
            <div class="sv-pager-side">
              <button class="sv-pg-btn" :disabled="page === 1" @click="goTo(1)">|&lt;</button>
            </div>
            <div class="sv-pager-nums">
              <button
                v-for="(n, i) in pageSlots" :key="i"
                class="sv-pg-btn"
                :class="{ 'sv-pg-active': n === page, 'sv-pg-ghost': n < 1 || n > totalPages }"
                :disabled="n < 1 || n > totalPages"
                @click="goTo(n)">{{ n >= 1 && n <= totalPages ? n : '' }}</button>
            </div>
            <div class="sv-pager-side">
              <button class="sv-pg-btn" :disabled="page === totalPages" @click="goTo(totalPages)">&gt;|</button>
            </div>
          </div>
        </div>

      </div>
    </main>
    <AppFooter />
  </div>
</template>

<style scoped>
.sv { min-height: 100vh; display: flex; flex-direction: column; background: #f8fafc; }

.sv-main { flex: 1; padding: 40px 24px; }
.sv-inner { max-width: 1100px; margin: 0 auto; }
.sv-title { font-size: 22px; font-weight: 700; color: #1e293b; margin-bottom: 24px; }

.sv-filter { display: flex; gap: 10px; margin-bottom: 20px; flex-wrap: wrap; }
.sv-sel { background: #fff; border: 1px solid #e2e8f0; color: #1e293b; padding: 8px 12px; border-radius: 6px; font-size: 14px; cursor: pointer; font-family: inherit; }
.sv-sel:focus { outline: none; border-color: #94a3b8; }
.sv-btn { background: #1e293b; border: none; color: #fff; padding: 8px 24px; border-radius: 6px; font-size: 14px; cursor: pointer; font-weight: 500; transition: background .15s; font-family: inherit; }
.sv-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.sv-btn:hover:not(:disabled) { background: #334155; }

.sv-err { color: #ef4444; font-size: 13px; margin-bottom: 12px; }

/* 통계 */
.sv-stats { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 16px; }
.sv-stat { background: #fff; border-radius: 10px; padding: 16px 20px; box-shadow: 0 1px 3px rgba(0,0,0,.06); }
.sv-stat-label { display: block; font-size: 11px; color: #94a3b8; font-weight: 500; margin-bottom: 4px; }
.sv-stat-value { font-size: 16px; font-weight: 700; color: #1e293b; }
.sv-stat-high { color: #3b82f6; }
.sv-stat-low { color: #64748b; }

/* 테이블 */
.sv-table-wrap { overflow-x: auto; background: #fff; border-radius: 10px; box-shadow: 0 1px 3px rgba(0,0,0,.06); }
.sv-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.sv-table th { background: #f8fafc; padding: 12px 16px; text-align: left; color: #64748b; font-weight: 600; font-size: 13px; border-bottom: 1px solid #e2e8f0; cursor: pointer; user-select: none; white-space: nowrap; }
.sv-table th:hover { color: #1e293b; }
.sv-table td { padding: 0 16px; height: 49px; border-bottom: 1px solid #f1f5f9; color: #334155; vertical-align: middle; }
.sv-table tr:last-child td { border-bottom: none; }
.sv-table tr:hover:not(.sv-row-empty) td { background: #f8fafc; }
.sv-row-empty td { border-bottom-color: transparent; pointer-events: none; }
.sv-amt { color: #3b82f6; font-weight: 500; }

/* 페이지네이션 */
.sv-pager { display: flex; justify-content: center; align-items: center; padding: 16px 0; }
.sv-pager-side { width: 40px; display: flex; justify-content: center; flex-shrink: 0; }
.sv-pager-nums { width: 180px; display: flex; justify-content: center; gap: 2px; flex-shrink: 0; }
.sv-pg-btn { min-width: 32px; height: 32px; padding: 0 8px; border: none; background: none; color: #94a3b8; border-radius: 6px; font-size: 13px; cursor: pointer; transition: color .15s; }
.sv-pg-btn:hover:not(:disabled) { color: #334155; }
.sv-pg-btn:disabled { cursor: default; }
.sv-pg-active { color: #1e293b !important; font-weight: 700; }
.sv-pg-ghost { visibility: hidden; pointer-events: none; }
</style>
