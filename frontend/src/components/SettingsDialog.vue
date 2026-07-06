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
.settings-section { padding: 4px 0 18px; }
.settings-section + .settings-section { border-top: 1px solid #e5e7eb; padding-top: 18px; }
.settings-section h3 { margin: 0 0 14px; font-size: 15px; color: #111827; }
.setting-row { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin: 11px 0; color: #4b5563; }
.setting-row strong { color: #111827; font-weight: 500; overflow-wrap: anywhere; }
.setting-row select { padding: 7px 28px 7px 10px; border: 1px solid #d1d5db; border-radius: 7px; background: #fff; }
.sync-button, .close-button { border: 0; border-radius: 7px; padding: 8px 15px; color: #fff; background: #4f46e5; cursor: pointer; }
.sync-button:disabled { opacity: .55; cursor: default; }
.close-button { min-width: 76px; }
</style>
