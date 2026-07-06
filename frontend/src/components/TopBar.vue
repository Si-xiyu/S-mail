<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useMailStore } from '../stores/mailStore'
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const mailStore = useMailStore()
const searchQuery = ref('')
const userMenuOpen = ref(false)
const userMenuRef = ref<HTMLDivElement | null>(null)

const handleSearch = async () => {
  try {
    await mailStore.searchMailbox(searchQuery.value)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '搜索失败')
  }
}

const clearSearch = async () => {
  searchQuery.value = ''
  await mailStore.searchMailbox('')
}

const handleLogout = () => {
  userMenuOpen.value = false
  mailStore.logout()
  router.push('/auth/login')
}

// 点击菜单按钮
const toggleUserMenu = () => {
  userMenuOpen.value = !userMenuOpen.value
}

// 点击外部关闭菜单
const handleClickOutside = (e: MouseEvent) => {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target as Node)) {
    userMenuOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onUnmounted(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<template>
  <header class="topbar">
    <div class="topbar-left">
      <div class="logo">
        <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="2" y="4" width="20" height="16" rx="2" />
          <path d="m22 6-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 6" />
        </svg>
        <span>SmartMail</span>
      </div>
    </div>

    <div class="topbar-center">
      <div class="search-box">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="11" cy="11" r="8" />
          <path d="m21 21-4.35-4.35" />
        </svg>
        <input
          v-model="searchQuery"
          type="text"
          placeholder="Search mail"
          @keyup.enter="handleSearch"
          @keyup.esc="clearSearch"
        />
        <button v-if="searchQuery" class="clear-search" type="button" @click="clearSearch">✕</button>
      </div>
    </div>

    <div class="topbar-right">
      <button class="icon-button">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="1" />
          <circle cx="19" cy="12" r="1" />
          <circle cx="5" cy="12" r="1" />
        </svg>
      </button>
      <button class="icon-button">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2m0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3m0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22" />
        </svg>
      </button>
      <div class="user-menu" ref="userMenuRef">
        <button class="avatar-button" @click="toggleUserMenu" :class="{ active: userMenuOpen }" type="button">
          <img v-if="mailStore.user?.avatar" :src="mailStore.user.avatar" :alt="mailStore.user?.name" />
          <span v-else class="avatar-placeholder">{{ mailStore.user?.name?.charAt(0).toUpperCase() || 'U' }}</span>
        </button>
        <transition name="dropdown">
          <div v-if="userMenuOpen" class="user-dropdown">
            <div class="user-info">
              <strong>{{ mailStore.user?.name || 'User' }}</strong>
              <span>{{ mailStore.user?.email || 'No email' }}</span>
            </div>
            <button @click="handleLogout" class="logout-button" type="button">Sign out</button>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  height: 52px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  gap: 16px;
}

.topbar-left {
  flex: 0 0 auto;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #37352f;
}

.logo svg {
  color: rgba(55, 53, 47, 0.55);
}

.topbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
  max-width: 480px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  background: #fbfbfa;
  width: 100%;
  transition: border-color 0.15s, background 0.15s;
}

.search-box:focus-within {
  border-color: #b0b0b0;
  background: #fff;
}

.search-box svg {
  color: rgba(55, 53, 47, 0.35);
  flex-shrink: 0;
}

.search-box input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 13px;
  color: #37352f;
}

.search-box input::placeholder {
  color: rgba(55, 53, 47, 0.35);
}

.clear-search { border: 0; color: rgba(55, 53, 47, 0.45); background: transparent; cursor: pointer; font-size: 14px; }

.topbar-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

.icon-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: rgba(55, 53, 47, 0.5);
  border-radius: 4px;
  transition: background 0.1s;
}

.icon-button:hover {
  background: #f4f4f4;
  color: #37352f;
}

.user-menu {
  position: relative;
}

.avatar-button {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  border: none;
  cursor: pointer;
  overflow: hidden;
  padding: 0;
  background: #37352f;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 500;
  font-size: 13px;
  transition: opacity 0.15s;
}

.avatar-button:hover {
  opacity: 0.85;
}

.avatar-button.active {
  opacity: 0.85;
}

.avatar-button img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.avatar-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-dropdown {
  position: absolute;
  top: 100%;
  right: 0;
  background: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.06);
  min-width: 200px;
  z-index: 1000;
  margin-top: 6px;
}

.user-info {
  padding: 12px 16px;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.user-info strong {
  color: #37352f;
  font-size: 13px;
  font-weight: 500;
}

.user-info span {
  color: rgba(55, 53, 47, 0.5);
  font-size: 12px;
}

.user-dropdown button {
  display: block;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: #37352f;
  font-size: 13px;
  transition: background 0.1s;
}

.user-dropdown button:hover {
  background: #f4f4f4;
}

.logout-button {
  color: #e03e3e !important;
}

.logout-button:hover {
  background: #fef2f2 !important;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.15s ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-4px);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
