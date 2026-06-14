<script setup lang="ts">
import { ref } from 'vue'
import TopBar from './components/TopBar.vue'
import Sidebar from './components/Sidebar.vue'
import MailList from './components/MailList.vue'
import MailDetailPage from './components/MailDetailPage.vue'
import ComposeDialog from './components/ComposeDialog.vue'
import { useMailStore } from './stores/mailStore'

// 页面模式枚举
enum PageMode {
  LIST = 'list',
  DETAIL = 'detail'
}

const mailStore = useMailStore()
const pageMode = ref<PageMode>(PageMode.LIST)
const selectedMailId = ref<string | null>(null)
const showCompose = ref(false)

const handleSelectMail = (mailId: string) => {
  selectedMailId.value = mailId
  pageMode.value = PageMode.DETAIL
}

const handleBackFromDetail = () => {
  pageMode.value = PageMode.LIST
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

      <!-- 邮件列表视图 -->
      <MailList v-if="pageMode === 'list'" @select-mail="handleSelectMail" />

      <!-- 邮件详情视图 -->
      <MailDetailPage v-if="pageMode === 'detail'" :mail-id="selectedMailId" @back="handleBackFromDetail" />
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
  gap: 0;
}
</style>
