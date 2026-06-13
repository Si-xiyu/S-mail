<script setup lang="ts">
import { useMailStore } from '../stores/mailStore'
import { computed, ref } from 'vue'

const mailStore = useMailStore()
const selectedMailId = ref<string | null>(null)

const emit = defineEmits<{
  selectMail: [mailId: string]
}>()

const handleSelectMail = (mailId: string) => {
  selectedMailId.value = mailId
  mailStore.markAsRead(mailId)
  emit('selectMail', mailId)
}

const getLabelName = (labelId: string) => {
  const label = mailStore.labels.find(l => l.id === labelId)
  return label?.name || labelId
}

const formatTime = (timestamp: number) => {
  const now = Date.now()
  const diff = now - timestamp
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (minutes < 60) {
    return `${minutes}m ago`
  } else if (hours < 24) {
    return `${hours}h ago`
  } else if (days < 7) {
    return `${days}d ago`
  } else {
    return new Date(timestamp).toLocaleDateString()
  }
}
</script>

<template>
  <div class="mail-list">
    <div class="list-header">
      <div class="header-left">
        <input type="checkbox" class="checkbox" />
        <span class="refresh-btn">🔄</span>
      </div>
      <div class="header-right">
        <span class="pagination">1-20 of {{ mailStore.mailItems.length }}</span>
      </div>
    </div>

    <div class="list-container">
      <div
        v-for="item in mailStore.mailItems"
        :key="item.id"
        class="mail-row"
        :class="{
          selected: selectedMailId === item.id,
          unread: !item.read
        }"
        @click="handleSelectMail(item.id)"
      >
        <div class="row-checkbox">
          <input type="checkbox" class="checkbox" @click.stop />
        </div>

        <div class="row-star">
          <button
            class="star-btn"
            :class="{ starred: item.starred }"
            @click.stop="mailStore.toggleStar(item.id)"
          >
            ⭐
          </button>
        </div>

        <div class="row-sender">
          <span class="sender-name">{{ item.senderName || item.senderEmail }}</span>
        </div>

        <div class="row-subject">
          <span class="subject-text">{{ item.subject }}</span>
          <span v-if="item.hasAttachment" class="attachment-icon">📎</span>
        </div>

        <div class="row-preview">
          <span class="preview-text">{{ item.preview }}</span>
        </div>

        <div class="row-time">
          <span class="time-text">{{ formatTime(item.timestamp) }}</span>
        </div>
      </div>
    </div>

    <div v-if="mailStore.mailItems.length === 0" class="empty-state">
      <div class="empty-icon">📭</div>
      <p>No emails in this label</p>
    </div>
  </div>
</template>

<style scoped>
.mail-list {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: white;
  border-right: 1px solid #e5e7eb;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #fafbfc;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.checkbox {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.refresh-btn {
  font-size: 18px;
  cursor: pointer;
  transition: transform 0.2s;
}

.refresh-btn:hover {
  transform: rotate(180deg);
}

.header-right {
  font-size: 12px;
  color: #6b7280;
}

.list-container {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.mail-row {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f3f4f6;
  cursor: pointer;
  transition: all 0.2s;
  gap: 12px;
}

.mail-row:hover {
  background: #f9fafb;
  box-shadow: inset 1px 0 0 #e5e7eb;
}

.mail-row.selected {
  background: #e0e7ff;
}

.mail-row.unread {
  background: #f3f7ff;
}

.mail-row.unread .sender-name,
.mail-row.unread .subject-text {
  font-weight: 600;
  color: #1f2937;
}

.row-checkbox {
  flex: 0 0 24px;
}

.row-star {
  flex: 0 0 24px;
}

.star-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
  opacity: 0.3;
  transition: all 0.2s;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.star-btn:hover {
  opacity: 0.6;
}

.star-btn.starred {
  opacity: 1;
}

.row-sender {
  flex: 0 0 120px;
  min-width: 100px;
}

.sender-name {
  font-size: 14px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.row-subject {
  flex: 0 0 200px;
  min-width: 150px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.subject-text {
  font-size: 14px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-icon {
  font-size: 12px;
  flex-shrink: 0;
}

.row-preview {
  flex: 1;
  min-width: 100px;
}

.preview-text {
  font-size: 13px;
  color: #9ca3af;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: block;
}

.row-time {
  flex: 0 0 60px;
  text-align: right;
}

.time-text {
  font-size: 12px;
  color: #9ca3af;
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
</style>
