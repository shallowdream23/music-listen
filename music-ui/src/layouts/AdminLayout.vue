<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAdminAuthStore } from '../store/adminAuth'

const router = useRouter()
const route = useRoute()
const adminAuth = useAdminAuthStore()

const activeMenu = computed(() => route.path)
const userInfo = computed(() => adminAuth.userInfo)

const menuList = [
  { path: '/admin/dashboard', name: '仪表盘' },
  { path: '/admin/users', name: '用户管理' },
  { path: '/admin/songs', name: '歌曲管理' },
  { path: '/admin/albums', name: '专辑管理' },
]

function isActive(path) {
  return activeMenu.value === path || activeMenu.value.startsWith(path + '/')
}

function goTo(path) {
  router.push(path)
}

function goHome() {
  router.push('/')
}

function handleLogout() {
  adminAuth.logout()
  router.push('/admin/login')
}
</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="sidebar-logo">音乐后台</div>
      <nav class="sidebar-nav">
        <a
          v-for="item in menuList"
          :key="item.path"
          :class="['nav-item', { active: isActive(item.path) }]"
          @click="goTo(item.path)"
        >
          <span class="nav-label">{{ item.name }}</span>
        </a>
      </nav>
    </aside>
    <div class="admin-main">
      <header class="admin-header">
        <span class="header-title">{{ menuList.find(m => isActive(m.path))?.name || '管理' }}</span>
        <div class="header-actions">
          <button type="button" class="btn-link" @click="goHome">返回站点</button>
          <span class="user-name">{{ userInfo?.username }}</span>
          <button type="button" class="btn-link danger" @click="handleLogout">退出</button>
        </div>
      </header>
      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #0f172a;
  color: #e2e8f0;
}

.admin-sidebar {
  width: 200px;
  background: #1e293b;
  border-right: 1px solid #334155;
  flex-shrink: 0;
}

.sidebar-logo {
  padding: 20px;
  font-size: 1.1rem;
  font-weight: 600;
  border-bottom: 1px solid #334155;
}

.sidebar-nav {
  padding: 12px 0;
}

.nav-item {
  display: block;
  padding: 12px 20px;
  color: #94a3b8;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.nav-item:hover {
  background: #334155;
  color: #e2e8f0;
}

.nav-item.active {
  background: #334155;
  color: #38bdf8;
  border-right: 3px solid #38bdf8;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.admin-header {
  height: 56px;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #1e293b;
  border-bottom: 1px solid #334155;
}

.header-title {
  font-weight: 600;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.user-name {
  color: #94a3b8;
  font-size: 0.9rem;
}

.btn-link {
  background: none;
  border: none;
  color: #38bdf8;
  cursor: pointer;
  font-size: 0.9rem;
  padding: 4px 8px;
}

.btn-link:hover {
  text-decoration: underline;
}

.btn-link.danger {
  color: #f87171;
}

.admin-content {
  flex: 1;
  padding: 24px;
  overflow: auto;
}
</style>
