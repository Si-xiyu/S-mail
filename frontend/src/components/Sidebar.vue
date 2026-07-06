<script setup lang="ts">
import { useMailStore } from '../stores/mailStore'
import { computed, ref } from 'vue'

const mailStore = useMailStore()
const showAddTagInput = ref(false)
const newTagName = ref('')
const hoveredLabelId = ref<string | null>(null)
const emit = defineEmits<{
  composeClick: []
  draftClick: []
  settingsClick: []
}>()

// 系统自带标签的 ID 列表
const SYSTEM_LABEL_IDS = ['INBOX', 'STARRED', 'SENT', 'DRAFTS', 'TRASH', 'SPAM', 'JUNK']

// 系统自带标签（按照指定的顺序）
const systemLabels = computed(() => {
  return SYSTEM_LABEL_IDS
    .map(id => mailStore.labels.find(l => l.id === id))
    .filter(l => l !== undefined) as typeof mailStore.labels
})

// 用户个性化标签
const customLabels = computed(() => {
  return mailStore.labels.filter(l => !SYSTEM_LABEL_IDS.includes(l.id))
})
const isProtectedLabel = (name: string) => name === 'Other' || name === 'Junk Mail'

const isActive = (labelId: string) => {
  return mailStore.currentLabel === labelId
}

const selectLabel = (labelId: string) => {
  if (labelId === 'DRAFTS') {
    emit('draftClick')
    return
  }
  mailStore.selectLabel(labelId)
}

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
    SPAM: '🚫',
    JUNK: '🚫'
  }
  return icons[labelId] || '📁'
}

const handleAddTag = async () => {
  if (!newTagName.value.trim()) {
    alert('Please enter a tag name')
    return
  }

  try {
    await mailStore.addLabel(newTagName.value.trim())
    newTagName.value = ''
    showAddTagInput.value = false
  } catch (err) {
    alert('Failed to add tag: ' + (err instanceof Error ? err.message : 'Unknown error'))
  }
}

const handleCancelAddTag = () => {
  newTagName.value = ''
  showAddTagInput.value = false
}

const handleDeleteLabel = async (labelId: string) => {
  if (confirm(`Delete label "${mailStore.labels.find(l => l.id === labelId)?.name}"?`)) {
    try {
      await mailStore.deleteLabel(labelId)
      hoveredLabelId.value = null
    } catch (err) {
      alert('Failed to delete label: ' + (err instanceof Error ? err.message : 'Unknown error'))
    }
  }
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
        <span>Compose</span>
      </button>
    </div>

    <nav class="labels-section">
      <!-- 系统自带标签 -->
      <div class="labels-group">
        <div class="group-title">System Tags</div>
        <button
          v-for="label in systemLabels"
          :key="label.id"
          class="label-item"
          :class="{ active: isActive(label.id) }"
          @click="selectLabel(label.id)"
        >
          <span class="label-icon">{{ getLabelIcon(label.id) }}</span>
          <span class="label-name">{{ label.name }}</span>
          <span v-if="mailStore.getUnreadCount(label.id) > 0" class="label-count">{{ mailStore.getUnreadCount(label.id) }}</span>
        </button>
      </div>

      <!-- 用户个性化标签 -->
      <div v-if="customLabels.length > 0" class="labels-group">
        <div class="group-title">Custom Tags</div>
        <button
          v-for="label in customLabels"
          :key="label.id"
          class="label-item custom-label"
          :class="{ active: isActive(label.id), hovered: hoveredLabelId === label.id }"
          @click="selectLabel(label.id)"
          @mouseenter="hoveredLabelId = label.id"
          @mouseleave="hoveredLabelId = null"
        >
          <span class="label-icon">{{ getLabelIcon(label.id) }}</span>
          <span class="label-name">{{ label.name }}</span>
          <span v-if="mailStore.getUnreadCount(label.id) > 0" class="label-count">{{ mailStore.getUnreadCount(label.id) }}</span>
          <button
            v-if="hoveredLabelId === label.id && !isProtectedLabel(label.name)"
            class="label-delete-btn"
            @click.stop="handleDeleteLabel(label.id)"
            title="Delete label"
          >
            ✕
          </button>
        </button>
      </div>
    </nav>

    <div class="sidebar-footer">
      <div v-if="showAddTagInput" class="add-tag-input-container">
        <input
          v-model="newTagName"
          type="text"
          class="tag-input"
          placeholder="Tag name"
          @keyup.enter="handleAddTag"
          @keyup.esc="handleCancelAddTag"
          autofocus
        />
        <div class="input-actions">
          <button class="action-btn confirm" @click="handleAddTag" title="Confirm">✓</button>
          <button class="action-btn cancel" @click="handleCancelAddTag" title="Cancel">✕</button>
        </div>
      </div>
      <button v-else class="add-tag-btn" @click="showAddTagInput = true" title="Add Tag">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 5v14M5 12h14" />
        </svg>
        <span>Add Tag</span>
      </button>
      <div class="footer-buttons">
        <button class="settings-btn" title="Settings" @click="emit('settingsClick')">
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
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fbfbfa;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  padding: 16px 0;
  overflow-y: auto;
}

.compose-btn-container {
  padding: 0 12px 16px;
}

.compose-btn {
  width: 100%;
  padding: 9px 12px;
  background: #37352f;
  color: #fff;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  transition: background 0.15s;
}

.compose-btn:hover {
  background: #2b2925;
}

.labels-section {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 0 8px;
}

.labels-group {
  display: flex;
  flex-direction: column;
  gap: 1px;
  margin-bottom: 12px;
}

.group-title {
  padding: 8px 12px 6px;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  color: rgba(55, 53, 47, 0.4);
  letter-spacing: 0.6px;
}

.section-title {
  display: none;
}

.labels-divider {
  display: none;
}

.label-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 7px 12px;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  color: #37352f;
  font-size: 13px;
  transition: background 0.1s;
  width: 100%;
  justify-content: flex-start;
  position: relative;
}

.label-item:hover {
  background: #f4f4f4;
}

.label-item.active {
  background: #f0efed;
  font-weight: 500;
}

.label-item.custom-label.hovered {
  padding-right: 4px;
}

.label-icon {
  flex: 0 0 18px;
  text-align: center;
  font-size: 14px;
  opacity: 0.65;
}

.label-name {
  flex: 1;
  text-align: left;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: inherit;
}

.label-count {
  flex: 0 0 auto;
  padding: 2px 6px;
  background: transparent;
  border-radius: 3px;
  font-size: 11px;
  color: rgba(55, 53, 47, 0.45);
  font-weight: 400;
}

.label-delete-btn {
  flex: 0 0 auto;
  width: 22px;
  height: 22px;
  padding: 0;
  background: none;
  border: none;
  color: rgba(55, 53, 47, 0.4);
  cursor: pointer;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 3px;
  transition: all 0.15s;
}

.label-delete-btn:hover {
  background: #f4f4f4;
  color: #e03e3e;
}

.sidebar-footer {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-top: 1px solid #e0e0e0;
  margin-top: auto;
}

.add-tag-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 7px 12px;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  color: rgba(55, 53, 47, 0.5);
  font-size: 13px;
  transition: background 0.1s;
  width: 100%;
  justify-content: flex-start;
}

.add-tag-btn:hover {
  background: #f4f4f4;
  color: #37352f;
}

.add-tag-btn svg {
  flex: 0 0 18px;
  opacity: 0.65;
}

.add-tag-input-container {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 4px 0;
}

.tag-input {
  padding: 7px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s;
  width: 100%;
  box-sizing: border-box;
  color: #37352f;
}

.tag-input:focus {
  border-color: #37352f;
}

.input-actions {
  display: flex;
  gap: 6px;
}

.input-actions .action-btn {
  flex: 1;
  padding: 5px 8px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
  transition: background 0.1s;
  color: #37352f;
}

.input-actions .action-btn:hover {
  background: #f4f4f4;
}

.footer-buttons {
  display: flex;
  gap: 8px;
}

.settings-btn,
.help-btn {
  flex: 1;
  padding: 7px;
  background: transparent;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  color: rgba(55, 53, 47, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.1s;
}

.settings-btn:hover,
.help-btn:hover {
  background: #f4f4f4;
  color: #37352f;
}
</style>
