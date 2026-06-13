<script setup lang="ts">
import { ref } from 'vue'
import TopBar from './components/TopBar.vue'
import Sidebar from './components/Sidebar.vue'
import MailList from './components/MailList.vue'
import MailDetail from './components/MailDetail.vue'
import ComposeDialog from './components/ComposeDialog.vue'
import { useMailStore } from './stores/mailStore'

const mailStore = useMailStore()
const selectedMailId = ref<string | null>(null)
const showCompose = ref(false)

const handleSelectMail = (mailId: string) => {
  selectedMailId.value = mailId
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
      <MailDetail :mail-id="selectedMailId" />
    </div>

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
}
</style>
