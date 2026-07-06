<script setup lang="ts">
import { ref, onBeforeUnmount, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '../components/TopBar.vue'
import Sidebar from '../components/Sidebar.vue'
import MailList from '../components/MailList.vue'
import MailDetailDrawer from '../components/MailDetailDrawer.vue'
import ComposeDialog from '../components/ComposeDialog.vue'
import { useMailStore } from '../stores/mailStore'

const mailStore = useMailStore()
const router = useRouter()
const selectedMailId = ref<string | null>(null)
const showCompose = ref(false)

// 初始化用户信息
onMounted(() => {
  window.addEventListener('smartmail:unauthorized', handleUnauthorized)
})

onBeforeUnmount(() => {
  window.removeEventListener('smartmail:unauthorized', handleUnauthorized)
})

const handleUnauthorized = () => {
  mailStore.logout()
  router.replace('/auth/login')
}

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
      <Sidebar @compose-click="handleComposeBtnClick" />

      <MailList @select-mail="handleSelectMail" />
    </div>

    <ComposeDialog v-model="showCompose" />
    <MailDetailDrawer :mail-id="selectedMailId" @close="selectedMailId = null" />
  </div>
</template>

<style scoped>
.mail-app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f9fafb;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 0;
}
</style>
