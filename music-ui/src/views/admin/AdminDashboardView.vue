<script setup>
import { ref, onMounted } from 'vue'
import adminRequest from '../../utils/adminRequest'

const loading = ref(true)
const stats = ref({
  userCount: 0,
  songCount: 0,
  pendingSongCount: 0,
})

async function loadStats() {
  loading.value = true
  try {
    const [userRes, songRes, pendingRes] = await Promise.all([
      adminRequest.get('/user/list-admin', { params: { pageNum: 1, pageSize: 1 } }),
      adminRequest.get('/song/list-admin', { params: { pageNum: 1, pageSize: 1, status: 1 } }),
      adminRequest.get('/song/list-admin', { params: { pageNum: 1, pageSize: 1, status: 0 } }),
    ])
    const code = (r) => r.data?.code === 2000 ? r.data?.data : null
    const userPage = code(userRes)
    const songPage = code(songRes)
    const pendingPage = code(pendingRes)
    stats.value = {
      userCount: userPage?.total ?? 0,
      songCount: songPage?.total ?? 0,
      pendingSongCount: pendingPage?.total ?? 0,
    }
  } catch (_) {
    stats.value = { userCount: 0, songCount: 0, pendingSongCount: 0 }
  } finally {
    loading.value = false
  }
}

onMounted(loadStats)
</script>

<template>
  <div class="dashboard">
    <h2 class="page-title">仪表盘</h2>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else class="stats-grid">
      <div class="stat-card">
        <div class="stat-value">{{ stats.userCount }}</div>
        <div class="stat-label">用户总数</div>
      </div>
      <div class="stat-card">
        <div class="stat-value">{{ stats.songCount }}</div>
        <div class="stat-label">已上架歌曲</div>
      </div>
      <div class="stat-card highlight">
        <div class="stat-value">{{ stats.pendingSongCount }}</div>
        <div class="stat-label">待审核歌曲</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 900px;
}

.page-title {
  margin: 0 0 24px;
  font-size: 1.25rem;
  font-weight: 600;
}

.loading {
  color: #94a3b8;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 20px;
}

.stat-card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 12px;
  padding: 24px;
}

.stat-card.highlight {
  border-color: #38bdf8;
  background: rgba(56, 189, 248, 0.08);
}

.stat-value {
  font-size: 2rem;
  font-weight: 700;
  color: #e2e8f0;
}

.stat-card.highlight .stat-value {
  color: #38bdf8;
}

.stat-label {
  margin-top: 8px;
  color: #94a3b8;
  font-size: 0.9rem;
}
</style>
