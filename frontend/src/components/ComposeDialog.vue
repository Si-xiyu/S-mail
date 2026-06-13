<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useMailStore } from '../stores/mailStore'

const props = withDefaults(defineProps<{
  modelValue: boolean
}>(), {
  modelValue: false
})

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const mailStore = useMailStore()

const form = reactive({
  to: '',
  cc: '',
  bcc: '',
  subject: '',
  content: ''
})

const isMinimized = ref(false)

const handleSend = () => {
  if (!form.to || !form.subject) {
    alert('Please fill in recipient and subject')
    return
  }

  const toList = form.to.split(',').map(e => e.trim()).filter(Boolean)
  const ccList = form.cc.split(',').map(e => e.trim()).filter(Boolean)

  mailStore.sendMail(toList, form.subject, form.content, ccList.length > 0 ? ccList : undefined)

  // Reset form
  form.to = ''
  form.cc = ''
  form.bcc = ''
  form.subject = ''
  form.content = ''

  emit('update:modelValue', false)
}

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleMinimize = () => {
  isMinimized.value = !isMinimized.value
}
</script>

<template>
  <transition name="compose">
    <div v-if="modelValue" class="compose-dialog" :class="{ minimized: isMinimized }">
      <div class="compose-header">
        <div class="header-title">New Message</div>
        <div class="header-actions">
          <button class="icon-btn" @click="handleMinimize">
            {{ isMinimized ? '▲' : '▼' }}
          </button>
          <button class="icon-btn" @click="handleClose">✕</button>
        </div>
      </div>

      <div v-if="!isMinimized" class="compose-body">
        <div class="form-group">
          <input
            v-model="form.to"
            type="email"
            class="form-input"
            placeholder="To"
            multiple
          />
        </div>

        <div class="form-group">
          <input
            v-model="form.cc"
            type="text"
            class="form-input"
            placeholder="Cc"
          />
        </div>

        <div class="form-group">
          <input
            v-model="form.bcc"
            type="text"
            class="form-input"
            placeholder="Bcc"
          />
        </div>

        <div class="form-group">
          <input
            v-model="form.subject"
            type="text"
            class="form-input"
            placeholder="Subject"
          />
        </div>

        <div class="form-group">
          <textarea
            v-model="form.content"
            class="form-textarea"
            placeholder="Compose your message..."
          ></textarea>
        </div>

        <div class="compose-footer">
          <button class="send-btn" @click="handleSend">Send</button>
          <button class="btn-icon">
            <span>📎</span>
          </button>
          <button class="btn-icon">
            <span>😊</span>
          </button>
        </div>
      </div>
    </div>
  </transition>
</template>

<style scoped>
.compose-dialog {
  position: fixed;
  bottom: 0;
  right: 24px;
  width: 400px;
  max-width: calc(100% - 48px);
  background: white;
  border: 1px solid #e5e7eb;
  border-bottom: none;
  border-radius: 8px 8px 0 0;
  box-shadow: 0 -2px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  z-index: 500;
  animation: slideUp 0.3s ease-out;
}

.compose-dialog.minimized {
  width: auto;
}

.compose-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  background: #f9fafb;
  cursor: move;
  user-select: none;
}

.header-title {
  font-weight: 500;
  color: #1f2937;
  font-size: 14px;
}

.header-actions {
  display: flex;
  gap: 4px;
}

.icon-btn {
  background: none;
  border: none;
  cursor: pointer;
  color: #6b7280;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #e5e7eb;
  color: #1f2937;
}

.compose-body {
  display: flex;
  flex-direction: column;
  padding: 0;
  max-height: 500px;
  overflow-y: auto;
}

.form-group {
  border-bottom: 1px solid #e5e7eb;
}

.form-input,
.form-textarea {
  width: 100%;
  border: none;
  padding: 12px 16px;
  font-family: inherit;
  font-size: 14px;
  color: #1f2937;
  outline: none;
  resize: none;
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: #9ca3af;
}

.form-textarea {
  min-height: 200px;
  border-bottom: 1px solid #e5e7eb;
}

.compose-footer {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #fafbfc;
}

.send-btn {
  padding: 8px 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.send-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.btn-icon {
  width: 32px;
  height: 32px;
  border: 1px solid #e5e7eb;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.2s;
  margin-left: auto;
}

.btn-icon:hover {
  background: #f3f4f6;
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.compose-enter-active,
.compose-leave-active {
  transition: all 0.3s ease;
}

.compose-enter-from,
.compose-leave-to {
  transform: translateY(100%);
  opacity: 0;
}
</style>
