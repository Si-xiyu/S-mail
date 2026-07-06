<script setup lang="ts">
import { ElMessage, ElNotification } from 'element-plus'
import { ref, onBeforeUnmount, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as apiClient from '../api/client'
import TopBar from '../components/TopBar.vue'
import Sidebar from '../components/Sidebar.vue'
import MailList from '../components/MailList.vue'
import MailDetailDrawer from '../components/MailDetailDrawer.vue'
import WelcomePanel from '../components/WelcomePanel.vue'
import ComposeDialog from '../components/ComposeDialog.vue'
import SettingsDialog from '../components/SettingsDialog.vue'
import { useMailStore } from '../stores/mailStore'

const mailStore = useMailStore()
const router = useRouter()
const selectedMailId = ref<string | null>(null)
const showCompose = ref(false)
const showSettings = ref(false)
const syncing = ref(false)
const lastSyncedAt = ref<Date | null>(null)
const savedPollInterval = Number(localStorage.getItem('smartmail_poll_interval'))
const pollInterval = ref([15, 30, 60, 120].includes(savedPollInterval) ? savedPollInterval : 30)
let pollTimer: number | undefined
let lastPollCursor = ''
let synchronizationInitialized = false

const schedulePolling = () => {
  window.clearInterval(pollTimer)
  pollTimer = window.setInterval(() => void pollMailbox(false), pollInterval.value * 1000)
}

const pollMailbox = async (manual: boolean) => {
  if (syncing.value || !mailStore.user) return
  syncing.value = true
  try {
    const notification = await apiClient.pollNotifications(lastPollCursor || undefined)
    mailStore.applyNotificationCounts(notification)
    lastPollCursor = notification.cursor
    lastSyncedAt.value = new Date()
    if (!synchronizationInitialized) {
      synchronizationInitialized = true
      await mailStore.refreshCurrent()
    } else if (notification.newMailCount > 0) {
      await mailStore.refreshCurrent()
      ElNotification({
        title: '收到新邮件',
        message: `${notification.newMailCount} 封新邮件已同步`,
        type: 'success',
        duration: 3500
      })
    } else if (manual) {
      await mailStore.refreshCurrent()
      ElMessage.success('邮箱已同步')
    }
  } catch (error) {
    if (manual) {
      ElMessage.error(error instanceof Error ? error.message : '同步邮箱失败')
    }
  } finally {
    syncing.value = false
  }
}

// 初始化用户信息
onMounted(() => {
  window.addEventListener('smartmail:unauthorized', handleUnauthorized)
  window.addEventListener('smartmail:draft-changed', handleDraftChanged)
  mailStore.refreshDraftCount()
  void pollMailbox(false)
  schedulePolling()
})

onBeforeUnmount(() => {
  window.removeEventListener('smartmail:unauthorized', handleUnauthorized)
  window.removeEventListener('smartmail:draft-changed', handleDraftChanged)
  window.clearInterval(pollTimer)
})

watch(pollInterval, value => {
  localStorage.setItem('smartmail_poll_interval', String(value))
  schedulePolling()
})

const handleUnauthorized = () => {
  window.clearInterval(pollTimer)
  mailStore.logout()
  router.replace('/auth/login')
}

const handleDraftChanged = () => mailStore.refreshDraftCount()

const handleSelectMail = (mailId: string) => {
  selectedMailId.value = mailId
}

const handleComposeBtnClick = () => {
  showCompose.value = true
}

</script>

<template>
  <div class="mail-app">
    <TopBar />

    <div class="main-container">
      <Sidebar
        @compose-click="handleComposeBtnClick"
        @draft-click="handleComposeBtnClick"
        @settings-click="showSettings = true"
      />

      <MailList
        :selected-mail-id="selectedMailId"
        @select-mail="handleSelectMail"
      />

      <div class="right-panel">
        <WelcomePanel
          v-if="!selectedMailId"
          @compose-click="handleComposeBtnClick"
        />
        <MailDetailDrawer
          v-else
          :mail-id="selectedMailId"
          inline
          @close="selectedMailId = null"
        />
      </div>
    </div>

    <ComposeDialog v-model="showCompose" />
    <SettingsDialog
      v-model="showSettings"
      v-model:poll-interval="pollInterval"
      :last-synced-at="lastSyncedAt"
      :syncing="syncing"
      @sync-now="pollMailbox(true)"
    />
  </div>
</template>

<style scoped>
.mail-app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 0;
}

.right-panel {
  flex: 1;
  min-width: 0;
  border-left: 1px solid #e0e0e0;
  background: #fbfbfa;
  overflow-y: auto;
}
</style>
