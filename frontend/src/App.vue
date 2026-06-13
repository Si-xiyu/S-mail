<script setup lang="ts">
import { ref } from 'vue'
import TopBar from './components/TopBar.vue'
import Sidebar from './components/Sidebar.vue'
import MailList from './components/MailList.vue'
import MailDetailDrawer from './components/MailDetailDrawer.vue'
import ComposeDialog from './components/ComposeDialog.vue'
import { useMailStore } from './stores/mailStore'

const mailStore = useMailStore()
const selectedMailId = ref<string | null>(null)
const showMailDetail = ref(false)
const showCompose = ref(false)

const handleSelectMail = (mailId: string) => {
  selectedMailId.value = mailId
  showMailDetail.value = true
}

const handleCloseMailDetail = () => {
  showMailDetail.value = false
  selectedMailId.value = null
}

const handleComposeBtnClick = () => {
  showCompose.value = true
}
</script>

<template>
  <div id="app" class="app">
    <TopBar />

    <div class="main-container">
      <Sidebar @compose-click="handleComposeBtnClick" />
      <MailList @select-mail="handleSelectMail" />
    </div>

    <MailDetailDrawer
      v-if="showMailDetail"
      :mail-id="selectedMailId"
      @close="handleCloseMailDetail"
    />

    <ComposeDialog v-model="showCompose" />
  </div>
</template>

<style scoped>
.app {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f9fafb;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', 'Oxygen', 'Ubuntu', 'Cantarell', 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
}

#app {
  height: 100vh;
}

.main-container {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 0;
}
</style>
