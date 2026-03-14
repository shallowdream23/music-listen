<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { usePlayerStore } from '../store/player'

const route = useRoute()
const router = useRouter()
const playerStore = usePlayerStore()

const albumId = computed(() => route.params.id)
const loading = ref(false)
const album = ref(null)
const singerName = ref('')
const songs = ref([])

const displayTitle = (title) => {
  if (!title) return ''
  return title.replace(/\.mp3$/i, '')
}

const fetchDetail = async () => {
  if (!albumId.value) return
  loading.value = true
  try {
    const res = await request.get(`/album/detail/${albumId.value}`)
    const data = res.data?.data
    if (data) {
      album.value = data.album || null
      singerName.value = data.singerName || ''
      songs.value = data.songs || []
      if (album.value?.title) {
        document.title = `${album.value.title} - 专辑 - 在线音乐`
      }
    } else {
      album.value = null
      songs.value = []
    }
  } catch (e) {
    const msg = e.response?.data?.message || '专辑不存在'
    ElMessage.error(msg)
    album.value = null
    songs.value = []
  } finally {
    loading.value = false
  }
}

const playAll = () => {
  const list = songs.value.filter((s) => s?.audioUrl)
  if (!list.length) {
    ElMessage.warning('该专辑暂无可播放的歌曲')
    return
  }
  playerStore.playQueue = [...list]
  playerStore.playSong(list[0])
  ElMessage.success('已开始播放专辑')
}

const playSong = (song) => {
  if (!song?.audioUrl) {
    ElMessage.warning('该歌曲暂时没有音频地址')
    return
  }
  const isCurrent = playerStore.currentSong?.id === song.id
  if (!isCurrent) {
    const list = songs.value.filter((s) => s?.audioUrl)
    playerStore.playQueue = list.length ? [...list] : [song]
    playerStore.playSong(song)
  }
  router.push({ name: 'player' })
}

const goBack = () => {
  router.back()
}

onMounted(fetchDetail)
</script>

<template>
  <div class="album-detail">
    <header class="detail-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <div v-if="loading" class="header-loading">加载中...</div>
      <div v-else-if="!album" class="header-empty">专辑不存在</div>
      <div v-else class="header-content">
        <div class="cover-wrap">
          <img v-if="album.coverUrl" :src="album.coverUrl" alt="" class="cover-img" />
          <div v-else class="cover-placeholder" />
        </div>
        <div class="meta">
          <h1 class="title">{{ album.title }}</h1>
          <p class="subtitle">
            <span v-if="singerName">歌手：{{ singerName }}</span>
            <span v-else>专辑</span>
            · {{ songs.length }} 首
          </p>
          <p v-if="album.description" class="description">{{ album.description }}</p>
          <div class="actions">
            <el-button type="primary" :disabled="!songs.length" @click="playAll">
              播放全部
            </el-button>
          </div>
        </div>
      </div>
    </header>

    <section class="song-section">
      <div v-if="loading" class="songs-loading">歌曲加载中...</div>
      <div v-else-if="!songs.length" class="songs-empty">
        该专辑暂无歌曲
      </div>
      <el-scrollbar v-else class="songs-list">
        <div
          v-for="(song, index) in songs"
          :key="song.id"
          class="song-row"
          :class="{ active: playerStore.currentSong?.id === song.id }"
          @click="playSong(song)"
        >
          <span class="song-index">{{ index + 1 }}</span>
          <div class="song-main">
            <div class="song-title" :title="displayTitle(song.title)">
              {{ displayTitle(song.title) }}
            </div>
            <div class="song-meta">播放量：{{ song.playCount ?? 0 }}</div>
          </div>
        </div>
      </el-scrollbar>
    </section>
  </div>
</template>

<style scoped>
.album-detail {
  min-height: 100vh;
  background: radial-gradient(circle at top left, #020617, #020617 40%, #020617);
  color: #e5e7eb;
  padding: 20px 24px 28px;
}

.detail-header {
  margin-bottom: 24px;
}

.back-btn {
  margin-bottom: 16px;
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

.header-loading,
.header-empty {
  font-size: 14px;
  color: #9ca3af;
}

.header-content {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.cover-wrap {
  flex-shrink: 0;
  width: 200px;
  height: 200px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #4f46e5, #ec4899, #22c55e);
  background-size: 200% 200%;
  animation: gradientMove 6s ease infinite;
}

.meta {
  flex: 1;
  min-width: 0;
}

.meta .title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #e5e7eb;
}

.meta .subtitle {
  margin: 0 0 8px;
  font-size: 13px;
  color: #9ca3af;
}

.description {
  margin: 0 0 16px;
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.5;
}

.meta .actions {
  display: flex;
  gap: 12px;
}

.song-section {
  background: rgba(15, 23, 42, 0.6);
  border-radius: 16px;
  padding: 16px;
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.songs-loading,
.songs-empty {
  font-size: 13px;
  color: #9ca3af;
  padding: 24px;
  text-align: center;
}

.songs-list {
  max-height: 420px;
}

.song-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 8px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  cursor: pointer;
}

.song-row:last-of-type {
  border-bottom: none;
}

.song-row {
  transition: background-color 0.2s;
}

.song-row:hover {
  background-color: rgba(30, 41, 59, 0.5);
}

.song-row.active {
  background-color: rgba(56, 189, 248, 0.15);
  color: #38bdf8;
}

.song-row.active .song-title,
.song-row.active .song-meta {
  color: #38bdf8;
}

.song-index {
  width: 28px;
  flex-shrink: 0;
  font-size: 13px;
  color: #9ca3af;
  text-align: right;
}

.song-row.active .song-index {
  color: #38bdf8;
}

.song-main {
  flex: 1;
  min-width: 0;
}

.song-title {
  font-size: 14px;
  font-weight: 500;
  color: #e5e7eb;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.song-meta {
  margin-top: 2px;
  font-size: 12px;
  color: #9ca3af;
}

@keyframes gradientMove {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
</style>
