<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { computed, ref, watch } from 'vue'
import * as apiClient from '../api/client'
import { useMailStore } from '../stores/mailStore'
import type { MailDetail, ThreadMessage } from '../types/mail'

const props = defineProps<{ mailId?: string | null; inline?: boolean; showBack?: boolean }>()
const emit = defineEmits<{ close: [] }>()
const mailStore = useMailStore()

const detail = ref<MailDetail | null>(null)
const thread = ref<ThreadMessage[]>([])
const loading = ref(false)
const replyMode = ref<'none' | 'reply' | 'forward'>('none')
const replyTo = ref('')
const replyContent = ref('')
const sending = ref(false)

const customLabels = computed(() => mailStore.labels.filter(label => /^\d+$/.test(label.id)))
const isTrash = computed(() => detail.value?.folder === 'TRASH')
const aiSummaryLines = computed(() => extractAiSummary(detail.value))
const aiStatus = computed(() => extractAiStatus(detail.value))
const showAiSummaryCard = computed(() => mailStore.aiEnabled && (aiSummaryLines.value.length > 0 || aiStatus.value !== 'DISABLED'))

const extractAiSummary = (mail: MailDetail | null): string[] => {
  if (!mail?.aiResults?.length) return []
  for (const result of mail.aiResults) {
    const type = String(result.type || '').toUpperCase()
    if (type !== 'SUMMARY' && type !== 'ANALYSIS') continue
    const raw = result.resultJson
    let parsed: unknown = raw
    if (typeof raw === 'string') {
      try {
        parsed = JSON.parse(raw)
      } catch {
        continue
      }
    }
    if (parsed && typeof parsed === 'object' && Array.isArray((parsed as { summary?: unknown }).summary)) {
      return (parsed as { summary: unknown[] }).summary
        .map(value => String(value).trim())
        .filter(Boolean)
        .slice(0, 3)
    }
  }
  return []
}

const extractAiStatus = (mail: MailDetail | null): string => {
  if (!mailStore.aiEnabled) return 'DISABLED'
  if (!mail?.aiResults?.length) return 'PENDING'
  return String(mail.aiResults[0].status || 'SUCCEEDED').toUpperCase()
}

const aiStatusText = (status: string): string => {
  if (status === 'SUCCEEDED') return '已生成'
  if (status === 'FAILED') return '生成失败'
  return '生成中'
}

const aiEmptyText = (status: string): string => {
  if (status === 'FAILED') return '本邮件摘要生成失败，不影响正文阅读。'
  return '摘要正在生成或暂未生成，可稍后刷新查看。'
}

const load = async () => {
  if (!props.mailId) {
    detail.value = null
    thread.value = []
    return
  }
  loading.value = true
  try {
    const mailId = Number(props.mailId)
    detail.value = await mailStore.getMailDetail(mailId)
    thread.value = await mailStore.getMailThread(mailId, true)
    if (detail.value && !detail.value.read) {
      await mailStore.markMailRead(detail.value.itemId, true)
      detail.value.read = true
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载邮件失败')
  } finally {
    loading.value = false
  }
}

watch(() => props.mailId, load, { immediate: true })

const startReply = () => {
  if (!detail.value) return
  replyMode.value = 'reply'
  replyTo.value = detail.value.senderEmail === mailStore.user?.email
    ? detail.value.recipients[0] || ''
    : detail.value.senderEmail
  replyContent.value = ''
}

const startForward = () => {
  if (!detail.value) return
  replyMode.value = 'forward'
  replyTo.value = ''
  replyContent.value = `\n\n---------- 转发邮件 ----------\n发件人：${detail.value.senderEmail}\n主题：${detail.value.subject}\n\n${detail.value.contentText}`
}

const sendReply = async () => {
  if (!detail.value || !replyTo.value.trim() || !replyContent.value.trim()) {
    ElMessage.warning('请填写收件人和正文')
    return
  }
  sending.value = true
  try {
    const isReply = replyMode.value === 'reply'
    await mailStore.sendMessage({
      to: replyTo.value.split(',').map(value => value.trim()).filter(Boolean),
      subject: isReply && !detail.value.subject.startsWith('Re:')
        ? `Re: ${detail.value.subject}`
        : isReply ? detail.value.subject : `Fwd: ${detail.value.subject}`,
      contentText: replyContent.value,
      parentMailId: isReply ? detail.value.mailId : undefined
    })
    if (isReply) {
      thread.value = await mailStore.getMailThread(detail.value.mailId, true)
    }
    replyMode.value = 'none'
    replyContent.value = ''
    ElMessage.success(isReply ? '回复已发送' : '转发已发送')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '发送失败')
  } finally {
    sending.value = false
  }
}

const toggleStar = async () => {
  if (!detail.value) return
  try {
    await mailStore.starMail(detail.value.itemId, !detail.value.starred)
    detail.value.starred = !detail.value.starred
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新星标失败')
  }
}

const deleteMail = async () => {
  if (!detail.value) return
  try {
    await mailStore.deleteMailByItemId(detail.value.itemId)
    await mailStore.refreshCurrent()
    emit('close')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除邮件失败')
  }
}

const restoreMail = async () => {
  if (!detail.value) return
  try {
    await mailStore.moveMail(detail.value.itemId, 'RESTORE')
    await mailStore.refreshCurrent()
    emit('close')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '恢复邮件失败')
  }
}

const moveToJunk = async () => {
  if (!detail.value) return
  try {
    await mailStore.moveMail(detail.value.itemId, 'JUNK')
    await mailStore.refreshCurrent()
    emit('close')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '移动邮件失败')
  }
}

const assignLabel = async (event: Event) => {
  if (!detail.value) return
  const categoryId = (event.target as HTMLSelectElement).value
  if (!categoryId) return
  try {
    await mailStore.changeCategoryByItemId(detail.value.itemId, categoryId)
    ElMessage.success('标签已更新')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新标签失败')
  }
}

const downloadAttachment = async (attachmentId: number, fileName: string) => {
  try {
    await apiClient.downloadAttachment(attachmentId, fileName)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '下载附件失败')
  }
}
</script>

<template>
  <!-- Inline mode: rendered as a normal block panel -->
  <div v-if="inline && mailId" class="inline-panel">
    <header class="drawer-header">
      <button v-if="showBack" class="back-btn" type="button" @click="emit('close')">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="19" y1="12" x2="5" y2="12" />
          <polyline points="12 19 5 12 12 5" />
        </svg>
        <span>返回邮件列表</span>
      </button>
      <div v-if="detail" class="header-actions">
        <button type="button" @click="toggleStar">{{ detail.starred ? '★' : '☆' }}</button>
        <select aria-label="添加标签" @change="assignLabel">
          <option value="">标签</option>
          <option v-for="label in customLabels" :key="label.id" :value="label.id">{{ label.name }}</option>
        </select>
        <button v-if="isTrash" type="button" @click="restoreMail">恢复</button>
        <button v-else type="button" @click="moveToJunk">移至 Junk</button>
        <button type="button" @click="deleteMail">{{ isTrash ? '永久删除' : '删除' }}</button>
      </div>
    </header>

    <div v-if="loading" class="state">正在加载邮件…</div>
    <div v-else-if="!detail" class="state">{{ mailStore.error || '邮件不存在' }}</div>
    <div v-else class="drawer-content">
      <h1>{{ detail.subject }}</h1>
      <div class="meta">
        <span class="avatar">{{ detail.senderEmail.charAt(0).toUpperCase() }}</span>
        <div><strong>{{ detail.senderEmail }}</strong><small>发送至 {{ detail.recipients.join(', ') }}</small></div>
        <time>{{ new Date(detail.sentAt).toLocaleString() }}</time>
      </div>

      <section v-if="showAiSummaryCard" class="ai-summary-card">
        <div class="ai-summary-title">
          <span>AI 总结</span>
          <small>{{ aiStatusText(aiStatus) }}</small>
        </div>
        <ul v-if="aiSummaryLines.length">
          <li v-for="line in aiSummaryLines" :key="line">{{ line }}</li>
        </ul>
        <p v-else>{{ aiEmptyText(aiStatus) }}</p>
      </section>
      <section v-if="thread.length > 1" class="thread">
        <details v-for="message in thread" :key="message.mailId" :open="message.mailId === detail.mailId">
          <summary>{{ message.senderEmail }} · {{ new Date(message.sentAt).toLocaleString() }}</summary>
          <p>{{ message.contentText }}</p>
        </details>
      </section>
      <article v-else class="body">{{ detail.contentText }}</article>

      <section v-if="detail.attachments.length" class="attachments">
        <h2>附件</h2>
        <button v-for="attachment in detail.attachments" :key="attachment.id" type="button"
          @click="downloadAttachment(attachment.id, attachment.fileName)">
          📎 {{ attachment.fileName }} · {{ Math.ceil(attachment.fileSize / 1024) }} KB
        </button>
      </section>

      <div class="reply-actions">
        <button type="button" @click="startReply">回复</button>
        <button type="button" @click="startForward">转发</button>
      </div>

      <section v-if="replyMode !== 'none'" class="reply-panel">
        <input v-model="replyTo" placeholder="收件人，多个地址用逗号分隔" />
        <textarea v-model="replyContent" :placeholder="replyMode === 'reply' ? '撰写回复' : '补充转发说明'"></textarea>
        <div>
          <button type="button" :disabled="sending" @click="sendReply">{{ sending ? '发送中…' : '发送' }}</button>
          <button type="button" @click="replyMode = 'none'">取消</button>
        </div>
      </section>
    </div>
  </div>

  <!-- Overlay mode (original drawer behavior) -->
  <transition v-else name="drawer">
    <div v-if="mailId" class="drawer-overlay" @click.self="emit('close')">
      <aside class="drawer-panel">
        <header class="drawer-header">
          <button type="button" @click="emit('close')">✕</button>
          <div v-if="detail" class="header-actions">
            <button type="button" @click="toggleStar">{{ detail.starred ? '★' : '☆' }}</button>
            <select aria-label="添加标签" @change="assignLabel">
              <option value="">标签</option>
              <option v-for="label in customLabels" :key="label.id" :value="label.id">{{ label.name }}</option>
            </select>
            <button v-if="isTrash" type="button" @click="restoreMail">恢复</button>
            <button v-else type="button" @click="moveToJunk">移至 Junk</button>
            <button type="button" @click="deleteMail">{{ isTrash ? '永久删除' : '删除' }}</button>
          </div>
        </header>

        <div v-if="loading" class="state">正在加载邮件…</div>
        <div v-else-if="!detail" class="state">{{ mailStore.error || '邮件不存在' }}</div>
        <div v-else class="drawer-content">
          <h1>{{ detail.subject }}</h1>
          <div class="meta">
            <span class="avatar">{{ detail.senderEmail.charAt(0).toUpperCase() }}</span>
            <div><strong>{{ detail.senderEmail }}</strong><small>发送至 {{ detail.recipients.join(', ') }}</small></div>
            <time>{{ new Date(detail.sentAt).toLocaleString() }}</time>
          </div>

          <section v-if="showAiSummaryCard" class="ai-summary-card">
            <div class="ai-summary-title">
              <span>AI 总结</span>
              <small>{{ aiStatusText(aiStatus) }}</small>
            </div>
            <ul v-if="aiSummaryLines.length">
              <li v-for="line in aiSummaryLines" :key="line">{{ line }}</li>
            </ul>
            <p v-else>{{ aiEmptyText(aiStatus) }}</p>
          </section>
          <section v-if="thread.length > 1" class="thread">
            <details v-for="message in thread" :key="message.mailId" :open="message.mailId === detail.mailId">
              <summary>{{ message.senderEmail }} · {{ new Date(message.sentAt).toLocaleString() }}</summary>
              <p>{{ message.contentText }}</p>
            </details>
          </section>
          <article v-else class="body">{{ detail.contentText }}</article>

          <section v-if="detail.attachments.length" class="attachments">
            <h2>附件</h2>
            <button v-for="attachment in detail.attachments" :key="attachment.id" type="button"
              @click="downloadAttachment(attachment.id, attachment.fileName)">
              📎 {{ attachment.fileName }} · {{ Math.ceil(attachment.fileSize / 1024) }} KB
            </button>
          </section>

          <div class="reply-actions">
            <button type="button" @click="startReply">回复</button>
            <button type="button" @click="startForward">转发</button>
          </div>

          <section v-if="replyMode !== 'none'" class="reply-panel">
            <input v-model="replyTo" placeholder="收件人，多个地址用逗号分隔" />
            <textarea v-model="replyContent" :placeholder="replyMode === 'reply' ? '撰写回复' : '补充转发说明'"></textarea>
            <div>
              <button type="button" :disabled="sending" @click="sendReply">{{ sending ? '发送中…' : '发送' }}</button>
              <button type="button" @click="replyMode = 'none'">取消</button>
            </div>
          </section>
        </div>
      </aside>
    </div>
  </transition>
</template>

<style scoped>
/* ---- inline panel ---- */
.inline-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #fbfbfa;
}

/* ---- overlay drawer ---- */
.drawer-overlay { position: fixed; inset: 0; z-index: 300; display: flex; justify-content: flex-end; background: rgba(15, 23, 42, .2); }
.drawer-panel { width: min(720px, 92vw); height: 100%; display: flex; flex-direction: column; background: #fff; box-shadow: -8px 0 30px rgba(0, 0, 0, .06); }

/* ---- shared ---- */
.drawer-header {
  min-height: 44px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e0e0e0;
  background: #fbfbfa;
  gap: 12px;
}

.back-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: auto;
  height: 32px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  color: rgba(55, 53, 47, 0.55);
  flex-shrink: 0;
  transition: all 0.1s;
}

.back-btn:hover {
  background: #f4f4f4;
  color: #37352f;
}

.drawer-header button, .drawer-header select { border: 1px solid #e0e0e0; border-radius: 4px; background: #fff; padding: 5px 10px; cursor: pointer; font-size: 12px; color: #37352f; transition: background 0.1s; }
.drawer-header button:hover, .drawer-header select:hover { background: #f4f4f4; }
.header-actions { display: flex; gap: 6px; flex-wrap: wrap; align-items: center; flex: 1; }
.drawer-content { overflow-y: auto; padding: 28px 32px 48px; flex: 1; }
.inline-panel .drawer-content { padding: 24px 28px 40px; }
h1 { margin: 0 0 20px; font-size: 22px; font-weight: 600; color: #37352f; line-height: 1.4; }
.inline-panel h1 { font-size: 20px; }
.meta { display: grid; grid-template-columns: 36px 1fr auto; gap: 12px; align-items: center; }
.meta small { display: block; margin-top: 2px; color: rgba(55, 53, 47, 0.5); font-size: 12px; }
.meta strong { color: #37352f; font-size: 14px; font-weight: 500; }
.meta time { color: rgba(55, 53, 47, 0.45); font-size: 11px; }
.avatar { width: 36px; height: 36px; display: grid; place-items: center; border-radius: 50%; color: #37352f; background: #f0efed; font-weight: 600; font-size: 13px; }
.ai-summary-card { margin-top: 24px; padding: 14px 16px; border: 1px solid #dbeafe; border-radius: 8px; background: #eff6ff; color: #1e3a8a; }
.ai-summary-title { margin-bottom: 8px; display: flex; justify-content: space-between; gap: 12px; font-size: 12px; font-weight: 600; letter-spacing: 0.03em; color: #2563eb; }
.ai-summary-title small { color: rgba(30, 58, 138, .58); font-weight: 400; letter-spacing: 0; }
.ai-summary-card ul { margin: 0; padding-left: 18px; display: grid; gap: 4px; }
.ai-summary-card li { line-height: 1.55; font-size: 13px; }
.ai-summary-card p { margin: 0; color: rgba(30, 58, 138, .68); font-size: 13px; line-height: 1.55; }
.body, .thread { margin-top: 28px; white-space: pre-wrap; line-height: 1.75; color: #37352f; font-size: 14px; }
.inline-panel .body, .inline-panel .thread { margin-top: 24px; font-size: 13px; }
.thread details { padding: 12px 0; border-bottom: 1px solid #f0efed; }
.thread details + details { margin-top: 0; }
.thread summary { cursor: pointer; color: rgba(55, 53, 47, 0.5); font-size: 12px; padding: 4px 0; }
.thread summary:hover { color: #37352f; }
.thread p { white-space: pre-wrap; margin-top: 10px; font-size: 13px; }
.attachments { margin-top: 24px; }
.attachments h2 { font-size: 12px; font-weight: 500; color: rgba(55, 53, 47, 0.5); margin-bottom: 8px; text-transform: uppercase; letter-spacing: 0.3px; }
.attachments button { display: block; width: 100%; padding: 9px 14px; margin: 5px 0; text-align: left; border: 1px solid #e0e0e0; border-radius: 4px; background: #fff; cursor: pointer; font-size: 12px; color: #37352f; transition: background 0.1s; }
.attachments button:hover { background: #f4f4f4; }
.reply-actions { display: flex; gap: 8px; margin-top: 28px; padding-top: 18px; border-top: 1px solid #e0e0e0; }
.reply-actions button { padding: 7px 18px; border: 1px solid #e0e0e0; border-radius: 4px; color: #37352f; background: #fff; cursor: pointer; font-size: 13px; transition: background 0.1s; }
.reply-actions button:hover { background: #f4f4f4; }
.reply-panel { display: grid; gap: 10px; margin-top: 14px; }
.reply-panel input, .reply-panel textarea { padding: 8px 12px; border: 1px solid #e0e0e0; border-radius: 4px; font: inherit; font-size: 13px; color: #37352f; outline: none; }
.reply-panel input:focus, .reply-panel textarea:focus { border-color: #37352f; }
.reply-panel textarea { min-height: 120px; resize: vertical; }
.reply-panel div { display: flex; gap: 8px; }
.reply-panel div button:first-child { padding: 7px 18px; border: none; border-radius: 4px; color: #fff; background: #37352f; cursor: pointer; font-size: 13px; }
.reply-panel div button:first-child:hover { background: #2b2925; }
.reply-panel div button:last-child { padding: 7px 18px; border: 1px solid #e0e0e0; border-radius: 4px; color: #37352f; background: #fff; cursor: pointer; font-size: 13px; }
.reply-panel div button:last-child:hover { background: #f4f4f4; }
.state { margin: auto; color: rgba(55, 53, 47, 0.5); font-size: 13px; padding: 32px; }
.drawer-enter-active, .drawer-leave-active { transition: opacity .2s ease; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
@media (max-width: 640px) { .drawer-panel { width: 100%; } .drawer-content { padding: 20px; } .meta { grid-template-columns: 36px 1fr; } .meta time { grid-column: 2; } }
</style>
