<script setup lang="ts">
import { useMailStore } from '../stores/mailStore'
import { computed } from 'vue'
import type { Mail } from '../types'

const props = defineProps<{
  mailId?: string | null
}>()

const emit = defineEmits<{
  close: []
}>()

const mailStore = useMailStore()

const mail = computed(() => {
  if (!props.mailId) return null
  return mailStore.getMailById(props.mailId)
})

const formatDate = (timestamp: number) => {
  return new Date(timestamp).toLocaleString()
}

const handleReply = () => {
  console.log('Reply to:', mail.value?.senderEmail)
}

const handleReplyAll = () => {
  console.log('Reply all to:', mail.value?.to)
}

const handleForward = () => {
  console.log('Forward mail:', mail.value?.id)
}

const handleDelete = () => {
  if (mail.value) {
    mailStore.deleteMail(mail.value.id)
    emit('close')
  }
}

const handleBackdropClick = () => {
  emit('close')
}
</script>

<template>
  <transition name="drawer">
    <div v-if="mail" class="drawer-overlay" @click="handleBackdropClick">
      <div class="drawer-panel" @click.stop>
        <div class="drawer-header">
          <button class="close-btn" @click="emit('close')">✕</button>
        </div>

        <div class="drawer-content">
          <div class="detail-header">
            <div class="detail-title">
              <h1 class="subject">{{ mail.subject }}</h1>
              <div class="sender-info">
                <div class="sender-avatar">
                  {{ mail.senderName.charAt(0).toUpperCase() }}
                </div>
                <div class="sender-details">
                  <div class="sender-name">{{ mail.senderName }}</div>
                  <div class="sender-email">{{ mail.senderEmail }}</div>
                </div>
                <div class="mail-date">{{ formatDate(mail.timestamp) }}</div>
              </div>
            </div>

            <div class="detail-actions">
              <button class="action-btn" @click="mailStore.toggleStar(mail.id)">
                <span :class="{ starred: mail.starred }">⭐</span>
              </button>
              <button class="action-btn">
                <span>🏷️</span>
              </button>
              <button class="action-btn">
                <span>📋</span>
              </button>
              <button class="action-btn" @click="handleDelete">
                <span>🗑️</span>
              </button>
              <button class="action-btn">
                <span>⋯</span>
              </button>
            </div>
          </div>

          <div class="recipients">
            <div class="recipient-row">
              <span class="label">To:</span>
              <span class="value">{{ mail.to.join(', ') }}</span>
            </div>
            <div v-if="mail.cc?.length" class="recipient-row">
              <span class="label">Cc:</span>
              <span class="value">{{ mail.cc.join(', ') }}</span>
            </div>
          </div>

          <div class="mail-content">
            <article class="content-text">{{ mail.content }}</article>
          </div>

          <div v-if="mail.attachments?.length" class="attachments">
            <h3>Attachments</h3>
            <div class="attachment-list">
              <div v-for="attachment in mail.attachments" :key="attachment.id" class="attachment-item">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2m0 18c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8m3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5m-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11m3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z" />
                </svg>
                <div class="attachment-info">
                  <div class="attachment-name">{{ attachment.filename }}</div>
                  <div class="attachment-size">{{ (attachment.size / 1024).toFixed(1) }} KB</div>
                </div>
                <button class="download-btn">⬇️</button>
              </div>
            </div>
          </div>

          <div class="reply-actions">
            <button class="reply-btn" @click="handleReply">
              <span>⬅️</span> Reply
            </button>
            <button class="reply-btn" @click="handleReplyAll">
              <span>⬅️⬅️</span> Reply All
            </button>
            <button class="reply-btn" @click="handleForward">
              <span>➡️</span> Forward
            </button>
          </div>

          <div class="more-actions">
            <button class="text-btn">Report phishing</button>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.drawer-overlay {
  position: fixed;
  top: 0;
  right: 0;
  bottom: 0;
  left: 0;
  background: rgba(0, 0, 0, 0.3);
  display: flex;
  justify-content: flex-end;
  z-index: 100;
  animation: fadeIn 0.2s ease-out;
}

.drawer-panel {
  width: 500px;
  height: 100%;
  background: white;
  display: flex;
  flex-direction: column;
  box-shadow: -2px 0 12px rgba(0, 0, 0, 0.1);
  animation: slideInRight 0.3s ease-out;
}

.drawer-header {
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
}

.close-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  background: transparent;
  border-radius: 8px;
  cursor: pointer;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.2s;
}

.close-btn:hover {
  background: #f9fafb;
  color: #1f2937;
}

.drawer-content {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  justify-content: flex-end;
  min-width: 200px;
  flex-wrap: wrap;
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
  flex: 1;
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

/* 抽屉动画 */
.drawer-enter-active,
.drawer-leave-active {
  transition: all 0.3s ease;
}

.drawer-enter-from {
  opacity: 0;
}

.drawer-enter-from :deep(.drawer-panel) {
  transform: translateX(100%);
}

.drawer-leave-to {
  opacity: 0;
}

.drawer-leave-to :deep(.drawer-panel) {
  transform: translateX(100%);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slideInRight {
  from {
    transform: translateX(100%);
  }
  to {
    transform: translateX(0);
  }
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .drawer-panel {
    width: 100%;
  }
}

@media (max-width: 768px) {
  .drawer-panel {
    width: 100%;
  }

  .detail-header {
    flex-direction: column;
    align-items: stretch;
  }

  .detail-actions {
    justify-content: flex-start;
  }

  .sender-info {
    flex-direction: column;
    align-items: flex-start;
  }

  .mail-date {
    white-space: normal;
  }

  .subject {
    font-size: 20px;
  }
}
</style>
