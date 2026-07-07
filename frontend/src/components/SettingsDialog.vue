<script setup lang="ts">
import { computed } from 'vue'
import { useMailStore } from '../stores/mailStore'

const props = defineProps<{
  modelValue: boolean
  pollInterval: number
  lastSyncedAt: Date | null
  syncing: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  'update:pollInterval': [value: number]
  syncNow: []
}>()

const mailStore = useMailStore()
const formattedLastSync = computed(() => props.lastSyncedAt
  ? props.lastSyncedAt.toLocaleString()
  : '尚未同步')

const updateInterval = (event: Event) => {
  emit('update:pollInterval', Number((event.target as HTMLSelectElement).value))
}

const toggleAi = async () => {
  const next = !mailStore.aiEnabled
  try {
    await mailStore.updateUserSettings({ aiEnabled: next })
  } catch (err) {
    console.error('Failed to update AI setting:', err)
  }
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="基础设置"
    width="460px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <section class="settings-section">
      <h3>账户</h3>
      <div class="setting-row">
        <span>用户名</span>
        <strong>{{ mailStore.user?.name || '-' }}</strong>
      </div>
      <div class="setting-row">
        <span>邮箱</span>
        <strong>{{ mailStore.user?.email || '-' }}</strong>
      </div>
    </section>

    <section class="settings-section">
      <h3>AI 功能</h3>
      <label class="setting-row toggle-row">
        <span>启用 AI 助手</span>
        <button
          type="button"
          class="toggle"
          :class="{ on: mailStore.aiEnabled }"
          @click="toggleAi"
        >
          <span class="thumb"></span>
        </button>
      </label>
      <p class="settings-hint">
        关闭后 Agent 助手将不可用，基础邮箱功能不受影响。
      </p>
    </section>

    <section class="settings-section">
      <h3>邮件同步</h3>
      <label class="setting-row">
        <span>自动检查间隔</span>
        <select :value="pollInterval" @change="updateInterval">
          <option :value="15">15 秒</option>
          <option :value="30">30 秒</option>
          <option :value="60">1 分钟</option>
          <option :value="120">2 分钟</option>
        </select>
      </label>
      <div class="setting-row">
        <span>上次同步</span>
        <span>{{ formattedLastSync }}</span>
      </div>
      <button type="button" class="sync-button" :disabled="syncing" @click="emit('syncNow')">
        {{ syncing ? '同步中…' : '立即同步' }}
      </button>
    </section>

    <template #footer>
      <button type="button" class="close-button" @click="emit('update:modelValue', false)">完成</button>
    </template>
  </el-dialog>
</template>

<style scoped>
.settings-section { padding: 8px 0 20px; }
.settings-section + .settings-section { border-top: 1px solid #e0e0e0; padding-top: 20px; }
.settings-section h3 { margin: 0 0 16px; font-size: 14px; font-weight: 500; color: #37352f; }
.setting-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin: 12px 0; color: rgba(55, 53, 47, 0.55); font-size: 13px; }
.setting-row strong { color: #37352f; font-weight: 500; overflow-wrap: anywhere; font-size: 13px; }
.setting-row select { padding: 6px 28px 6px 10px; border: 1px solid #e0e0e0; border-radius: 4px; background: #fff; font-size: 13px; color: #37352f; cursor: pointer; }
.settings-hint { margin: 8px 0 0; font-size: 12px; color: rgba(55, 53, 47, 0.45); line-height: 1.5; }
.toggle-row { cursor: pointer; }
.toggle {
  width: 40px;
  height: 22px;
  border-radius: 11px;
  border: none;
  background: #d1d5db;
  position: relative;
  cursor: pointer;
  transition: background 0.15s;
  padding: 0;
}
.toggle.on { background: #37352f; }
.toggle .thumb {
  position: absolute;
  top: 2px;
  left: 2px;
  width: 18px;
  height: 18px;
  border-radius: 50%;
  background: #fff;
  transition: transform 0.15s;
  display: block;
}
.toggle.on .thumb { transform: translateX(18px); }
.sync-button, .close-button { border: 0; border-radius: 4px; padding: 8px 16px; color: #fff; background: #37352f; cursor: pointer; font-size: 13px; transition: background 0.15s; }
.sync-button:hover:not(:disabled), .close-button:hover { background: #2b2925; }
.sync-button:disabled { opacity: .45; cursor: default; }
.close-button { min-width: 72px; }
</style>
