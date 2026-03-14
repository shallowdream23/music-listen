<script setup>
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { usePlayerStore } from '../store/player'

const route = useRoute()
const router = useRouter()
const playerStore = usePlayerStore()

const playlistId = computed(() => route.params.id)
const loading = ref(false)
const playlist = ref(null)
const songs = ref([])
const loadingSongs = ref(false)
const currentUser = ref(null)
const editDialogVisible = ref(false)
const editName = ref('')
const coverUploading = ref(false)
const coverFileInput = ref(null)

const isOwner = computed(() => {
  const uid = currentUser.value?.id
  const pid = playlist.value?.userId
  return uid != null && pid != null && String(uid) === String(pid)
})

const displayTitle = (title) => {
  if (!title) return ''
  return title.replace(/\.mp3$/i, '')
}

const fetchPlaylist = async () => {
  if (!playlistId.value) return
  loading.value = true
  try {
    const res = await request.get(`/playlist/${playlistId.value}`)
    playlist.value = res.data?.data || null
    if (playlist.value?.name) {
      document.title = `${playlist.value.name} - 在线音乐`
    }
  } catch (e) {
    const msg = e.response?.data?.message || '歌单不存在'
    ElMessage.error(msg)
    playlist.value = null
  } finally {
    loading.value = false
  }
}

const fetchSongs = async () => {
  if (!playlistId.value) return
  loadingSongs.value = true
  try {
    const res = await request.get('/playlist-song/list', {
      params: { playlistId: playlistId.value },
    })
    songs.value = res.data?.data || []
  } catch (e) {
    const msg = e.response?.data?.message || '获取歌曲列表失败'
    ElMessage.error(msg)
    songs.value = []
  } finally {
    loadingSongs.value = false
  }
}

const playAll = () => {
  const list = songs.value.filter((s) => s?.audioUrl)
  if (!list.length) {
    ElMessage.warning('该歌单暂无可播放的歌曲')
    return
  }
  playerStore.playQueue = [...list]
  playerStore.playSong(list[0])
  ElMessage.success('已开始播放歌单')
}

/** 点击歌曲：进入播放页；若是当前曲不切换，否则用本歌单作为播放列表并切换至该曲 */
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

const fetchCurrentUser = async () => {
  try {
    const res = await request.get('/user/getUserInfo')
    currentUser.value = res.data?.data || null
  } catch {
    currentUser.value = null
  }
}

const openEditName = () => {
  editName.value = playlist.value?.name || ''
  editDialogVisible.value = true
}

const submitEditName = async () => {
  const name = editName.value?.trim()
  if (!name) {
    ElMessage.warning('歌单名称不能为空')
    return
  }
  try {
    await request.put(`/playlist/${playlistId.value}`, { name })
    ElMessage.success('已修改')
    editDialogVisible.value = false
    await fetchPlaylist()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  }
}

const triggerCoverUpload = () => {
  coverFileInput.value?.click()
}

const onCoverFileChange = async (e) => {
  const file = e.target?.files?.[0]
  if (!file || !file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    e.target.value = ''
    return
  }
  coverUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post(`/playlist/${playlistId.value}/cover`, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const url = res.data?.data
    if (url) {
      if (playlist.value) playlist.value.coverUrl = url
      ElMessage.success('封面上传成功')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '上传失败')
  } finally {
    coverUploading.value = false
    e.target.value = ''
  }
}

onMounted(async () => {
  await fetchPlaylist()
  if (playlist.value) await fetchSongs()
  await fetchCurrentUser()
})
</script>

<template>
  <div class="playlist-detail">
    <header class="detail-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <div v-if="loading" class="header-loading">加载中...</div>
      <div v-else-if="!playlist" class="header-empty">歌单不存在</div>
      <div v-else class="header-content">
        <div
          class="cover-wrap"
          :class="{ 'cover-wrap--editable': isOwner }"
          @click="isOwner ? triggerCoverUpload() : null"
        >
          <img v-if="playlist.coverUrl" :src="playlist.coverUrl" alt="" class="cover-img" />
          <div v-else class="cover-placeholder" />
          <div v-if="isOwner" class="cover-overlay">
            <span v-if="coverUploading">上传中...</span>
            <span v-else>更换封面</span>
          </div>
          <input
            ref="coverFileInput"
            type="file"
            accept="image/*"
            class="cover-file-input"
            @change="onCoverFileChange"
          />
        </div>
        <div class="meta">
          <h1 class="title">{{ playlist.name }}</h1>
          <p class="subtitle">
            {{ playlist.type === 2 ? '收藏歌单' : '创建歌单' }} · {{ songs.length }} 首
          </p>
          <div class="actions">
            <el-button type="primary" :disabled="!songs.length" @click="playAll">
              播放全部
            </el-button>
            <el-button v-if="isOwner" @click="openEditName">修改名称</el-button>
          </div>
        </div>
      </div>
    </header>

    <section class="song-section">
      <div v-if="loadingSongs" class="songs-loading">歌曲加载中...</div>
      <div v-else-if="!songs.length" class="songs-empty">
        该歌单暂无歌曲
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

    <el-dialog
      v-model="editDialogVisible"
      title="修改歌单名称"
      width="400px"
    >
      <el-input v-model="editName" placeholder="请输入歌单名称" maxlength="100" show-word-limit />
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditName">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.playlist-detail {
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
  position: relative;
  flex-shrink: 0;
  width: 200px;
  height: 200px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.cover-wrap--editable {
  cursor: pointer;
}

.cover-wrap--editable:hover .cover-overlay {
  opacity: 1;
}

.cover-file-input {
  display: none;
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #e5e7eb;
  opacity: 0;
  transition: opacity 0.2s;
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
  margin: 0 0 16px;
  font-size: 13px;
  color: #9ca3af;
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
  background-color: rgba(56, 189, 248, 0.12);
}

.song-row.active .song-title,
.song-row.active .song-meta {
  color: #38bdf8;
}

.song-row.active .song-index {
  color: #38bdf8;
}

.song-index {
  width: 28px;
  flex-shrink: 0;
  font-size: 13px;
  color: #9ca3af;
  text-align: right;
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
