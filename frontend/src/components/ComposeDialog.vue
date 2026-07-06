<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { reactive, ref, watch } from 'vue'
import * as apiClient from '../api/client'
import { useMailStore } from '../stores/mailStore'
import type { PendingAttachment } from '../types/mail'

const props = withDefaults(defineProps<{ modelValue: boolean }>(), { modelValue: false })
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const mailStore = useMailStore()

const form = reactive({ to: '', cc: '', bcc: '', subject: '', content: '' })
const attachments = ref<PendingAttachment[]>([])
const isMinimized = ref(false)
const sending = ref(false)
const uploading = ref(false)
let saveTimer: number | undefined

const draftKey = () => `smartmail_draft_${mailStore.user?.id || 'anonymous'}`
const addresses = (value: string) => value.split(',').map(item => item.trim()).filter(Boolean)
const notifyDraftChanged = () => window.dispatchEvent(new CustomEvent('smartmail:draft-changed'))

const saveDraft = () => {
  window.clearTimeout(saveTimer)
  saveTimer = window.setTimeout(() => {
    const hasContent = Object.values(form).some(value => value.trim()) || attachments.value.length > 0
    if (hasContent) {
      localStorage.setItem(draftKey(), JSON.stringify({ ...form, attachments: attachments.value }))
    } else {
      localStorage.removeItem(draftKey())
    }
    notifyDraftChanged()
  }, 250)
}

const restoreDraft = () => {
  const raw = localStorage.getItem(draftKey())
  if (!raw) return
  try {
    const draft = JSON.parse(raw)
    form.to = draft.to || ''
    form.cc = draft.cc || ''
    form.bcc = draft.bcc || ''
    form.subject = draft.subject || ''
    form.content = draft.content || ''
    attachments.value = Array.isArray(draft.attachments) ? draft.attachments : []
  } catch {
    localStorage.removeItem(draftKey())
    notifyDraftChanged()
  }
}

watch(() => props.modelValue, open => {
  if (open) restoreDraft()
})
watch(form, saveDraft, { deep: true })
watch(attachments, saveDraft, { deep: true })

const handleFiles = async (event: Event) => {
  const input = event.target as HTMLInputElement
  const files = Array.from(input.files || [])
  if (!files.length) return
  uploading.value = true
  try {
    for (const file of files) {
      attachments.value.push(await apiClient.uploadPendingAttachment(file))
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '附件上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const removeAttachment = async (attachment: PendingAttachment) => {
  try {
    await apiClient.removePendingAttachment(attachment.pendingAttachmentId)
    attachments.value = attachments.value.filter(item => item.pendingAttachmentId !== attachment.pendingAttachmentId)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '附件移除失败')
  }
}

const clearDraft = () => {
  Object.assign(form, { to: '', cc: '', bcc: '', subject: '', content: '' })
  attachments.value = []
  localStorage.removeItem(draftKey())
  notifyDraftChanged()
}

const handleSend = async () => {
  const to = addresses(form.to)
  if (!to.length || !form.subject.trim() || !form.content.trim()) {
    ElMessage.warning('请填写收件人、主题和正文')
    return
  }
  sending.value = true
  try {
    const result = await mailStore.sendMessage({
      to,
      cc: addresses(form.cc),
      bcc: addresses(form.bcc),
      subject: form.subject.trim(),
      contentText: form.content,
      pendingAttachmentIds: attachments.value.map(item => item.pendingAttachmentId)
    })
    if (result.delivery.failed.length) {
      ElMessage.warning(`邮件已发送，但以下地址投递失败：${result.delivery.failed.join(', ')}`)
    } else {
      ElMessage.success('邮件发送成功')
    }
    clearDraft()
    emit('update:modelValue', false)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '邮件发送失败')
  } finally {
    sending.value = false
  }
}

const discardDraft = async () => {
  await Promise.allSettled(attachments.value.map(item =>
    apiClient.removePendingAttachment(item.pendingAttachmentId)
  ))
  clearDraft()
  emit('update:modelValue', false)
}
</script>

<template>
  <transition name="compose">
    <section v-if="modelValue" class="compose-dialog" :class="{ minimized: isMinimized }">
      <header class="compose-header">
        <strong>新邮件</strong>
        <div>
          <button type="button" class="icon-btn" @click="isMinimized = !isMinimized">{{ isMinimized ? '▲' : '▼' }}</button>
          <button type="button" class="icon-btn" title="保存并关闭" @click="emit('update:modelValue', false)">✕</button>
        </div>
      </header>
      <div v-if="!isMinimized" class="compose-body">
        <input v-model="form.to" class="field" placeholder="收件人，多个地址用逗号分隔" />
        <input v-model="form.cc" class="field" placeholder="抄送" />
        <input v-model="form.bcc" class="field" placeholder="密送" />
        <input v-model="form.subject" class="field" placeholder="主题" />
        <textarea v-model="form.content" class="content" placeholder="撰写邮件"></textarea>

        <div v-if="attachments.length" class="attachment-list">
          <div v-for="attachment in attachments" :key="attachment.pendingAttachmentId" class="attachment">
            <span>{{ attachment.fileName }} · {{ Math.ceil(attachment.fileSize / 1024) }} KB</span>
            <button type="button" @click="removeAttachment(attachment)">移除</button>
          </div>
        </div>

        <footer class="compose-footer">
          <button type="button" class="send-btn" :disabled="sending || uploading" @click="handleSend">
            {{ sending ? '发送中…' : '发送' }}
          </button>
          <label class="attach-btn" :class="{ disabled: uploading }">
            {{ uploading ? '上传中…' : '📎 添加附件' }}
            <input type="file" multiple :disabled="uploading" @change="handleFiles" />
          </label>
          <button type="button" class="discard-btn" @click="discardDraft">丢弃草稿</button>
        </footer>
      </div>
    </section>
  </transition>
</template>

<style scoped>
.compose-dialog { position: fixed; right: 24px; bottom: 0; z-index: 500; width: 560px; max-width: calc(100vw - 32px); background: #fff; border: 1px solid #e0e0e0; border-bottom: 0; border-radius: 6px 6px 0 0; box-shadow: 0 -4px 24px rgba(0, 0, 0, .06); }
.compose-dialog.minimized { width: 300px; }
.compose-header { display: flex; align-items: center; justify-content: space-between; padding: 10px 16px; color: rgba(255, 255, 255, .9); background: #37352f; border-radius: 5px 5px 0 0; font-size: 13px; }
.compose-header strong { font-weight: 500; }
.icon-btn { border: 0; color: inherit; background: transparent; cursor: pointer; padding: 2px 4px; border-radius: 3px; font-size: 14px; opacity: .7; transition: opacity 0.1s; }
.icon-btn:hover { opacity: 1; background: rgba(255, 255, 255, .1); }
.compose-body { display: flex; flex-direction: column; }
.field { border: 0; border-bottom: 1px solid #e0e0e0; padding: 12px 16px; outline: none; font-size: 13px; color: #37352f; }
.field::placeholder { color: rgba(55, 53, 47, 0.35); }
.content { min-height: 240px; padding: 16px; border: 0; resize: vertical; outline: none; font: inherit; font-size: 13px; color: #37352f; line-height: 1.6; }
.content::placeholder { color: rgba(55, 53, 47, 0.35); }
.attachment-list { padding: 0 16px 8px; display: grid; gap: 6px; }
.attachment { display: flex; justify-content: space-between; gap: 12px; padding: 8px 12px; background: #fbfbfa; border: 1px solid #e0e0e0; border-radius: 4px; font-size: 12px; color: #37352f; }
.attachment button, .discard-btn { border: 0; background: transparent; color: rgba(55, 53, 47, 0.5); cursor: pointer; font-size: 12px; transition: color 0.1s; }
.attachment button:hover, .discard-btn:hover { color: #e03e3e; }
.compose-footer { display: flex; align-items: center; gap: 12px; padding: 12px 16px; border-top: 1px solid #e0e0e0; }
.send-btn { padding: 8px 22px; border: 0; border-radius: 4px; background: #37352f; color: #fff; cursor: pointer; font-size: 13px; font-weight: 500; transition: background 0.15s; }
.send-btn:hover:not(:disabled) { background: #2b2925; }
.send-btn:disabled { opacity: .45; cursor: wait; }
.attach-btn { cursor: pointer; color: rgba(55, 53, 47, 0.55); font-size: 12px; padding: 8px 0; transition: color 0.1s; }
.attach-btn:hover:not(.disabled) { color: #37352f; }
.attach-btn.disabled { opacity: .45; cursor: default; }
.attach-btn input { display: none; }
.discard-btn { margin-left: auto; font-size: 12px; }
.compose-enter-active, .compose-leave-active { transition: transform .2s ease, opacity .2s ease; }
.compose-enter-from, .compose-leave-to { transform: translateY(100%); opacity: 0; }
@media (max-width: 640px) { .compose-dialog { right: 8px; width: calc(100vw - 16px); } }
</style>
