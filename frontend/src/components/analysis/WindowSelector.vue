<script setup lang="ts">
defineProps<{ modelValue: 3 | 6 | 12 }>()
const emit = defineEmits<{ 'update:modelValue': [w: 3 | 6 | 12] }>()
const options = [3, 6, 12] as const
</script>

<template>
  <div class="wsel">
    <span class="wsel-lbl">윈도우</span>
    <button
      v-for="w in options"
      :key="w"
      class="wb"
      :class="{ on: modelValue === w }"
      @click="emit('update:modelValue', w)"
    >{{ w }}M</button>
  </div>
</template>
