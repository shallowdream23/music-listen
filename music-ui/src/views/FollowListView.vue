<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useRouter } from 'vue-router'

const router = useRouter()

const loading = ref(false)
const userInfo = ref(null)
const list = ref([])

const fetchData = async () => {
  loading.value = true
  try {
    const userRes = await request.get('/user/getUserInfo')
    userInfo.value = userRes.data?.data || null
    if (!userInfo.value?.id) {
      throw new Error('未获取到用户 ID')
    }
    const res = await request.get('/follow/listFollow', {
      params: { id: userInfo.value.id },
    })
    list.value = res.data?.data || []
  } catch (e) {
    const msg = e.response?.data?.message || e.message || '获取关注列表失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})

const goBack = () => {
  router.back()
}

const goUserHome = (id) => {
  if (id) router.push({ name: 'user-home', params: { id } })
}

const displayName = (item) => {
  const name = item?.user?.username
  if (name != null && String(name).trim() !== '') return name.trim()
  if (item?.userId != null) return `用户 ${item.userId}`
  return '用户'
}
</script>

<template>
  <div class="page">
    <header class="header">
      <div>
        <h1>关注列表</h1>
        <p>查看你关注的用户</p>
      </div>
      <button type="button" class="back-btn" @click="goBack">← 返回</button>
    </header>

    <main class="content">
      <el-card class="card" shadow="hover">
        <div v-if="loading" class="loading">加载中...</div>
        <div v-else-if="!list.length" class="empty">
          暂无关注的用户。
        </div>
        <el-scrollbar v-else class="scroll-area">
          <div
            v-for="item in list"
            :key="item.id"
            class="follow-item"
            @click="goUserHome(item.userId || item.user?.id)"
          >
            <el-avatar :size="40" :src="item.user?.avatarUrl" class="avatar">
              {{ displayName(item).charAt(0).toUpperCase() }}
            </el-avatar>
            <div class="meta">
              <div class="name">{{ displayName(item) }}</div>
            </div>
            <span class="go">进入主页 →</span>
          </div>
        </el-scrollbar>
      </el-card>
    </main>
  </div>
</template>

<style scoped>
.page {
  min-height: 100vh;
  padding: 20px 24px 28px;
  background: radial-gradient(circle at top left, #020617, #020617 40%, #020617);
  color: #e5e7eb;
  box-sizing: border-box;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.header h1 {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 600;
}

.header p {
  margin: 0;
  font-size: 13px;
  color: #94a3b8;
}

.back-btn {
  padding: 8px 14px;
  font-size: 14px;
  font-weight: 500;
  color: #94a3b8;
  background: rgba(30, 41, 59, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.3);
  border-radius: 10px;
  cursor: pointer;
  transition: color 0.2s, border-color 0.2s, background 0.2s;
}

.back-btn:hover {
  color: #e5e7eb;
  border-color: rgba(148, 163, 184, 0.5);
  background: rgba(30, 41, 59, 0.8);
}

.back-btn:focus-visible {
  outline: 2px solid rgba(79, 70, 229, 0.6);
  outline-offset: 2px;
}

.content {
  max-width: 720px;
}

.card {
  border-radius: 16px;
  max-height: 480px;
  background: rgba(15, 23, 42, 0.9) !important;
  border: 1px solid rgba(148, 163, 184, 0.25);
}

.card :deep(.el-card__body) {
  background: rgba(15, 23, 42, 0.9);
  color: #e5e7eb;
}

.loading,
.empty {
  font-size: 13px;
  color: #9ca3af;
}

.scroll-area {
  max-height: 420px;
}

.follow-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 12px;
  margin: 0 4px;
  border-radius: 12px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  cursor: pointer;
  transition: background-color 0.2s;
}

.follow-item:last-of-type {
  border-bottom: none;
}

.follow-item:hover {
  background-color: rgba(30, 41, 59, 0.5);
}


.avatar {
  flex-shrink: 0;
  background: radial-gradient(circle at 30% 20%, #38bdf8, #4f46e5);
  color: #e5e7eb;
}

.meta {
  flex: 1;
  min-width: 0;
}

.name {
  font-size: 14px;
  font-weight: 500;
  color: #e5e7eb;
}

.go {
  font-size: 12px;
  color: #94a3b8;
}
</style>

