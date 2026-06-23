<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { MapEvent } from '@/types/analysis'

const props = defineProps<{
  events: MapEvent[]
  modelValue: number
}>()
const emit = defineEmits<{ 'update:modelValue': [index: number] }>()

// events에서 연도 목록 추출 (정렬)
const years = computed(() => {
  const ys = [...new Set(props.events.map(ev => ev.short.slice(0, 4)))]
  return ys.sort()
})

const fromYear   = ref('')
const toYear     = ref('')
const activeYear = ref('')

// 이벤트 목록 최초 로드 시 초기화
watch(years, ys => {
  if (ys.length === 0) return
  if (!fromYear.value)   fromYear.value   = ys[0]                  ?? ''
  if (!toYear.value)     toYear.value     = ys[ys.length - 1]      ?? ''
  if (!activeYear.value) activeYear.value = ys[0]                  ?? ''
}, { immediate: true })

// fromYear 변경 시 toYear가 더 작아지면 맞춤
watch(fromYear, fy => { if (fy > toYear.value) toYear.value = fy })

// toYear 옵션: fromYear 이상만
const toYearOptions = computed(() => years.value.filter(y => y >= fromYear.value))

// 범위 내 연도 탭
const filteredYears = computed(() =>
  years.value.filter(y => y >= fromYear.value && y <= toYear.value)
)

// activeYear가 범위를 벗어나면 첫 연도로 이동
watch(filteredYears, ys => {
  if (ys.length > 0 && !ys.includes(activeYear.value)) {
    activeYear.value = ys[0] ?? ''
  }
})

// 선택된 이벤트가 바뀌면 해당 연도 탭으로 이동
watch(() => props.modelValue, idx => {
  const ev = props.events[idx]
  if (ev) activeYear.value = ev.short.slice(0, 4)
})

// activeYear의 이벤트 목록 (원본 인덱스 보존)
const yearEvents = computed(() =>
  props.events
    .map((ev, i) => ({ ev, i }))
    .filter(({ ev }) => ev.short.slice(0, 4) === activeYear.value)
)
</script>

<template>
  <div class="evc">
    <!-- 연도 범위 필터 -->
    <div class="ef">
      <span class="ef-lbl">기간</span>
      <select class="efy-sel" v-model="fromYear">
        <option v-for="y in years" :key="y" :value="y">{{ y }}</option>
      </select>
      <span class="ef-dash">—</span>
      <select class="efy-sel" v-model="toYear">
        <option v-for="y in toYearOptions" :key="y" :value="y">{{ y }}</option>
      </select>
    </div>
    <!-- 연도 탭 -->
    <div class="efy">
      <button
        v-for="y in filteredYears"
        :key="y"
        class="eyt"
        :class="{ on: y === activeYear }"
        @click="activeYear = y"
      >{{ y }}</button>
    </div>
    <!-- 해당 연도 이벤트 버튼 -->
    <div class="evs">
      <button
        v-for="{ ev, i } in yearEvents"
        :key="ev.id"
        class="evb"
        :class="{ on: i === modelValue }"
        @click="emit('update:modelValue', i)"
      >{{ ev.short.slice(5) }} {{ ev.name }}</button>
    </div>
  </div>
</template>
