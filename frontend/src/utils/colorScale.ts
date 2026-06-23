export function valToColor(v: number): string {
  if (v >= 15) return '#ff1133'
  if (v >= 10) return '#ff6622'
  if (v >= 5)  return '#ffcc22'
  if (v >= 0)  return '#334466'
  if (v >= -5) return '#2255cc'
  return '#1133aa'
}
