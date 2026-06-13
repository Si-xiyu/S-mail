<script setup lang="ts">
import { useMailStore } from '../stores/mailStore'
import { computed } from 'vue'

const mailStore = useMailStore()

const mainLabels = computed(() => {
  return mailStore.labels.filter(l => ['INBOX', 'STARRED', 'SENT', 'DRAFTS'].includes(l.id))
})

const otherLabels = computed(() => {
  return mailStore.labels.filter(l => !['INBOX', 'STARRED', 'SENT', 'DRAFTS'].includes(l.id))
})

const isActive = (labelId: string) => {
  return mailStore.currentLabel === labelId
}

const selectLabel = (labelId: string) => {
  mailStore.selectLabel(labelId)
}

const emit = defineEmits<{
  composeClick: []
}>()

const handleComposeClick = () => {
  emit('composeClick')
}

const getLabelIcon = (labelId: string) => {
  const icons: Record<string, string> = {
    INBOX: '📥',
    STARRED: '⭐',
    SENT: '📤',
    DRAFTS: '✏️',
    TRASH: '🗑️',
    SPAM: '🚫'
  }
  return icons[labelId] || '📁'
}
</script>

<template>
  <aside class="sidebar">
    <div class="compose-btn-container">
      <button class="compose-btn" @click="handleComposeClick">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="12" y1="5" x2="12" y2="19" />
          <line x1="5" y1="12" x2="19" y2="12" />
        </svg>
        Compose
      </button>
    </div>

    <nav class="labels-section">
      <div class="labels-group">
        <button
          v-for="label in mainLabels"
          :key="label.id"
          class="label-item"
          :class="{ active: isActive(label.id) }"
          @click="selectLabel(label.id)"
        >
          <span class="label-icon">{{ getLabelIcon(label.id) }}</span>
          <span class="label-name">{{ label.name }}</span>
          <span v-if="label.count > 0" class="label-count">{{ label.count }}</span>
        </button>
      </div>

      <div class="labels-divider"></div>

      <div class="labels-group">
        <div class="section-title">More</div>
        <button
          v-for="label in otherLabels"
          :key="label.id"
          class="label-item"
          :class="{ active: isActive(label.id) }"
          @click="selectLabel(label.id)"
        >
          <span class="label-icon">{{ getLabelIcon(label.id) }}</span>
          <span class="label-name">{{ label.name }}</span>
          <span v-if="label.count > 0" class="label-count">{{ label.count }}</span>
        </button>
      </div>
    </nav>

    <div class="sidebar-footer">
      <button class="settings-btn">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="3" />
          <path d="M12 1v6m0 6v6M4.22 4.22l4.24 4.24m3.08 3.08l4.24 4.24M1 12h6m6 0h6m-16.78 7.78l4.24-4.24m3.08-3.08l4.24-4.24" />
        </svg>
      </button>
      <button class="help-btn">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10" />
          <path d="M12 16v-4M12 8h.01" />
        </svg>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 256px;
  background: #fff;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  padding: 16px 0;
  overflow-y: auto;
}

.compose-btn-container {
  padding: 0 16px 16px;
}

.compose-btn {
  width: 100%;
  padding: 12px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 24px;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s;
  box-shadow: 0 2px 4px rgba(102, 126, 234, 0.3);
}

.compose-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(102, 126, 234, 0.4);
}

.labels-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 0 8px;
}

.labels-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.section-title {
  padding: 12px 12px 8px;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  color: #6b7280;
  letter-spacing: 0.5px;
}

.label-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: transparent;
  border: none;
  border-radius: 0 24px 24px 0;
  cursor: pointer;
  color: #374151;
  font-size: 14px;
  transition: all 0.2s;
}

.label-item:hover {
  background: #f3f4f6;
}

.label-item.active {
  background: #e0e7ff;
  color: #667eea;
  font-weight: 500;
}

.label-icon {
  flex: 0 0 24px;
  text-align: center;
  font-size: 18px;
}

.label-name {
  flex: 1;
  text-align: left;
}

.label-count {
  flex: 0 0 auto;
  padding: 2px 6px;
  background: #f3f4f6;
  border-radius: 12px;
  font-size: 12px;
  color: #6b7280;
  font-weight: 500;
}

.label-item.active .label-count {
  background: transparent;
  color: #667eea;
}

.labels-divider {
  height: 1px;
  background: #e5e7eb;
  margin: 8px 12px;
}

.sidebar-footer {
  display: flex;
  gap: 8px;
  padding: 16px;
  border-top: 1px solid #e5e7eb;
}

.settings-btn,
.help-btn {
  flex: 1;
  padding: 8px;
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.settings-btn:hover,
.help-btn:hover {
  background: #f9fafb;
  color: #1f2937;
}
</style>
