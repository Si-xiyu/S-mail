<script setup lang="ts">
import { useMailStore } from '../stores/mailStore'
import { ElMessage } from 'element-plus'
import { computed, ref } from 'vue'

const mailStore = useMailStore()
const selectedMailId = ref<string | null>(null)

const emit = defineEmits<{
  selectMail: [mailId: string]
}>()

const handleSelectMail = async (mailId: string) => {
  selectedMailId.value = mailId
  try {
    await mailStore.markAsRead(mailId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新已读状态失败')
  }
  emit('selectMail', mailId)
}

const handleToggleStar = async (mailId: string) => {
  try {
    await mailStore.toggleStar(mailId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '更新星标失败')
  }
}

const rangeStart = computed(() => mailStore.total ? (mailStore.page - 1) * mailStore.pageSize + 1 : 0)
const rangeEnd = computed(() => Math.min(mailStore.page * mailStore.pageSize, mailStore.total))
const hasPrevious = computed(() => mailStore.page > 1)
const hasNext = computed(() => mailStore.page * mailStore.pageSize < mailStore.total)

const changePage = async (nextPage: number) => {
  if (mailStore.searchQuery) {
    await mailStore.searchMailbox(mailStore.searchQuery, nextPage)
  } else if (mailStore.currentLabel === 'STARRED') {
    await mailStore.loadStarredMails(nextPage)
  } else {
    await mailStore.loadMailbox(mailStore.currentLabel, nextPage, mailStore.pageSize)
  }
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
        <button type="button" class="refresh-btn" :disabled="mailStore.isLoading" @click="mailStore.refreshCurrent()">🔄</button>
      </div>
      <div class="header-right">
        <span class="pagination">{{ rangeStart }}-{{ rangeEnd }} / {{ mailStore.total }}</span>
        <button type="button" :disabled="!hasPrevious" @click="changePage(mailStore.page - 1)">‹</button>
        <button type="button" :disabled="!hasNext" @click="changePage(mailStore.page + 1)">›</button>
      </div>
    </div>

    <div v-if="mailStore.error" class="error-state">
      <span>{{ mailStore.error }}</span>
      <button type="button" @click="mailStore.refreshCurrent()">重试</button>
    </div>

    <div v-if="mailStore.isLoading" class="loading-state">正在加载…</div>
    <div v-else class="list-container">
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
            @click.stop="handleToggleStar(item.id)"
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

    <div v-if="!mailStore.isLoading && mailStore.mailItems.length === 0" class="empty-state">
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
  flex: 1;
  background: #fff;
  min-width: 0;
}

.list-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  border-bottom: 1px solid #e0e0e0;
  background: #fff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-right { display: flex; align-items: center; gap: 4px; }
.header-right button {
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
  color: rgba(55, 53, 47, 0.45);
  padding: 4px 8px;
  font-size: 14px;
  transition: background 0.1s;
}
.header-right button:hover:not(:disabled) { background: #f4f4f4; }
.header-right button:disabled { opacity: .3; cursor: default; }

.pagination {
  font-size: 12px;
  color: rgba(55, 53, 47, 0.5);
  margin-right: 4px;
}

.checkbox {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #37352f;
}

.refresh-btn {
  border: 0;
  background: transparent;
  font-size: 16px;
  cursor: pointer;
  color: rgba(55, 53, 47, 0.5);
  transition: transform 0.2s, color 0.1s;
  padding: 4px;
  border-radius: 4px;
}

.refresh-btn:hover {
  color: #37352f;
  background: #f4f4f4;
  transform: rotate(180deg);
}

.refresh-btn:disabled {
  cursor: default;
  opacity: .3;
}

.refresh-btn:disabled:hover {
  transform: none;
  background: transparent;
}

.loading-state, .error-state { padding: 24px; text-align: center; color: rgba(55, 53, 47, 0.5); font-size: 13px; }
.error-state { color: #e03e3e; background: #fef2f2; border-radius: 4px; margin: 8px; }
.error-state button { margin-left: 10px; border: 1px solid #e0e0e0; border-radius: 4px; padding: 4px 12px; cursor: pointer; background: #fff; }

.header-right {
  font-size: 12px;
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
  padding: 12px 20px;
  border-bottom: 1px solid #f0efed;
  cursor: pointer;
  transition: background 0.1s;
  gap: 14px;
}

.mail-row:hover {
  background: #fbfbfa;
}

.mail-row.selected {
  background: #f4f4f4;
}

.mail-row.unread {
  background: #fcfcfa;
}

.mail-row.unread .sender-name,
.mail-row.unread .subject-text {
  font-weight: 600;
  color: #37352f;
}

.row-checkbox {
  flex: 0 0 20px;
}

.row-star {
  flex: 0 0 22px;
}

.star-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 15px;
  opacity: 0.25;
  transition: opacity 0.15s, transform 0.15s;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.star-btn:hover {
  opacity: 0.55;
  transform: scale(1.1);
}

.star-btn.starred {
  opacity: 1;
}

.row-sender {
  flex: 0 0 130px;
  min-width: 100px;
}

.sender-name {
  font-size: 13px;
  color: #37352f;
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
  font-size: 13px;
  color: #37352f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-icon {
  font-size: 12px;
  flex-shrink: 0;
  opacity: 0.5;
}

.row-preview {
  flex: 1;
  min-width: 100px;
}

.preview-text {
  font-size: 12px;
  color: rgba(55, 53, 47, 0.5);
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
  font-size: 11px;
  color: rgba(55, 53, 47, 0.45);
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 16px;
  color: rgba(55, 53, 47, 0.35);
}

.empty-icon {
  font-size: 48px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}
</style>
