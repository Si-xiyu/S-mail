<script setup lang="ts">
import { useMailStore } from '../stores/mailStore'
import { computed, ref, onMounted, watch, onBeforeUnmount } from 'vue'
import type { MailDetail } from '../types/mail'

const props = defineProps<{
  mailId?: string | null
}>()

const emit = defineEmits<{
  back: []
}>()

const mailStore = useMailStore()
const showLabelMenu = ref(false)
const replyMode = ref<'none' | 'reply' | 'forward'>('none')
const replyContent = ref('')
const mailThread = ref<MailDetail[]>([])
const threadLoading = ref(false)
const labelMenuRef = ref<HTMLElement | null>(null)
const labelButtonRef = ref<HTMLElement | null>(null)
const labelMenuPosition = ref({ top: 0, left: 240 })

// 系统自带标签的 ID 列表
const SYSTEM_LABEL_IDS = ['INBOX', 'STARRED', 'SENT', 'DRAFTS', 'TRASH', 'SPAM', 'JUNK']

// 只显示用户个性化标签
const customLabels = computed(() => {
  return mailStore.labels.filter(l => !SYSTEM_LABEL_IDS.includes(l.id))
})

const mail = computed(() => {
  if (!props.mailId) return null
  return mailStore.getMailById(props.mailId)
})

const isInTrash = computed(() => {
  return mailStore.currentLabel === 'TRASH'
})

const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleString()
}

const formatShortDate = (timestamp: number) => {
  const date = new Date(timestamp)
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(yesterday.getDate() - 1)

  if (date.toDateString() === today.toDateString()) {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
  } else if (date.toDateString() === yesterday.toDateString()) {
    return 'Yesterday'
  } else {
    return date.toLocaleDateString()
  }
}

// 加载邮件对话路径
onMounted(async () => {
  if (props.mailId) {
    threadLoading.value = true
    try {
      const mailIdNum = parseInt(props.mailId, 10)
      mailThread.value = await mailStore.getMailPath(mailIdNum)
    } catch (err) {
      console.error('Failed to load mail path:', err)
    } finally {
      threadLoading.value = false
    }
  }
})

// 监听标签菜单显示状态，更新位置
watch(showLabelMenu, (newVal) => {
  if (newVal && labelButtonRef.value) {
    const rect = labelButtonRef.value.getBoundingClientRect()
    labelMenuPosition.value = {
      top: rect.bottom + 4,
      left: 240
    }
    // 延迟添加全局点击监听，避免立即关闭
    setTimeout(() => {
      document.addEventListener('click', handleMenuClickOutside)
    }, 0)
  } else {
    document.removeEventListener('click', handleMenuClickOutside)
  }
})

const handleMenuClickOutside = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (labelMenuRef.value && labelButtonRef.value) {
    if (!labelMenuRef.value.contains(target) && !labelButtonRef.value.contains(target)) {
      showLabelMenu.value = false
    }
  }
}

// 组件卸载时清理事件监听器
onBeforeUnmount(() => {
  document.removeEventListener('click', handleMenuClickOutside)
})

const handleReply = () => {
  replyMode.value = 'reply'
  replyContent.value = ''
}

const handleForward = () => {
  replyMode.value = 'forward'
  replyContent.value = ''
}

const handleCancelReply = () => {
  replyMode.value = 'none'
  replyContent.value = ''
}

const handleSendReply = async () => {
  if (!replyContent.value.trim()) {
    alert('Please enter your message')
    return
  }

  if (!mail.value) return

  try {
    const recipients = replyMode.value === 'reply'
      ? [mail.value.senderEmail]
      : [mail.value.senderEmail]

    const subject = replyMode.value === 'reply'
      ? `Re: ${mail.value.subject}`
      : `Fwd: ${mail.value.subject}`

    const mailIdNum = parseInt(props.mailId || '0', 10)
    const parentMailId = replyMode.value === 'reply' ? mailIdNum : undefined

    await mailStore.sendMail(recipients, subject, replyContent.value, [], undefined, parentMailId)

    replyMode.value = 'none'
    replyContent.value = ''

    // 重新加载对话路径
    if (props.mailId) {
      const newMailIdNum = parseInt(props.mailId, 10)
      mailThread.value = await mailStore.getMailPath(newMailIdNum)
    }
  } catch (err) {
    alert('Failed to send: ' + (err instanceof Error ? err.message : 'Unknown error'))
  }
}

const handleMarkAsSpam = () => {
  if (mail.value) {
    mailStore.moveToLabel(mail.value.id, 'SPAM')
  }
}

const handleDelete = () => {
  if (mail.value) {
    mailStore.deleteMail(mail.value.id)
    emit('back')
  }
}

const handleRestore = async () => {
  if (mail.value) {
    try {
      // 后端会根据 originalFolder 自动恢复到原来的位置
      // 这里传任意 folder（后端会忽略），后端会用 originalFolder
      await mailStore.moveToLabel(mail.value.id, 'RESTORE')
      emit('back')
    } catch (err) {
      alert('Failed to restore email')
    }
  }
}

const handleToggleStar = async () => {
  if (!mail.value || mail.value.itemId === undefined) return
  try {
    await mailStore.starMail(mail.value.itemId, !mail.value.starred)
  } catch (err) {
    alert('Failed to toggle star: ' + (err instanceof Error ? err.message : 'Unknown error'))
  }
}

const handlePermanentDelete = () => {
  if (mail.value && confirm('Are you sure you want to permanently delete this email? This action cannot be undone.')) {
    mailStore.deleteMail(mail.value.id)
    emit('back')
  }
}

const handleAddToLabel = async (labelId: string) => {
  if (!mail.value) return

  try {
    // 添加到自定义标签（复制方式，原邮件不删除）
    await mailStore.changeCategory(mail.value.id, labelId)
    showLabelMenu.value = false
  } catch (err) {
    alert('Failed to add to label: ' + (err instanceof Error ? err.message : 'Unknown error'))
  }
}
</script>

<template>
  <div class="detail-page">
    <!-- 顶部返回栏 -->
    <div class="detail-top-bar">
      <button class="back-btn" @click="emit('back')">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="19" y1="12" x2="5" y2="12" />
          <polyline points="12 19 5 12 12 5" />
        </svg>
        Back
      </button>
    </div>

    <!-- 邮件线程内容 -->
    <div v-if="mail && !threadLoading" class="detail-content">
      <!-- 邮件线程 -->
      <div class="thread-container">
        <div
          v-for="(threadMail, index) in mailThread"
          :key="index"
          class="thread-message"
        >
          <div class="thread-message-header">
            <div class="thread-message-sender">
              <span class="sender-avatar">{{ (threadMail.senderName || threadMail.senderEmail).charAt(0).toUpperCase() }}</span>
              <div class="sender-info-text">
                <span class="sender-name">{{ threadMail.senderName || threadMail.senderEmail }}</span>
                <span class="sender-email">{{ threadMail.senderEmail }}</span>
              </div>
            </div>
            <span class="message-time">{{ formatShortDate(new Date(threadMail.receivedAt).getTime()) }}</span>
          </div>

          <div class="thread-message-subject" v-if="index === 0">
            <h2>{{ threadMail.subject }}</h2>
          </div>

          <div class="thread-message-body">
            {{ threadMail.contentText }}
          </div>
        </div>
      </div>

      <!-- 当前邮件的操作按钮 -->
      <div v-if="mail" class="current-mail-actions">
        <div class="detail-header">
          <div class="detail-title">
            <div class="action-buttons-group">
              <div class="detail-actions">
                <button class="action-btn" @click="handleToggleStar" title="Star">
                  <span :class="{ starred: mail.starred }">⭐</span>
                </button>
                <div class="label-action-wrapper">
                  <button ref="labelButtonRef" class="action-btn" @click="showLabelMenu = !showLabelMenu" title="Label">
                    <span>🏷️</span>
                  </button>
                  <div v-if="showLabelMenu" ref="labelMenuRef" class="label-menu" :style="{ top: labelMenuPosition.top + 'px', left: labelMenuPosition.left + 'px' }">
                    <div class="label-menu-title">Add to Custom Tag</div>
                    <div v-if="customLabels.length === 0" class="label-menu-empty">
                      No custom tags yet. Create one in the sidebar!
                    </div>
                    <button
                      v-for="label in customLabels"
                      :key="label.id"
                      class="label-menu-item"
                      @click="handleAddToLabel(label.id)"
                    >
                      {{ label.name }}
                    </button>
                  </div>
                </div>
                <button v-if="!isInTrash" class="action-btn" @click="handleDelete" title="Delete">
                  <span>🗑️</span>
                </button>
                <button v-if="isInTrash" class="action-btn restore" @click="handleRestore" title="Restore">
                  <span>↩️</span>
                </button>
                <button v-if="isInTrash" class="action-btn delete-permanent" @click="handlePermanentDelete" title="Permanently Delete">
                  <span>🔥</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <div class="reply-actions">
          <button class="reply-btn" @click="handleReply">
            <span>⬅️</span> Reply
          </button>
          <button class="reply-btn" @click="handleForward">
            <span>➡️</span> Forward
          </button>
        </div>
      </div>

      <!-- Reply/Forward Panel -->
      <div v-if="replyMode !== 'none'" class="reply-panel">
        <div class="reply-header">
          <span class="reply-label">{{ replyMode === 'reply' ? 'Reply to' : 'Forward to' }} {{ mail.senderEmail }}</span>
          <button class="close-btn" @click="handleCancelReply">✕</button>
        </div>
        <textarea
          v-model="replyContent"
          class="reply-textarea"
          :placeholder="replyMode === 'reply' ? 'Type your reply...' : 'Type your forwarded message...'"
        ></textarea>
        <div class="reply-footer">
          <button class="send-btn" @click="handleSendReply">Send</button>
          <button class="cancel-btn" @click="handleCancelReply">Cancel</button>
        </div>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-else-if="threadLoading" class="detail-content">
      <div class="loading-state">
        <div class="loading-spinner">⏳</div>
        <p>Loading conversation...</p>
      </div>
    </div>

    <!-- 空状态 -->
    <div v-else class="empty-state">
      <div class="empty-icon">📧</div>
      <p>Email not found</p>
    </div>
  </div>
</template>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  height: 100%;
  width: 100%;
  background: white;
  overflow: hidden;
}

.detail-top-bar {
  padding: 12px 24px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafbfc;
  display: flex;
  align-items: center;
  gap: 12px;
  height: 64px;
  flex-shrink: 0;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  color: #374151;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.2s;
}

.back-btn:hover {
  background: #f9fafb;
  color: #1f2937;
}

.detail-content {
  height: calc(100% - 64px);
  width: 100%;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: #9ca3af;
}

.empty-icon {
  font-size: 48px;
}

.empty-state p {
  margin: 0;
  font-size: 16px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  border-bottom: 1px solid #e5e7eb;
  padding-bottom: 16px;
}

.detail-title {
  flex: 1;
}

.subject {
  margin: 0 0 16px;
  font-size: 24px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.3;
}

.sender-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sender-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e7ff;
  color: #667eea;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  flex-shrink: 0;
}

.sender-details {
  flex: 1;
}

.sender-name {
  font-weight: 500;
  color: #1f2937;
  font-size: 14px;
}

.sender-email {
  font-size: 12px;
  color: #6b7280;
}

.mail-date {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

.detail-actions {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  position: relative;
}

.label-action-wrapper {
  position: relative;
}

.label-menu {
  position: fixed;
  top: 0;
  left: 240px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  min-width: 160px;
  overflow: hidden;
}

.label-menu-title {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
  border-bottom: 1px solid #e5e7eb;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.label-menu-item {
  display: block;
  width: 100%;
  padding: 8px 12px;
  text-align: left;
  background: none;
  border: none;
  cursor: pointer;
  color: #374151;
  font-size: 13px;
  transition: background 0.2s;
}

.label-menu-item:hover {
  background: #f3f4f6;
}

.label-menu-empty {
  padding: 12px;
  text-align: center;
  color: #9ca3af;
  font-size: 12px;
  line-height: 1.5;
}

.action-btn {
  width: 36px;
  height: 36px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f9fafb;
}

.action-btn.restore {
  border-color: #10b981;
  color: #10b981;
}

.action-btn.restore:hover {
  background: #f0fdf4;
  border-color: #059669;
  color: #059669;
}

.action-btn.delete-permanent {
  border-color: #ef4444;
  color: #ef4444;
}

.action-btn.delete-permanent:hover {
  background: #fef2f2;
  border-color: #dc2626;
  color: #dc2626;
}

.action-btn span.starred {
  opacity: 1;
}

.recipients {
  font-size: 13px;
  color: #6b7280;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.recipient-row {
  display: flex;
  gap: 8px;
}

.recipient-row .label {
  flex: 0 0 auto;
  font-weight: 500;
}

.recipient-row .value {
  flex: 1;
  word-break: break-all;
}

.mail-content {
  padding: 16px 0;
}

.content-text {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}

.attachments {
  border-top: 1px solid #e5e7eb;
  padding-top: 16px;
}

.attachments h3 {
  margin: 0 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  color: #667eea;
}

.attachment-info {
  flex: 1;
}

.attachment-name {
  font-size: 13px;
  font-weight: 500;
  color: #1f2937;
}

.attachment-size {
  font-size: 12px;
  color: #9ca3af;
}

.download-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 18px;
  flex-shrink: 0;
  transition: all 0.2s;
}

.download-btn:hover {
  transform: scale(1.2);
}

.reply-actions {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
  flex-wrap: wrap;
}

.reply-btn {
  padding: 8px 16px;
  background: #e0e7ff;
  color: #667eea;
  border: none;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}

.reply-btn:hover {
  background: #c7d2fe;
}

.more-actions {
  display: flex;
  gap: 16px;
  padding-top: 12px;
  border-top: 1px solid #e5e7eb;
}

.text-btn {
  background: none;
  border: none;
  color: #667eea;
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  transition: all 0.2s;
}

.text-btn:hover {
  color: #764ba2;
  text-decoration: underline;
}

.reply-panel {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #f9fafb;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 8px;
  border-bottom: 1px solid #e5e7eb;
}

.reply-label {
  font-size: 13px;
  color: #6b7280;
  font-weight: 500;
}

.close-btn {
  background: none;
  border: none;
  color: #6b7280;
  cursor: pointer;
  font-size: 16px;
  transition: color 0.2s;
}

.close-btn:hover {
  color: #1f2937;
}

.reply-textarea {
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-family: inherit;
  font-size: 14px;
  color: #1f2937;
  outline: none;
  resize: vertical;
  min-height: 120px;
  background: white;
}

.reply-textarea:focus {
  border-color: #667eea;
  box-shadow: 0 0 4px rgba(102, 126, 234, 0.2);
}

.reply-footer {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.send-btn {
  padding: 8px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.cancel-btn {
  padding: 8px 16px;
  background: white;
  color: #6b7280;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.cancel-btn:hover {
  background: #f3f4f6;
  color: #1f2937;
}

.thread-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 24px;
}

.thread-message {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
  background: white;
}

.thread-message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: #f9fafb;
  border-bottom: 1px solid #e5e7eb;
}

.thread-message-sender {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sender-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #e0e7ff;
  color: #667eea;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  flex-shrink: 0;
}

.sender-info-text {
  display: flex;
  flex-direction: column;
}

.sender-name {
  font-weight: 500;
  color: #1f2937;
  font-size: 14px;
}

.sender-email {
  font-size: 12px;
  color: #6b7280;
}

.message-time {
  font-size: 12px;
  color: #9ca3af;
  white-space: nowrap;
}

.thread-message-subject {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.thread-message-subject h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f2937;
}

.thread-message-body {
  padding: 16px;
  font-size: 14px;
  line-height: 1.6;
  color: #374151;
  white-space: pre-wrap;
  word-break: break-word;
}

.current-mail-actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.action-buttons-group {
  width: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
}

.detail-title {
  flex: 1;
}

.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: #9ca3af;
}

.loading-spinner {
  font-size: 48px;
  animation: spin 2s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
