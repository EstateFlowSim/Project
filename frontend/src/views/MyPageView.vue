<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppHeader from '@/components/common/AppHeader.vue'
import AppFooter from '@/components/common/AppFooter.vue'
import { useAuthStore } from '@/stores/authStore'
import { useReportStore } from '@/stores/reportStore'
import { useScenarioStore } from '@/stores/scenarioStore'
import { getMember, parseUserId, updateMember, changePassword, withdrawMember, type MemberResponse } from '@/api/authApi'

const router = useRouter()
const authStore = useAuthStore()
const reportStore = useReportStore()
const scenarioStore = useScenarioStore()

const member = ref<MemberResponse | null>(null)
const loading = ref(false)

// 프로필 편집 모달
const showEditModal = ref(false)
const editNick = ref('')
const editBirthDate = ref('')
const editSaving = ref(false)
const editError = ref('')

// 비밀번호 변경 모달
const showPwModal = ref(false)
const pwCur = ref('')
const pwNew = ref('')
const pwConfirm = ref('')
const pwSaving = ref(false)
const pwError = ref('')
const pwSuccess = ref(false)

// 탈퇴 모달
const showWithdrawModal = ref(false)
const withdrawing = ref(false)

onMounted(async () => {
  if (!authStore.isLoggedIn || !authStore.accessToken) {
    router.push('/login')
    return
  }

  loading.value = true
  try {
    const userId = parseUserId(authStore.accessToken)
    if (userId) {
      member.value = await getMember(userId, authStore.accessToken)
    }
  } finally {
    loading.value = false
  }
})

async function handleLogout() {
  await authStore.logout()
  router.push('/')
}

function openEditModal() {
  editNick.value = member.value?.nickname ?? authStore.nickname ?? ''
  editBirthDate.value = member.value?.birthDate ?? ''
  editError.value = ''
  showEditModal.value = true
}

async function saveProfile() {
  const trimmed = editNick.value.trim()
  if (!trimmed || trimmed.length < 2) { editError.value = '닉네임은 2자 이상이어야 합니다.'; return }
  if (trimmed.length > 20) { editError.value = '닉네임은 20자 이하여야 합니다.'; return }

  editSaving.value = true
  editError.value = ''
  try {
    const userId = member.value?.userId ?? parseUserId(authStore.accessToken ?? '')
    if (!userId) return
    await updateMember(userId, { nickname: trimmed, birthDate: editBirthDate.value || null })
    localStorage.setItem('nickname', trimmed)
    authStore.nickname = trimmed
    if (member.value) member.value = { ...member.value, nickname: trimmed, birthDate: editBirthDate.value || null }
    showEditModal.value = false
  } catch {
    editError.value = '프로필 수정에 실패했습니다.'
  } finally {
    editSaving.value = false
  }
}

function openPwModal() {
  pwCur.value = ''; pwNew.value = ''; pwConfirm.value = ''
  pwError.value = ''; pwSuccess.value = false
  showPwModal.value = true
}

async function savePassword() {
  if (!pwCur.value || !pwNew.value || !pwConfirm.value) { pwError.value = '모든 항목을 입력해주세요.'; return }
  if (pwNew.value.length < 8) { pwError.value = '새 비밀번호는 8자 이상이어야 합니다.'; return }
  if (pwNew.value !== pwConfirm.value) { pwError.value = '새 비밀번호가 일치하지 않습니다.'; return }

  pwSaving.value = true
  pwError.value = ''
  try {
    const userId = member.value?.userId ?? parseUserId(authStore.accessToken ?? '')
    if (!userId) return
    await changePassword(userId, pwCur.value, pwNew.value, pwConfirm.value)
    pwSuccess.value = true
  } catch {
    pwError.value = '비밀번호 변경에 실패했습니다. 현재 비밀번호를 확인해주세요.'
  } finally {
    pwSaving.value = false
  }
}

async function handleWithdraw() {
  withdrawing.value = true

  try {
    const userId = member.value?.userId ?? parseUserId(authStore.accessToken ?? '')
    if (!userId) return

    await withdrawMember(userId)
    await authStore.logout()
    router.push('/')
  } catch {
    showWithdrawModal.value = false
  } finally {
    withdrawing.value = false
  }
}

async function handlePersonaAnalysis() {
  const analysisCacheId = reportStore.getAnalysisCacheId()
  if (!analysisCacheId) return

  const scenario = await scenarioStore.createFromAnalysisCache(analysisCacheId)
  if (scenario) {
    router.push(`/scenarios/${scenario.scenario_id}`)
  }
}

function fmtDate(value: string) {
  return value?.slice(0, 10) ?? ''
}

function signed(value: number | undefined) {
  if (value === undefined) return '-'
  return `${value >= 0 ? '+' : ''}${value.toFixed(2)}%`
}
</script>

<template>
  <div class="mp">
    <AppHeader />

    <main class="mp-main">
      <div class="mp-inner">
        <section class="mp-section">
          <h2 class="mp-section-title">프로필</h2>

          <div v-if="loading" class="mp-loading">불러오는 중...</div>
          <div v-else class="mp-profile-card">
            <div class="mp-avatar">{{ (authStore.nickname ?? '?')[0] }}</div>

            <div class="mp-profile-info">
              <div class="mp-nick-row">
                <span class="mp-nick">{{ authStore.nickname }}</span>
              </div>
              <div class="mp-email">{{ member?.email ?? '-' }}</div>
              <div v-if="member?.birthDate" class="mp-birthdate">{{ member.birthDate }}</div>
            </div>

            <div class="mp-profile-actions">
              <button class="mp-nick-edit-btn" @click="openEditModal">프로필 수정</button>
              <button class="mp-nick-edit-btn" @click="openPwModal">비밀번호 변경</button>
            </div>
          </div>

          <div class="mp-withdraw-row">
            <button class="mp-withdraw-btn" @click="showWithdrawModal = true">회원 탈퇴</button>
          </div>

          <!-- 프로필 편집 모달 -->
          <Transition name="mp-modal">
            <div v-if="showEditModal" class="mp-modal-overlay" @click.self="showEditModal = false">
              <div class="mp-modal">
                <h3 class="mp-modal-title">프로필 수정</h3>

                <div class="mp-form">
                  <label class="mp-form-label">이메일</label>
                  <input class="mp-form-input mp-form-input--readonly" :value="member?.email ?? ''" readonly />

                  <label class="mp-form-label">닉네임</label>
                  <input
                    v-model="editNick"
                    class="mp-form-input"
                    maxlength="20"
                    placeholder="2~20자"
                    @keyup.enter="saveProfile"
                  />

                  <label class="mp-form-label">생년월일</label>
                  <input
                    v-model="editBirthDate"
                    class="mp-form-input"
                    type="date"
                  />
                </div>

                <span v-if="editError" class="mp-form-err">{{ editError }}</span>

                <div class="mp-modal-actions">
                  <button class="mp-modal-cancel" @click="showEditModal = false">취소</button>
                  <button class="mp-modal-confirm mp-modal-confirm--save" :disabled="editSaving" @click="saveProfile">
                    {{ editSaving ? '저장 중...' : '저장' }}
                  </button>
                </div>
              </div>
            </div>
          </Transition>

          <!-- 비밀번호 변경 모달 -->
          <Transition name="mp-modal">
            <div v-if="showPwModal" class="mp-modal-overlay" @click.self="showPwModal = false">
              <div class="mp-modal">
                <h3 class="mp-modal-title">비밀번호 변경</h3>

                <div v-if="pwSuccess" class="mp-pw-success">비밀번호가 변경되었습니다.</div>
                <div v-else class="mp-form">
                  <label class="mp-form-label">현재 비밀번호</label>
                  <input v-model="pwCur" class="mp-form-input" type="password" @keyup.enter="savePassword" />

                  <label class="mp-form-label">새 비밀번호</label>
                  <input v-model="pwNew" class="mp-form-input" type="password" placeholder="8자 이상" @keyup.enter="savePassword" />

                  <label class="mp-form-label">새 비밀번호 확인</label>
                  <input v-model="pwConfirm" class="mp-form-input" type="password" @keyup.enter="savePassword" />
                </div>

                <span v-if="pwError" class="mp-form-err">{{ pwError }}</span>

                <div class="mp-modal-actions">
                  <template v-if="pwSuccess">
                    <button class="mp-modal-confirm mp-modal-confirm--save" @click="showPwModal = false">확인</button>
                  </template>
                  <template v-else>
                    <button class="mp-modal-cancel" @click="showPwModal = false">취소</button>
                    <button class="mp-modal-confirm mp-modal-confirm--save" :disabled="pwSaving" @click="savePassword">
                      {{ pwSaving ? '변경 중...' : '변경' }}
                    </button>
                  </template>
                </div>
              </div>
            </div>
          </Transition>
        </section>

        <div v-if="showWithdrawModal" class="mp-modal-overlay" @click.self="showWithdrawModal = false">
          <div class="mp-modal">
            <h3 class="mp-modal-title">정말 탈퇴하시겠습니까?</h3>
            <p class="mp-modal-desc">
              탈퇴 후에는 다시 로그인할 수 없으며, 작성하신 데이터 일부는 탈퇴한 사용자 정보로만 남을 수 있습니다.
            </p>

            <div class="mp-modal-actions">
              <button class="mp-modal-cancel" @click="showWithdrawModal = false">취소</button>
              <button class="mp-modal-confirm" :disabled="withdrawing" @click="handleWithdraw">
                {{ withdrawing ? '처리 중...' : '탈퇴하기' }}
              </button>
            </div>
          </div>
        </div>

        <section class="mp-section">
          <div class="mp-section-hdr">
            <h2 class="mp-section-title">AI 리포트</h2>
          </div>

          <div v-if="reportStore.report" class="mp-report-card">
            <div class="mp-report-head">
              <div>
                <span class="mp-report-kicker">AI REPORT</span>
                <p class="mp-report-title">{{ reportStore.report.draft.title }}</p>
              </div>

              <span class="mp-report-status" :class="{ fallback: reportStore.report.status === 'DRAFT_FALLBACK' }">
                {{ reportStore.report.status === 'COMPLETED' ? '생성 완료' : '초안' }}
              </span>
            </div>

            <p class="mp-report-summary">
              {{ reportStore.report.ai_enhancement?.executive_summary?.trim() || reportStore.report.draft.overview }}
            </p>

            <div v-if="reportStore.report.analysis_result?.summary" class="mp-report-metrics">
              <div class="mp-metric">
                <span>분석 지역</span>
                <strong>{{ reportStore.report.analysis_result.summary.region_count ?? '-' }}곳</strong>
              </div>

              <div class="mp-metric">
                <span>상승 / 하락</span>
                <strong>
                  {{ reportStore.report.analysis_result.summary.rising_region_count ?? '-' }}
                  /
                  {{ reportStore.report.analysis_result.summary.falling_region_count ?? '-' }}
                </strong>
              </div>

              <div class="mp-metric">
                <span>평균 가격 변화</span>
                <strong :class="{ neg: (reportStore.report.analysis_result.summary.avg_price_change_after_window_pct ?? 0) < 0 }">
                  {{ signed(reportStore.report.analysis_result.summary.avg_price_change_after_window_pct) }}
                </strong>
              </div>

              <div class="mp-metric">
                <span>평균 거래량 변화</span>
                <strong :class="{ neg: (reportStore.report.analysis_result.summary.avg_volume_change_after_window_pct ?? 0) < 0 }">
                  {{ signed(reportStore.report.analysis_result.summary.avg_volume_change_after_window_pct) }}
                </strong>
              </div>
            </div>

            <div class="mp-report-meta">생성일 {{ fmtDate(reportStore.report.created_at) }}</div>

            <div class="mp-report-actions">
              <button
                class="mp-persona-btn"
                :disabled="scenarioStore.loading || !reportStore.getAnalysisCacheId()"
                @click="handlePersonaAnalysis"
              >
                {{ scenarioStore.loading ? '시장 참여자 관점 분석 준비 중...' : '시장 참여자 관점 분석' }}
              </button>

              <button class="mp-download-btn" :disabled="reportStore.loading" @click="reportStore.download()">
                {{ reportStore.loading ? 'PDF 준비 중...' : 'PDF 다운로드' }}
              </button>
            </div>

            <p v-if="!reportStore.getAnalysisCacheId()" class="mp-scenario-hint">
              이 리포트에는 분석 연결 정보가 없어 시장 참여자 관점 분석을 바로 시작할 수 없습니다.
            </p>
            <p v-if="scenarioStore.error" class="mp-scenario-hint err">{{ scenarioStore.error }}</p>
          </div>

          <div v-else class="mp-report-empty">
            <div class="mp-empty-icon">📄</div>
            <p>생성된 리포트가 없습니다.</p>
            <p class="mp-empty-sub">분석 페이지에서 이벤트를 선택하고 AI 리포트를 먼저 생성해 주세요.</p>
            <router-link to="/analysis" class="mp-go-analysis">분석 시작하기</router-link>
          </div>
        </section>
      </div>
    </main>

    <AppFooter />
  </div>
</template>

<style scoped>
.mp { min-height: 100vh; display: flex; flex-direction: column; background: #f8fafc; }
.mp-main { flex: 1; padding: 40px 24px; }
.mp-inner { max-width: 800px; margin: 0 auto; display: flex; flex-direction: column; gap: 32px; }

.mp-section { background: #fff; border-radius: 12px; box-shadow: 0 1px 3px rgba(0,0,0,.06); overflow: hidden; }
.mp-section-hdr { display: flex; justify-content: space-between; align-items: center; }
.mp-section-title { font-size: 16px; font-weight: 700; color: #1e293b; }

.mp-loading { padding: 28px; color: #94a3b8; font-size: 14px; }
.mp-profile-card { display: flex; align-items: center; gap: 16px; padding: 28px; }
.mp-avatar {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #1e293b;
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.mp-profile-info { flex: 1; }
.mp-nick { font-size: 18px; font-weight: 700; color: #1e293b; }
.mp-email { font-size: 14px; color: #64748b; margin-top: 2px; }
.mp-logout {
  padding: 7px 14px;
  border-radius: 6px;
  background: none;
  border: 1px solid #e2e8f0;
  color: #64748b;
  font-size: 14px;
  cursor: pointer;
  transition: all .15s;
  font-family: inherit;
}
.mp-logout:hover { border-color: #cbd5e1; color: #1e293b; }
.mp-nick-row { display: flex; align-items: center; gap: 8px; }
.mp-birthdate { font-size: 13px; color: #94a3b8; margin-top: 2px; }
.mp-profile-actions { display: flex; flex-direction: column; gap: 6px; margin-left: auto; }
.mp-nick-edit-btn {
  padding: 5px 12px;
  border: 1px solid #e2e8f0;
  background: none;
  color: #64748b;
  border-radius: 6px;
  font-size: 13px;
  cursor: pointer;
  transition: all .12s;
  font-family: inherit;
  white-space: nowrap;
}
.mp-nick-edit-btn:hover { border-color: #cbd5e1; color: #1e293b; }

/* 폼 */
.mp-form { display: flex; flex-direction: column; gap: 6px; margin-bottom: 4px; }
.mp-form-label { font-size: 12px; font-weight: 600; color: #64748b; margin-top: 8px; }
.mp-form-input {
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 14px;
  color: #1e293b;
  font-family: inherit;
  transition: border-color .12s;
}
.mp-form-input:focus { outline: none; border-color: #94a3b8; }
.mp-form-input--readonly { background: #f8fafc; color: #94a3b8; cursor: default; }
.mp-form-err { font-size: 12px; color: #ef4444; display: block; margin-bottom: 8px; }
.mp-pw-success { text-align: center; padding: 24px 0; font-size: 15px; font-weight: 600; color: #16a34a; }
.mp-modal-confirm--save { background: #1e293b; }
.mp-modal-confirm--save:hover:not(:disabled) { background: #334155; }

/* 모달 트랜지션 */
.mp-modal-enter-active, .mp-modal-leave-active { transition: opacity .15s; }
.mp-modal-enter-from, .mp-modal-leave-to { opacity: 0; }
.mp-modal-enter-active .mp-modal, .mp-modal-leave-active .mp-modal { transition: transform .15s; }
.mp-modal-enter-from .mp-modal, .mp-modal-leave-to .mp-modal { transform: scale(0.96) translateY(-6px); }
.mp-withdraw-row { padding: 12px 28px 20px; border-top: 1px solid #f1f5f9; }
.mp-withdraw-btn {
  background: none;
  border: none;
  color: #94a3b8;
  font-size: 12px;
  cursor: pointer;
  text-decoration: underline;
  font-family: inherit;
}
.mp-withdraw-btn:hover { color: #ef4444; }

.mp-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,.4);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}
.mp-modal {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  width: 100%;
  max-width: 400px;
  box-shadow: 0 20px 60px rgba(0,0,0,.15);
}
.mp-modal-title { font-size: 18px; font-weight: 700; color: #1e293b; margin-bottom: 12px; }
.mp-modal-desc { font-size: 14px; color: #64748b; line-height: 1.6; margin-bottom: 24px; }
.mp-modal-actions { display: flex; gap: 10px; justify-content: flex-end; }
.mp-modal-cancel {
  padding: 9px 18px;
  background: none;
  border: 1px solid #e2e8f0;
  color: #64748b;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  font-family: inherit;
}
.mp-modal-confirm {
  padding: 9px 18px;
  background: #ef4444;
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  font-family: inherit;
}
.mp-modal-confirm:disabled { opacity: .5; cursor: not-allowed; }

.mp-section > .mp-section-hdr { padding: 24px 28px 0; }
.mp-report-card { padding: 28px; }
.mp-report-head { display: flex; align-items: flex-start; justify-content: space-between; gap: 12px; margin-bottom: 12px; }
.mp-report-kicker { display: block; font-size: 10px; font-weight: 700; letter-spacing: 1.5px; color: #3b82f6; margin-bottom: 4px; }
.mp-report-title { font-size: 16px; font-weight: 600; color: #1e293b; line-height: 1.4; }
.mp-report-status {
  padding: 3px 9px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 600;
  background: #dcfce7;
  color: #16a34a;
  white-space: nowrap;
}
.mp-report-status.fallback { background: #fef9c3; color: #ca8a04; }
.mp-report-summary {
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
  margin-bottom: 16px;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
  overflow: hidden;
}
.mp-report-metrics {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 16px;
  background: #f8fafc;
  border-radius: 8px;
  margin-bottom: 16px;
}
.mp-metric { display: flex; flex-direction: column; gap: 3px; }
.mp-metric span { font-size: 11px; color: #94a3b8; }
.mp-metric strong { font-size: 15px; font-weight: 700; color: #1e293b; }
.mp-metric strong.neg { color: #ef4444; }
.mp-report-meta { font-size: 12px; color: #94a3b8; margin-bottom: 14px; }
.mp-report-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.mp-download-btn,
.mp-persona-btn {
  width: 100%;
  padding: 10px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background .15s;
  font-family: inherit;
}
.mp-download-btn { background: #1e293b; color: #fff; }
.mp-download-btn:hover:not(:disabled) { background: #334155; }
.mp-persona-btn { background: #3b82f6; color: #fff; }
.mp-persona-btn:hover:not(:disabled) { background: #2563eb; }
.mp-download-btn:disabled,
.mp-persona-btn:disabled { opacity: .5; cursor: not-allowed; }
.mp-scenario-hint { margin-top: 10px; font-size: 12px; color: #64748b; }
.mp-scenario-hint.err { color: #dc2626; }

.mp-report-empty { padding: 60px 28px; text-align: center; }
.mp-empty-icon { font-size: 40px; margin-bottom: 12px; }
.mp-report-empty p { font-size: 15px; color: #475569; font-weight: 500; }
.mp-empty-sub { font-size: 13px; color: #94a3b8; margin-top: 6px; font-weight: 400; }
.mp-go-analysis {
  display: inline-block;
  margin-top: 20px;
  padding: 10px 24px;
  background: #1e293b;
  color: #fff;
  border-radius: 8px;
  text-decoration: none;
  font-size: 14px;
  font-weight: 600;
  transition: background .15s;
}
.mp-go-analysis:hover { background: #334155; }

@media (max-width: 720px) {
  .mp-report-actions,
  .mp-report-metrics { grid-template-columns: 1fr; }
}
</style>
