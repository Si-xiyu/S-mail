<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getMailDetail, listMailbox, login, register, runAiTask, sendMail } from './api/client'
import type { AgentTaskResponse, MailDetail, MailboxItem } from './types/mail'

const folders = [
  { key: 'INBOX', label: '收件箱' },
  { key: 'SENT', label: '已发送' },
  { key: 'TRASH', label: '垃圾箱' }
]

const currentFolder = ref('INBOX')
const user = ref({ id: 0, email: 'demo@smartmail.local', username: 'Demo' })
const mails = ref<MailboxItem[]>([])
const selected = ref<MailDetail | null>(null)
const aiResult = ref<AgentTaskResponse | null>(null)
const loading = ref(false)

const authForm = reactive({
  email: 'demo@smartmail.local',
  username: 'Demo',
  password: '123456'
})

const compose = reactive({
  to: 'alice@smartmail.local',
  cc: '',
  subject: 'SmartMail MVP 联调',
  contentText: '这是一封用于验证 SmartMail 前后端和 Agent MVP 的测试邮件。'
})

const selectedPreview = computed(() => selected.value?.contentText ?? '选择一封邮件查看详情')

async function authenticate(mode: 'login' | 'register') {
  user.value = mode === 'login'
    ? await login(authForm.email, authForm.password)
    : await register(authForm.email, authForm.username, authForm.password)
  ElMessage.success(`${mode === 'login' ? '登录' : '注册'}完成`)
  await refreshMailbox()
}

async function refreshMailbox() {
  loading.value = true
  mails.value = await listMailbox(currentFolder.value)
  selected.value = mails.value.length ? await getMailDetail(mails.value[0].mailId) : null
  aiResult.value = null
  loading.value = false
}

async function selectMail(item: MailboxItem) {
  selected.value = await getMailDetail(item.mailId)
  aiResult.value = null
}

async function submitMail() {
  await sendMail({
    to: compose.to.split(',').map((item) => item.trim()).filter(Boolean),
    cc: compose.cc.split(',').map((item) => item.trim()).filter(Boolean),
    subject: compose.subject,
    contentText: compose.contentText
  })
  ElMessage.success('邮件已提交')
  currentFolder.value = 'SENT'
  await refreshMailbox()
}

async function ai(task: 'summary' | 'reply-draft' | 'analyze') {
  if (!selected.value) return
  aiResult.value = await runAiTask(selected.value.mailId, task)
}

onMounted(refreshMailbox)
</script>

<template>
  <main class="app-shell">
    <aside class="sidebar">
      <div class="brand">SmartMail</div>
      <div class="account">
        <strong>{{ user.username }}</strong>
        <span>{{ user.email }}</span>
      </div>
      <div class="auth-panel">
        <el-input v-model="authForm.email" size="small" placeholder="邮箱" />
        <el-input v-model="authForm.username" size="small" placeholder="用户名" />
        <el-input v-model="authForm.password" size="small" placeholder="密码" show-password />
        <div class="auth-actions">
          <el-button size="small" type="primary" @click="authenticate('login')">登录</el-button>
          <el-button size="small" @click="authenticate('register')">注册</el-button>
        </div>
      </div>
      <nav class="folders">
        <button
          v-for="folder in folders"
          :key="folder.key"
          :class="{ active: currentFolder === folder.key }"
          @click="currentFolder = folder.key; refreshMailbox()"
        >
          {{ folder.label }}
        </button>
      </nav>
    </aside>

    <section class="mail-list">
      <div class="section-head">
        <h2>{{ folders.find((folder) => folder.key === currentFolder)?.label }}</h2>
        <el-button size="small" @click="refreshMailbox">刷新</el-button>
      </div>
      <div v-loading="loading" class="list-body">
        <button
          v-for="mail in mails"
          :key="mail.itemId"
          class="mail-row"
          :class="{ unread: !mail.read, selected: selected?.mailId === mail.mailId }"
          @click="selectMail(mail)"
        >
          <span class="row-top">
            <strong>{{ mail.subject }}</strong>
            <em>{{ mail.priority }}</em>
          </span>
          <span>{{ mail.senderEmail }}</span>
          <small>{{ mail.preview }}</small>
        </button>
      </div>

      <div class="compose">
        <h3>写邮件</h3>
        <el-input v-model="compose.to" placeholder="收件人，逗号分隔" />
        <el-input v-model="compose.cc" placeholder="抄送，逗号分隔" />
        <el-input v-model="compose.subject" placeholder="主题" />
        <el-input v-model="compose.contentText" type="textarea" :rows="4" placeholder="正文" />
        <el-button type="primary" @click="submitMail">发送</el-button>
      </div>
    </section>

    <section class="reader">
      <div v-if="selected" class="reader-content">
        <div class="reader-title">
          <div>
            <h1>{{ selected.subject }}</h1>
            <p>{{ selected.senderEmail }} -> {{ selected.recipients.join(', ') }}</p>
          </div>
          <span class="priority">{{ selected.priority }}</span>
        </div>
        <article>{{ selectedPreview }}</article>
        <div class="ai-actions">
          <el-button @click="ai('summary')">AI 摘要</el-button>
          <el-button @click="ai('reply-draft')">回复草稿</el-button>
          <el-button @click="ai('analyze')">优先级分析</el-button>
        </div>
        <pre v-if="aiResult" class="ai-panel">{{ JSON.stringify(aiResult.result, null, 2) }}</pre>
      </div>
      <div v-else class="empty">暂无邮件</div>
    </section>
  </main>
</template>
