<script setup lang="ts">
import { useMailStore } from '../stores/mailStore'

const mailStore = useMailStore()

const emit = defineEmits<{
  composeClick: []
}>()

const greeting = () => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 18) return 'Good afternoon'
  return 'Good evening'
}

const avatarChar = () => {
  return mailStore.user?.name?.charAt(0).toUpperCase() || 'U'
}
</script>

<template>
  <div class="welcome-panel">
    <div class="welcome-content">
      <div class="avatar-circle">{{ avatarChar() }}</div>
      <h2>{{ greeting() }}, {{ mailStore.user?.name || 'User' }}</h2>
      <p class="email-text">{{ mailStore.user?.email }}</p>

      <div class="quick-actions">
        <button class="action-card" @click="emit('composeClick')">
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
            <line x1="12" y1="5" x2="12" y2="19" />
            <line x1="5" y1="12" x2="19" y2="12" />
          </svg>
          <span>Write a new email</span>
        </button>
      </div>

      <div class="tip-section">
        <p class="tip-text">Select an email from the list to view its contents here.</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.welcome-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: #fbfbfa;
}

.welcome-content {
  text-align: center;
  padding: 48px 32px;
  max-width: 340px;
}

.avatar-circle {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: #f0efed;
  color: #37352f;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 20px;
}

h2 {
  margin: 0 0 6px;
  font-size: 20px;
  font-weight: 600;
  color: #37352f;
}

.email-text {
  margin: 0 0 32px;
  font-size: 13px;
  color: rgba(55, 53, 47, 0.5);
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.action-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #37352f;
  transition: background 0.1s, border-color 0.1s;
}

.action-card:hover {
  background: #f4f4f4;
  border-color: #c0c0c0;
}

.action-card svg {
  color: rgba(55, 53, 47, 0.45);
  flex-shrink: 0;
}

.tip-section {
  margin-top: 36px;
}

.tip-text {
  margin: 0;
  font-size: 12px;
  color: rgba(55, 53, 47, 0.35);
  line-height: 1.6;
}
</style>
