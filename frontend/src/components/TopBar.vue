<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useMailStore } from '../stores/mailStore'
import { ref, onMounted, onUnmounted } from 'vue'

const router = useRouter()
const mailStore = useMailStore()
const searchQuery = ref('')
const userMenuOpen = ref(false)
const userMenuRef = ref<HTMLDivElement | null>(null)

const handleSearch = () => {
  console.log('Search for:', searchQuery.value)
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
        />
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
  padding: 0 16px;
  height: 64px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  gap: 16px;
}

.topbar-left {
  flex: 0 0 auto;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  color: #1f2937;
}

.topbar-center {
  flex: 1;
  display: flex;
  justify-content: center;
  max-width: 400px;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 24px;
  background: #f9fafb;
  width: 100%;
}

.search-box input {
  flex: 1;
  border: none;
  background: transparent;
  outline: none;
  font-size: 14px;
}

.search-box input::placeholder {
  color: #9ca3af;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.icon-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #6b7280;
  border-radius: 50%;
  transition: all 0.2s;
}

.icon-button:hover {
  background: #f3f4f6;
  color: #1f2937;
}

.user-menu {
  position: relative;
}

.avatar-button {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid transparent;
  cursor: pointer;
  overflow: hidden;
  padding: 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: 600;
  font-size: 14px;
  transition: all 0.2s;
}

.avatar-button:hover {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
}

.avatar-button.active {
  border-color: #667eea;
  box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.2);
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
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 200px;
  z-index: 1000;
  margin-top: 8px;
}

.user-info {
  padding: 12px 16px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-info strong {
  color: #1f2937;
  font-size: 14px;
}

.user-info span {
  color: #6b7280;
  font-size: 12px;
}

.user-dropdown button {
  display: block;
  width: 100%;
  padding: 12px 16px;
  border: none;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: #1f2937;
  font-size: 14px;
  transition: all 0.2s;
}

.user-dropdown button:hover {
  background: #f3f4f6;
}

.logout-button {
  color: #ef4444 !important;
}

.logout-button:hover {
  background: #fee2e2 !important;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from {
  opacity: 0;
  transform: translateY(-8px);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>
