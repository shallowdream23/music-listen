<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useAuthStore } from '../store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const userId = computed(() => route.params.id)
const loading = ref(false)
const userInfo = ref(null)
const playlists = ref([])
const loadingPlaylists = ref(false)
const collectingId = ref(null)

// 粉丝数、关注数（仅显示数字，不展示列表）
const stats = ref({ followCount: 0, fansCount: 0 })
// 当前登录用户信息（用于关注按钮与判断是否本人）
const currentUserInfo = ref(null)
const isFollowing = ref(false)
const followLoading = ref(false)

const isSelf = computed(() => {
  return currentUserInfo.value?.id != null && String(currentUserInfo.value.id) === String(userId.value)
})

const fetchUser = async () => {
  if (!userId.value) return
  loading.value = true
  try {
    const res = await request.get(`/user/${userId.value}/public`)
    userInfo.value = res.data?.data || null
    if (userInfo.value) {
      fetchPlaylists()
      fetchTabCount()
      if (authStore.isAuthenticated) {
        fetchCurrentUser()
        fetchIsFollowing()
      }
    }
  } catch (e) {
    const msg = e.response?.data?.message || '用户不存在'
    ElMessage.error(msg)
    userInfo.value = null
  } finally {
    loading.value = false
  }
  if (userInfo.value?.username) {
    document.title = `${userInfo.value.username}的主页 - 在线音乐`
  }
}

const fetchCurrentUser = async () => {
  try {
    const res = await request.get('/user/getUserInfo')
    currentUserInfo.value = res.data?.data || null
  } catch {
    currentUserInfo.value = null
  }
}

const fetchTabCount = async () => {
  if (!userId.value) return
  try {
    const res = await request.get('/follow/tabCount', { params: { id: userId.value } })
    const data = res.data?.data || {}
    stats.value = {
      followCount: data.followCount ?? 0,
      fansCount: data.fansCount ?? 0,
    }
  } catch {
    stats.value = { followCount: 0, fansCount: 0 }
  }
}

const fetchIsFollowing = async () => {
  if (!authStore.isAuthenticated || !userId.value || isSelf.value) return
  try {
    const res = await request.get('/follow/check', { params: { targetId: userId.value } })
    isFollowing.value = res.data?.data?.following === true
  } catch {
    isFollowing.value = false
  }
}

const followUser = async () => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录后关注')
    return
  }
  if (!currentUserInfo.value?.id || !userId.value || isSelf.value) return
  followLoading.value = true
  try {
    await request.post('/follow/add', {
      followerId: currentUserInfo.value.id,
      followeeId: Number(userId.value),
    })
    isFollowing.value = true
    stats.value.fansCount = (stats.value.fansCount || 0) + 1
    ElMessage.success('关注成功')
  } catch (e) {
    const msg = e.response?.data?.message || '关注失败'
    ElMessage.error(msg)
  } finally {
    followLoading.value = false
  }
}

const unfollowUser = async () => {
  if (!currentUserInfo.value?.id || !userId.value) return
  followLoading.value = true
  try {
    await request.post('/follow/delete', {
      followerId: currentUserInfo.value.id,
      followeeId: Number(userId.value),
    })
    isFollowing.value = false
    stats.value.fansCount = Math.max(0, (stats.value.fansCount || 0) - 1)
    ElMessage.success('已取消关注')
  } catch (e) {
    const msg = e.response?.data?.message || '取消失败'
    ElMessage.error(msg)
  } finally {
    followLoading.value = false
  }
}

const fetchPlaylists = async () => {
  if (!userId.value) return
  loadingPlaylists.value = true
  try {
    const res = await request.get('/playlist/list', { params: { id: userId.value } })
    playlists.value = res.data?.data || []
  } catch (e) {
    playlists.value = []
  } finally {
    loadingPlaylists.value = false
  }
}

const collectPlaylist = async (playlistId) => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录后收藏歌单')
    return
  }
  collectingId.value = playlistId
  try {
    await request.post('/playlist/collect', { playlistId })
    ElMessage.success('已收藏到我的歌单')
  } catch (e) {
    const msg = e.response?.data?.message || '收藏失败'
    ElMessage.error(msg)
  } finally {
    collectingId.value = null
  }
}

const goBack = () => {
  router.push('/')
}

onMounted(() => {
  fetchUser()
})
</script>

<template>
  <div class="user-home-page">
    <header class="header">
      <button type="button" class="back-btn" @click="goBack">← 返回首页</button>
    </header>

    <main v-if="loading" class="loading-wrap">加载中...</main>
    <main v-else-if="!userInfo" class="empty-wrap">用户不存在</main>
    <main v-else class="content">
      <section class="user-card section">
        <div class="user-info">
          <el-avatar :size="80" :src="userInfo.avatarUrl" class="avatar">
            {{ (userInfo.username || '').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="meta">
            <h1 class="name">{{ userInfo.username }}</h1>
            <p class="profile">{{ userInfo.profile || '暂无简介' }}</p>
            <div class="tags-row">
              <el-tag v-if="userInfo.isSinger" type="success" size="small">认证歌手</el-tag>
              <span class="stats-text">
                关注 <strong>{{ stats.followCount }}</strong> · 粉丝 <strong>{{ stats.fansCount }}</strong>
              </span>
            </div>
            <div v-if="!isSelf && authStore.isAuthenticated" class="follow-btn-wrap">
              <el-button
                v-if="!isFollowing"
                type="primary"
                :loading="followLoading"
                @click="followUser"
              >
                关注
              </el-button>
              <el-button
                v-else
                :loading="followLoading"
                @click="unfollowUser"
              >
                已关注
              </el-button>
            </div>
          </div>
        </div>
      </section>

      <section class="section">
        <h2 class="section-title">TA 的歌单</h2>
        <div v-if="loadingPlaylists" class="loading-inline">加载中...</div>
        <div v-else-if="!playlists.length" class="empty-inline">暂无歌单</div>
        <div v-else class="playlist-grid">
          <div
            v-for="pl in playlists"
            :key="pl.id"
            class="playlist-card playlist-card--clickable"
            @click="router.push({ name: 'playlist-detail', params: { id: pl.id } })"
          >
            <div class="cover">
              <img v-if="pl.coverUrl" :src="pl.coverUrl" alt="" />
            </div>
            <div class="info">
              <div class="name" :title="pl.name">{{ pl.name }}</div>
              <div class="meta">{{ pl.type === 2 ? '收藏歌单' : '创建歌单' }}</div>
              <el-button
                type="primary"
                size="small"
                :loading="collectingId === pl.id"
                class="collect-btn"
                @click.stop="collectPlaylist(pl.id)"
              >
                收藏
              </el-button>
            </div>
          </div>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.user-home-page {
  min-height: 100vh;
  padding: 20px 24px 28px;
  background: radial-gradient(circle at top left, #020617, #020617 40%, #020617);
  color: #e5e7eb;
}

.header {
  margin-bottom: 24px;
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

.loading-wrap,
.empty-wrap {
  text-align: center;
  padding: 40px;
  color: #9ca3af;
}

.content {
  max-width: 720px;
}

.section {
  background: rgba(15, 23, 42, 0.6);
  border-radius: 16px;
  padding: 20px;
  border: 1px solid rgba(148, 163, 184, 0.25);
  margin-bottom: 20px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.section:hover {
  border-color: rgba(148, 163, 184, 0.35);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
}

.user-info {
  display: flex;
  gap: 20px;
  align-items: center;
}

.avatar {
  background: radial-gradient(circle at 30% 20%, #38bdf8, #4f46e5);
  flex-shrink: 0;
}

.meta {
  flex: 1;
  min-width: 0;
}

.name {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #e5e7eb;
}

.profile {
  margin: 0 0 10px;
  font-size: 14px;
  color: #9ca3af;
  line-height: 1.5;
}

.tags-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.stats-text {
  font-size: 13px;
  color: #9ca3af;
}

.stats-text strong {
  color: #e5e7eb;
  font-weight: 600;
}

.follow-btn-wrap {
  margin-top: 10px;
}

.follow-btn-wrap .el-button--primary {
  background: linear-gradient(135deg, #4f46e5 0%, #0ea5e9 100%);
  border: none;
}

.follow-btn-wrap .el-button--primary:hover {
  background: linear-gradient(135deg, #6366f1 0%, #38bdf8 100%);
}

.section-title {
  margin: 0 0 16px;
  font-size: 16px;
  font-weight: 600;
  color: #e5e7eb;
}

.loading-inline,
.empty-inline {
  color: #9ca3af;
  font-size: 14px;
}

.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 16px;
}

.playlist-card {
  border-radius: 12px;
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.25);
  overflow: hidden;
  transition: border-color 0.2s;
}

.playlist-card--clickable {
  cursor: pointer;
}

.playlist-card:hover {
  border-color: rgba(59, 130, 246, 0.5);
}

.playlist-card .cover {
  width: 100%;
  aspect-ratio: 1;
  background: linear-gradient(135deg, #4f46e5, #0ea5e9);
  overflow: hidden;
}

.playlist-card .cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.playlist-card .info {
  padding: 12px;
}

.playlist-card .name {
  font-size: 14px;
  font-weight: 500;
  color: #e5e7eb;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.playlist-card .meta {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 8px;
}

.collect-btn {
  width: 100%;
}
</style>
