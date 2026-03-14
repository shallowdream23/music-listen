<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { usePlayerStore } from '../store/player'
import { useAuthStore } from '../store/auth'
import { useDownloadStore } from '../store/download'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const route = useRoute()
const playerStore = usePlayerStore()
const authStore = useAuthStore()
const downloadStore = useDownloadStore()

const lyricScrollRef = ref(null)
const progressSliderRef = ref(null)

// 播放页拖动
const panelLeft = ref(0)
const panelTop = ref(0)
const panelDragging = ref(false)
const panelDragStart = ref({ x: 0, y: 0, left: 0, top: 0 })

function getEventXY(e) {
  if (e.touches?.length) return { x: e.touches[0].clientX, y: e.touches[0].clientY }
  if (e.changedTouches?.length) return { x: e.changedTouches[0].clientX, y: e.changedTouches[0].clientY }
  return { x: e.clientX, y: e.clientY }
}

function onPanelDragStart(e) {
  if (e.target?.closest?.('button')) return
  if (e.type === 'mousedown' && e.button !== 0) return
  e.preventDefault()
  panelDragging.value = true
  const { x, y } = getEventXY(e)
  panelDragStart.value = { x, y, left: panelLeft.value, top: panelTop.value }
}

function onPanelDragMove(e) {
  if (!panelDragging.value) return
  const { x, y } = getEventXY(e)
  panelLeft.value = panelDragStart.value.left + (x - panelDragStart.value.x)
  panelTop.value = panelDragStart.value.top + (y - panelDragStart.value.y)
}

function onPanelDragEnd() {
  panelDragging.value = false
}

onMounted(() => {
  downloadStore.loadStoredHandle()
  window.addEventListener('mousemove', onPanelDragMove)
  window.addEventListener('mouseup', onPanelDragEnd)
  window.addEventListener('touchmove', onPanelDragMove, { passive: true })
  window.addEventListener('touchend', onPanelDragEnd)
})
onUnmounted(() => {
  window.removeEventListener('mousemove', onPanelDragMove)
  window.removeEventListener('mouseup', onPanelDragEnd)
  window.removeEventListener('touchmove', onPanelDragMove)
  window.removeEventListener('touchend', onPanelDragEnd)
})

// 收藏到歌单
const showAddToPlaylist = ref(false)
const myPlaylists = ref([])
const loadingPlaylists = ref(false)
const addingPlaylistId = ref(null)

const openAddToPlaylist = async () => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录后收藏到歌单')
    return
  }
  if (!playerStore.currentSong?.id) return
  showAddToPlaylist.value = true
  loadingPlaylists.value = true
  myPlaylists.value = []
  try {
    const userRes = await request.get('/user/getUserInfo')
    const userId = userRes.data?.data?.id
    if (!userId) return
    const listRes = await request.get('/playlist/list', { params: { id: userId } })
    myPlaylists.value = listRes.data?.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取歌单失败')
  } finally {
    loadingPlaylists.value = false
  }
}

const addSongToPlaylist = async (playlistId) => {
  if (!playerStore.currentSong?.id) return
  addingPlaylistId.value = playlistId
  try {
    await request.post('/playlist-song/add', {
      playlistId,
      songId: playerStore.currentSong.id,
    })
    ElMessage.success('已加入歌单')
    showAddToPlaylist.value = false
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '添加失败')
  } finally {
    addingPlaylistId.value = null
  }
}

// 歌词行 [{ time: number, text: string }]，暂无歌词时用空数组
const lyricLines = computed(() => {
  const raw = playerStore.currentSong?.lyric
  if (!raw || typeof raw !== 'string') return []
  return parseLrc(raw)
})

// 当前歌曲无歌词时拉取详情以补全 lyric（兼容从列表进入的旧数据）
watch(
  () => playerStore.currentSong?.id,
  (id) => {
    if (!id) return
    const song = playerStore.currentSong
    if (song.lyric != null && song.lyric !== '') return
    request.get(`/song/${id}`).then((res) => {
      const data = res?.data?.data
      if (data?.lyric != null && song && playerStore.currentSong?.id === id) {
        playerStore.currentSong.lyric = data.lyric
      }
    }).catch(() => {})
  },
  { immediate: true },
)

const currentLyricIndex = computed(() => {
  const t = playerStore.currentTime
  const lines = lyricLines.value
  for (let i = lines.length - 1; i >= 0; i--) {
    if (t >= lines[i].time) return i
  }
  return 0
})

function parseLrc(lrc) {
  const lines = []
  const regex = /\[(\d+):(\d+)\.?(\d*)\]\s*(.*)/g
  let m
  while ((m = regex.exec(lrc)) !== null) {
    const min = parseInt(m[1], 10)
    const sec = parseInt(m[2], 10)
    const ms = m[3] ? parseInt(m[3].padEnd(3, '0').slice(0, 3), 10) : 0
    const time = min * 60 + sec + ms / 1000
    const text = m[4].trim()
    if (text) lines.push({ time, text })
  }
  lines.sort((a, b) => a.time - b.time)
  return lines
}

// 滚动到当前歌词
watch(
  currentLyricIndex,
  (idx) => {
    const el = lyricScrollRef.value
    if (!el) return
    const item = el.querySelector(`[data-lyric-index="${idx}"]`)
    if (item) item.scrollIntoView({ block: 'center', behavior: 'smooth' })
  },
  { flush: 'post' },
)

// 拖动过程中实时更新进度与音频
const handleProgressInput = (val) => {
  if (!playerStore.duration) return
  const target = (val / 100) * playerStore.duration
  playerStore.setCurrentTime(target)
  const audio = document.querySelector('audio')
  if (audio) audio.currentTime = target
}

const handleProgressChange = (val) => {
  if (!playerStore.duration) return
  const target = (val / 100) * playerStore.duration
  const audio = document.querySelector('audio')
  if (audio) {
    audio.currentTime = target
    playerStore.setCurrentTime(target)
  }
  playerStore.setIsSeeking(false)
}

const handleProgressStart = () => {
  playerStore.setIsSeeking(true)
}

const togglePlay = () => {
  if (!playerStore.currentSong) return
  playerStore.togglePlay()
}

const toggleMode = () => {
  playerStore.togglePlayMode()
  ElMessage.success(`已切换为${playerStore.playModeLabel}`)
}

const goBack = () => {
  router.back()
}

const playSongFromList = (song) => {
  playerStore.playSong(song)
}

const showList = ref(false)

// 下载当前歌曲：若已在个人中心选择了文件夹则保存到该文件夹，否则保存到浏览器默认下载目录
const downloading = ref(false)
const downloadCurrentSong = async () => {
  const song = playerStore.currentSong
  if (!song?.audioUrl) {
    ElMessage.warning('该歌曲暂无音频地址')
    return
  }
  const filename = (playerStore.displayTitle(song.title) || '歌曲').replace(/[/\\?*:"<>|]/g, '_') + '.mp3'
  downloading.value = true
  try {
    const blob = await fetch(song.audioUrl, { mode: 'cors' }).then((r) => (r.ok ? r.blob() : Promise.reject(new Error('下载失败'))))
    const dirHandle = await downloadStore.getWriteableHandle()
    if (dirHandle) {
      const fileHandle = await dirHandle.getFileHandle(filename, { create: true })
      const writable = await fileHandle.createWritable()
      await writable.write(blob)
      await writable.close()
      ElMessage.success(`已保存到所选文件夹：${filename}`)
    } else {
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = filename
      a.style.display = 'none'
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      ElMessage.success('已开始下载，请到浏览器默认下载目录查看')
    }
  } catch (e) {
    const a = document.createElement('a')
    a.href = song.audioUrl
    a.download = filename
    a.target = '_blank'
    a.rel = 'noopener'
    a.style.display = 'none'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    ElMessage.success('已发起下载，请到浏览器默认下载目录查看')
  } finally {
    downloading.value = false
  }
}
</script>

<template>
  <div
    class="player-full"
    :class="{ 'no-song': !playerStore.currentSong }"
    :style="{ left: panelLeft + 'px', top: panelTop + 'px' }"
  >
    <div
      class="player-bg"
      :style="playerStore.currentSong?.coverUrl
        ? { backgroundImage: `url(${playerStore.currentSong.coverUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' }
        : {}"
    />
    <header
      class="player-header player-header-draggable"
      @mousedown="onPanelDragStart"
      @touchstart="onPanelDragStart"
    >
      <el-button text class="back-btn" @click="goBack">← 返回</el-button>
      <span class="header-title">正在播放</span>
    </header>

    <template v-if="playerStore.currentSong">
      <main class="player-main">
        <!-- 左侧：旋转封面、进度条、控制区 -->
        <div class="player-left">
          <div class="cover-wrap vinyl-wrap">
            <div class="vinyl" :class="{ spinning: playerStore.playing }">
              <div
                class="vinyl-label"
                :style="playerStore.currentSong?.coverUrl
                  ? { backgroundImage: `url(${playerStore.currentSong.coverUrl})`, backgroundSize: 'cover', backgroundPosition: 'center' }
                  : {}"
              />
              <div class="vinyl-center" />
            </div>
          </div>
          <div class="song-info">
            <h1 class="song-title">
              {{ playerStore.displayTitle(playerStore.currentSong?.title) }}
            </h1>
            <p class="song-meta">播放量 {{ playerStore.currentSong?.playCount ?? 0 }}</p>
            <div class="song-actions-row">
              <el-button
                type="primary"
                plain
                size="small"
                class="add-to-playlist-btn"
                @click="openAddToPlaylist"
              >
                收藏到歌单
              </el-button>
              <el-button
                plain
                size="small"
                class="download-btn"
                :loading="downloading"
                @click="downloadCurrentSong"
              >
                下载
              </el-button>
            </div>
          </div>

          <!-- 进度条 -->
          <div class="progress-wrap">
            <span class="time">{{ playerStore.formattedCurrentTime }}</span>
            <el-slider
              :model-value="playerStore.progressPercent"
              class="progress-slider"
              :show-tooltip="false"
              @input="handleProgressInput"
              @update:model-value="handleProgressInput"
              @change="handleProgressChange"
              @mousedown="handleProgressStart"
              @touchstart="handleProgressStart"
            />
            <span class="time">{{ playerStore.formattedDuration }}</span>
          </div>

          <!-- 控制区：上一首、播放/暂停、下一首、切换顺序、列表 -->
          <div class="controls">
            <el-button circle size="large" class="ctrl-btn" @click="playerStore.playPrev()">
              ⏮
            </el-button>
            <el-button
              circle
              size="large"
              class="ctrl-btn play-btn"
              @click="togglePlay"
            >
              <span v-if="!playerStore.playing">▶</span>
              <span v-else>⏸</span>
            </el-button>
            <el-button circle size="large" class="ctrl-btn" @click="playerStore.playNext()">
              ⏭
            </el-button>
            <el-button circle class="ctrl-btn mode-btn" @click="toggleMode" :title="playerStore.playModeLabel">
              <!-- 顺序：列表循环；随机：交叉箭头 -->
              <svg v-if="playerStore.playMode === 'sequence'" class="mode-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M4 5h14M4 12h14M4 19h8" />
                <circle cx="17" cy="19" r="2" fill="currentColor" stroke="none" />
              </svg>
              <svg v-else class="mode-icon mode-icon-shuffle" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5" />
              </svg>
            </el-button>
            <el-button circle class="ctrl-btn list-btn" @click="showList = true" title="播放列表">
              <svg class="list-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01" />
              </svg>
            </el-button>
          </div>
        </div>

        <!-- 右侧：歌词区域（占大半） -->
        <div class="lyric-wrap" ref="lyricScrollRef">
          <div v-if="!lyricLines.length" class="lyric-empty">暂无歌词</div>
          <div
            v-else
            v-for="(line, idx) in lyricLines"
            :key="idx"
            class="lyric-line"
            :class="{ active: currentLyricIndex === idx }"
            :data-lyric-index="idx"
          >
            {{ line.text }}
          </div>
        </div>
      </main>
    </template>
    <template v-else>
      <main class="player-main empty">
        <p class="empty-tip">暂无播放</p>
        <el-button type="primary" @click="goBack">去选歌</el-button>
      </main>
    </template>

    <!-- 收藏到歌单弹窗 -->
    <el-dialog
      v-model="showAddToPlaylist"
      title="收藏到歌单"
      width="360px"
      class="add-to-playlist-dialog"
    >
      <div v-if="loadingPlaylists" class="dialog-loading">加载中...</div>
      <div v-else-if="!myPlaylists.length" class="dialog-empty">
        暂无歌单，请先在个人中心创建歌单
      </div>
      <div v-else class="playlist-pick-list">
        <div
          v-for="pl in myPlaylists"
          :key="pl.id"
          class="playlist-pick-item"
          @click="addSongToPlaylist(pl.id)"
        >
          <span class="name">{{ pl.name }}</span>
          <el-button
            type="primary"
            size="small"
            :loading="addingPlaylistId === pl.id"
          >
            添加
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 播放列表抽屉 -->
    <el-drawer
      v-model="showList"
      direction="btt"
      size="52%"
      class="playlist-drawer"
      :show-close="false"
    >
      <template #header>
        <div class="drawer-header">
          <div class="drawer-handle" />
          <div class="drawer-title-row">
            <span class="drawer-title-icon">♪</span>
            <div class="drawer-title-text">
              <h3 class="drawer-title">播放列表</h3>
              <p class="drawer-subtitle">共 {{ playerStore.playQueue.length }} 首</p>
            </div>
            <el-button text class="drawer-close-btn" @click="showList = false" title="关闭">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><path d="M18 6L6 18M6 6l12 12" /></svg>
            </el-button>
          </div>
        </div>
      </template>
      <div class="drawer-body-inner">
        <div v-if="!playerStore.playQueue.length" class="drawer-empty">
          <span class="drawer-empty-icon">♫</span>
          <p>暂无歌曲</p>
          <span class="drawer-empty-hint">在首页或歌单中选歌即可加入列表</span>
        </div>
        <div v-else class="drawer-list">
          <div
            v-for="(song, idx) in playerStore.playQueue"
            :key="song.id"
            class="drawer-item"
            :class="{ active: playerStore.currentSong?.id === song.id, playing: playerStore.currentSong?.id === song.id && playerStore.playing }"
            @click="playSongFromList(song)"
          >
            <span class="drawer-item-index">{{ idx + 1 }}</span>
            <div class="drawer-item-cover" :style="song.coverUrl ? { backgroundImage: `url(${song.coverUrl})` } : {}">
              <span v-if="playerStore.currentSong?.id === song.id && playerStore.playing" class="drawer-item-playing">
                <span class="bar" /><span class="bar" /><span class="bar" />
              </span>
            </div>
            <span class="drawer-item-name">{{ playerStore.displayTitle(song.title) }}</span>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.player-full {
  position: fixed;
  inset: 0;
  z-index: 1001;
  display: flex;
  flex-direction: column;
  background: #0f172a;
  color: #e5e7eb;
  overflow: hidden;
}

.player-bg {
  position: absolute;
  inset: 0;
  background-size: cover;
  background-position: center;
  background-image: linear-gradient(135deg, #1e293b 0%, #334155 50%, #4f46e5 100%);
  filter: blur(80px) brightness(0.35);
  transform: scale(1.1);
}

.player-header {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  z-index: 1;
}

.player-header-draggable {
  cursor: move;
  touch-action: none;
  user-select: none;
}

.back-btn {
  color: #e5e7eb;
  font-size: 15px;
}

.header-title {
  font-size: 15px;
  color: #94a3b8;
}

.player-main {
  position: relative;
  flex: 1;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  padding: 20px 24px 32px;
  gap: 32px;
  z-index: 1;
  min-height: 0;
}

/* 左侧：旋转封面 + 歌曲信息 + 进度条 + 控制按钮，放大并居中 */
.player-left {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 420px;
  min-width: 360px;
  align-self: stretch;
}

.player-main.empty {
  flex-direction: column;
  justify-content: center;
  align-items: center;
  gap: 16px;
}

.empty-tip {
  color: #94a3b8;
  margin: 0;
}

.player-left .cover-wrap {
  margin-bottom: 28px;
}

.vinyl-wrap {
  padding: 8px;
}

.vinyl {
  width: 300px;
  height: 300px;
  border-radius: 50%;
  background: radial-gradient(circle at 50% 50%, #2d2d2d 0%, #1a1a1a 40%, #0d0d0d 100%);
  box-shadow:
    0 0 0 2px rgba(255, 255, 255, 0.06),
    0 0 0 4px #1a1a1a,
    0 12px 32px rgba(0, 0, 0, 0.6),
    inset 0 0 40px rgba(0, 0, 0, 0.5);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
}

.vinyl.spinning {
  animation: vinyl-spin 8s linear infinite;
}

.vinyl-label {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background: linear-gradient(135deg, #334155, #1e293b);
  background-size: cover;
  background-position: center;
  position: absolute;
  box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.6);
  border: 3px solid #1a1a1a;
}

.vinyl-center {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #0f172a;
  position: absolute;
  z-index: 1;
  box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.8), 0 0 0 1px rgba(255, 255, 255, 0.05);
}

@keyframes vinyl-spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.player-left .song-info {
  text-align: center;
  margin-bottom: 22px;
}

.song-title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #f1f5f9;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.song-meta {
  margin: 0 0 12px;
  font-size: 14px;
  color: #94a3b8;
}

.song-actions-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}
.add-to-playlist-btn,
.download-btn {
  flex-shrink: 0;
}

.dialog-loading,
.dialog-empty {
  padding: 24px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}

.playlist-pick-list {
  max-height: 320px;
  overflow-y: auto;
}

.playlist-pick-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  cursor: pointer;
}

.playlist-pick-item:hover {
  background: rgba(148, 163, 184, 0.08);
}

.playlist-pick-item .name {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-right: 12px;
}

.add-to-playlist-dialog :deep(.el-dialog__header) {
  color: #e5e7eb;
}

.add-to-playlist-dialog :deep(.el-dialog__body) {
  color: #e5e7eb;
}

.player-left .progress-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  margin-bottom: 26px;
}

.progress-wrap .time {
  font-size: 12px;
  color: #94a3b8;
  flex-shrink: 0;
}

.progress-slider {
  flex: 1;
}

.player-left .controls {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 18px;
  margin-bottom: 0;
}

.ctrl-btn {
  background: rgba(148, 163, 184, 0.2);
  border: none;
  color: #e5e7eb;
}

.ctrl-btn:hover {
  background: rgba(148, 163, 184, 0.35);
  color: #fff;
}

.play-btn {
  width: 60px;
  height: 60px;
  font-size: 24px;
  background: linear-gradient(135deg, #4f46e5, #0ea5e9);
}

.play-btn:hover {
  background: linear-gradient(135deg, #6366f1, #38bdf8);
}

/* 切换顺序 / 播放列表 图标按钮 */
.ctrl-btn.mode-btn,
.ctrl-btn.list-btn {
  width: 40px;
  height: 40px;
  padding: 0;
}
.mode-icon,
.list-icon {
  width: 20px;
  height: 20px;
  display: block;
  color: inherit;
}
.mode-icon-shuffle {
  width: 18px;
  height: 18px;
}

/* 歌词与左侧同高，同为主内容区拉伸高度；隐藏右侧滚动条 */
.lyric-wrap {
  flex: 1;
  min-width: 0;
  align-self: stretch;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
  text-align: center;
  padding: 24px 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.lyric-wrap::-webkit-scrollbar {
  display: none;
}

.lyric-empty {
  color: #64748b;
  font-size: 14px;
  padding: 20px;
}

.lyric-line {
  font-size: 15px;
  line-height: 2.2;
  color: #64748b;
  transition: color 0.2s, transform 0.2s;
}

.lyric-line.active {
  color: #f1f5f9;
  font-size: 16px;
  font-weight: 500;
}

/* 播放列表抽屉：圆角、背景、隐藏默认关闭 */
.playlist-drawer :deep(.el-drawer) {
  border-radius: 20px 20px 0 0;
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%);
  box-shadow: 0 -8px 32px rgba(0, 0, 0, 0.4);
}
.playlist-drawer :deep(.el-drawer__header) {
  margin-bottom: 0;
  padding: 12px 20px 16px;
}
.playlist-drawer :deep(.el-drawer__body) {
  padding: 0 20px 28px;
  overflow-y: auto;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.playlist-drawer :deep(.el-drawer__body)::-webkit-scrollbar {
  display: none;
}

.drawer-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.drawer-handle {
  width: 40px;
  height: 4px;
  border-radius: 2px;
  background: rgba(148, 163, 184, 0.4);
}
.drawer-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}
.drawer-title-text {
  flex: 1;
  min-width: 0;
}
.drawer-close-btn {
  color: #94a3b8;
  padding: 6px;
}
.drawer-close-btn:hover {
  color: #e5e7eb;
}
.drawer-close-btn svg {
  width: 20px;
  height: 20px;
  display: block;
}
.drawer-title-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #4f46e5 0%, #0ea5e9 100%);
  color: #fff;
  font-size: 22px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 1;
}
.drawer-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #f1f5f9;
}
.drawer-subtitle {
  margin: 4px 0 0;
  font-size: 13px;
  color: #94a3b8;
}

.drawer-body-inner {
  min-height: 120px;
}
.drawer-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.drawer-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 10px 14px;
  border-radius: 14px;
  cursor: pointer;
  color: #94a3b8;
  transition: background 0.2s, color 0.2s;
}
.drawer-item:hover {
  background: rgba(148, 163, 184, 0.12);
  color: #e5e7eb;
}
.drawer-item.active {
  background: rgba(79, 70, 229, 0.2);
  color: #c7d2fe;
}
.drawer-item.playing {
  color: #38bdf8;
}
.drawer-item-index {
  width: 28px;
  flex-shrink: 0;
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  text-align: center;
}
.drawer-item.active .drawer-item-index {
  color: #38bdf8;
}
.drawer-item-cover {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #334155 0%, #1e293b 100%);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}
.drawer-item-playing {
  display: flex;
  align-items: flex-end;
  gap: 3px;
  height: 18px;
}
.drawer-item-playing .bar {
  width: 4px;
  height: 100%;
  min-height: 6px;
  border-radius: 2px;
  background: currentColor;
  animation: drawer-bar 0.6s ease-in-out infinite;
}
.drawer-item-playing .bar:nth-child(1) { animation-delay: 0s; }
.drawer-item-playing .bar:nth-child(2) { animation-delay: 0.15s; }
.drawer-item-playing .bar:nth-child(3) { animation-delay: 0.3s; }
@keyframes drawer-bar {
  0%, 100% { transform: scaleY(0.4); }
  50% { transform: scaleY(1); }
}
.drawer-item-name {
  flex: 1;
  min-width: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
}
.drawer-item.active .drawer-item-name {
  font-weight: 500;
}

.drawer-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  text-align: center;
  color: #64748b;
}
.drawer-empty-icon {
  font-size: 48px;
  opacity: 0.5;
  margin-bottom: 16px;
}
.drawer-empty p {
  margin: 0;
  font-size: 15px;
  color: #94a3b8;
}
.drawer-empty-hint {
  margin-top: 8px;
  font-size: 13px;
  color: #64748b;
}
</style>
