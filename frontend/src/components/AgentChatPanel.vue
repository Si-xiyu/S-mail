<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, nextTick, ref, watch } from 'vue'
import { useMailStore } from '../stores/mailStore'
import type { AgentMessageView, PendingAgentAction } from '../types/mail'

const props = defineProps<{
  scope: 'GLOBAL' | 'CURRENT_MAIL'
  itemId?: number | null
  title?: string
  placeholder?: string
}>()

const emit = defineEmits<{
  actionConfirmed: []
}>()

const mailStore = useMailStore()

const sessionId = ref<string | null>(null)
const messages = ref<AgentMessageView[]>([])
const pendingActions = ref<PendingAgentAction[]>([])
const input = ref('')
const loading = ref(false)
const initLoading = ref(false)
const messagesRef = ref<HTMLDivElement | null>(null)

const aiEnabled = computed(() => mailStore.aiEnabled)
const isCurrentMail = computed(() => props.scope === 'CURRENT_MAIL')

const displayTitle = computed(() => {
  if (props.title) return props.title
  return isCurrentMail.value ? '当前邮件助手' : 'SmartMail 助手'
})

const displayPlaceholder = computed(() => {
  if (props.placeholder) return props.placeholder
  if (isCurrentMail.value) {
    return '询问这封邮件…'
  }
  return '询问邮箱事务，例如搜索、整理邮件…'
})

const scrollToBottom = async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

const loadSession = async () => {
  if (initLoading.value) return
  initLoading.value = true
  try {
    const context: Record<string, unknown> = {}
    if (isCurrentMail.value && props.itemId) {
      context.mailItemId = props.itemId
    }
    const session = await mailStore.createAgentSession(props.scope, context)
    sessionId.value = session.sessionId
    messages.value = session.messages || []
    pendingActions.value = session.pendingActions || []
    await scrollToBottom()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '加载 Agent 会话失败')
  } finally {
    initLoading.value = false
  }
}

const refreshSession = async () => {
  if (!sessionId.value) return
  try {
    const session = await mailStore.getAgentSession(sessionId.value)
    messages.value = session.messages || []
    pendingActions.value = session.pendingActions || []
    await scrollToBottom()
  } catch (err) {
    console.error('Failed to refresh agent session:', err)
  }
}

const sendMessage = async () => {
  const text = input.value.trim()
  if (!text || !sessionId.value || loading.value) return

  messages.value.push({ role: 'USER', content: text, status: 'SUCCEEDED' })
  input.value = ''
  loading.value = true
  await scrollToBottom()

  try {
    const response = await mailStore.sendAgentMessage(sessionId.value, text)
    messages.value.push(response.assistantMessage)
    pendingActions.value = response.pendingActions || []
    if (response.status === 'DISABLED') {
      // 后端已禁用 AI，刷新用户设置以同步状态
      if (mailStore.userSettings) {
        mailStore.userSettings.aiEnabled = false
      } else {
        mailStore.userSettings = { aiEnabled: false, agentAutoWriteEnabled: false }
      }
    }
    await scrollToBottom()
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '发送消息失败')
    messages.value.push({
      role: 'ASSISTANT',
      content: '消息发送失败，请稍后重试。',
      status: 'FAILED'
    })
    await scrollToBottom()
  } finally {
    loading.value = false
  }
}

const handleAction = async (action: PendingAgentAction, confirmed: boolean) => {
  try {
    const result = await mailStore.confirmAgentAction(action.actionId, confirmed)
    action.status = result.status
    ElMessage.success(confirmed ? '动作已执行' : '动作已取消')
    // 刷新会话，更新动作状态和历史
    await refreshSession()
    if (confirmed) {
      emit('actionConfirmed')
    }
  } catch (err) {
    ElMessage.error(err instanceof Error ? err.message : '操作失败')
  }
}

const formatTime = (value?: string) => {
  if (!value) return ''
  return new Date(value).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

watch(() => [props.scope, props.itemId], () => {
  sessionId.value = null
  messages.value = []
  pendingActions.value = []
  void loadSession()
}, { immediate: true })
</script>

<template>
  <div class="agent-panel">
    <header class="agent-header">
      <span class="agent-icon">✦</span>
      <span class="agent-title">{{ displayTitle }}</span>
    </header>

    <div v-if="!aiEnabled" class="ai-disabled-banner">
      <span class="banner-icon">🌙</span>
      <p>AI 功能已关闭，基础邮箱可用</p>
    </div>

    <div ref="messagesRef" class="messages">
      <div v-if="initLoading" class="state">正在启动助手…</div>
      <template v-else>
        <div
          v-for="(message, index) in messages"
          :key="index"
          class="message"
          :class="{ user: message.role === 'USER', assistant: message.role === 'ASSISTANT' }"
        >
          <div class="bubble">
            <p>{{ message.content }}</p>
            <time v-if="message.createdAt">{{ formatTime(message.createdAt) }}</time>
          </div>
        </div>

        <div v-if="loading" class="message assistant">
          <div class="bubble typing">
            <span></span><span></span><span></span>
          </div>
        </div>

        <div v-if="pendingActions.length" class="pending-actions">
          <div class="actions-title">待确认动作</div>
          <div
            v-for="action in pendingActions"
            :key="action.actionId"
            class="action-card"
            :class="{ executed: action.status === 'EXECUTED', cancelled: action.status === 'CANCELLED' }"
          >
            <div class="action-label">{{ action.label }}</div>
            <div v-if="action.reason" class="action-reason">{{ action.reason }}</div>
            <div v-if="action.status === 'PENDING'" class="action-buttons">
              <button type="button" @click="handleAction(action, true)">确认</button>
              <button type="button" class="secondary" @click="handleAction(action, false)">取消</button>
            </div>
            <div v-else class="action-status">
              {{ action.status === 'EXECUTED' ? '已执行' : '已取消' }}
            </div>
          </div>
        </div>
      </template>
    </div>

    <div class="input-area">
      <input
        v-model="input"
        type="text"
        :placeholder="displayPlaceholder"
        :disabled="loading || !sessionId || !aiEnabled"
        @keyup.enter="sendMessage"
      />
      <button
        type="button"
        :disabled="loading || !input.trim() || !sessionId || !aiEnabled"
        @click="sendMessage"
      >
        发送
      </button>
    </div>
  </div>
</template>

<style scoped>
.agent-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fbfbfa;
  border-left: 1px solid #e0e0e0;
}

.agent-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
  background: #fff;
}

.agent-icon {
  width: 26px;
  height: 26px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: #37352f;
  color: #fff;
  font-size: 13px;
}

.agent-title {
  font-size: 14px;
  font-weight: 600;
  color: #37352f;
}

.ai-disabled-banner {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  background: #fefce8;
  border-bottom: 1px solid #fde68a;
  color: #854d0e;
  font-size: 12px;
}

.ai-disabled-banner p {
  margin: 0;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.state {
  margin: auto;
  color: rgba(55, 53, 47, 0.45);
  font-size: 13px;
}

.message {
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.bubble {
  max-width: 88%;
  padding: 10px 13px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.55;
  color: #37352f;
  background: #fff;
  border: 1px solid #e8e8e8;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.02);
}

.message.user .bubble {
  background: #37352f;
  color: #fff;
  border-color: #37352f;
}

.bubble p {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.bubble time {
  display: block;
  margin-top: 6px;
  font-size: 10px;
  opacity: 0.5;
}

.typing {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 14px 12px;
}

.typing span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: rgba(55, 53, 47, 0.35);
  animation: bounce 1.2s infinite ease-in-out;
}

.typing span:nth-child(2) {
  animation-delay: 0.15s;
}

.typing span:nth-child(3) {
  animation-delay: 0.3s;
}

@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
  40% { transform: scale(1.2); opacity: 1; }
}

.pending-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.actions-title {
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: rgba(55, 53, 47, 0.45);
}

.action-card {
  padding: 12px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #e0e0e0;
  font-size: 13px;
}

.action-card.executed {
  border-color: #86efac;
  background: #f0fdf4;
}

.action-card.cancelled {
  border-color: #d1d5db;
  background: #f3f4f6;
  opacity: 0.75;
}

.action-label {
  color: #37352f;
  font-weight: 500;
}

.action-reason {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(55, 53, 47, 0.55);
}

.action-buttons {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.action-buttons button {
  flex: 1;
  padding: 6px 12px;
  border: none;
  border-radius: 4px;
  background: #37352f;
  color: #fff;
  font-size: 12px;
  cursor: pointer;
  transition: background 0.1s;
}

.action-buttons button:hover:not(:disabled) {
  background: #2b2925;
}

.action-buttons button.secondary {
  background: #fff;
  color: #37352f;
  border: 1px solid #e0e0e0;
}

.action-buttons button.secondary:hover:not(:disabled) {
  background: #f4f4f4;
}

.action-status {
  margin-top: 8px;
  font-size: 12px;
  color: rgba(55, 53, 47, 0.5);
}

.input-area {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #e0e0e0;
  background: #fff;
}

.input-area input {
  flex: 1;
  padding: 9px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  font-size: 13px;
  color: #37352f;
  outline: none;
  background: #fbfbfa;
}

.input-area input:focus {
  border-color: #37352f;
  background: #fff;
}

.input-area input:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.input-area button {
  padding: 9px 16px;
  border: none;
  border-radius: 6px;
  background: #37352f;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: background 0.1s;
}

.input-area button:hover:not(:disabled) {
  background: #2b2925;
}

.input-area button:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}
</style>
