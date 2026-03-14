<script setup>
import { watch, ref, nextTick, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { usePlayerStore } from '../store/player'

const BAR_WIDTH = 560
const STORAGE_KEY = 'music_player_bar_pos'

const router = useRouter()
const playerStore = usePlayerStore()
const audioRef = ref(null)
const playlistPopoverVisible = ref(false)

// 拖动：位置与状态
const barLeft = ref(20)
const barBottom = ref(20)
const barWidthPx = ref(BAR_WIDTH)
const dragging = ref(false)
const dragStart = ref({ x: 0, y: 0, left: 0, bottom: 0 })

function loadSavedPosition() {
  try {
    const s = localStorage.getItem(STORAGE_KEY)
    if (s) {
      const { left, bottom } = JSON.parse(s)
      if (typeof left === 'number' && typeof bottom === 'number') {
        barLeft.value = left
        barBottom.value = bottom
      }
    }
  } catch (_) {}
}

function savePosition() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ left: barLeft.value, bottom: barBottom.value }))
  } catch (_) {}
}

function updateBarWidth() {
  barWidthPx.value = Math.min(BAR_WIDTH, window.innerWidth - 40)
  nextTick(clampPosition)
}

function clampPosition() {
  const w = window.innerWidth
  const h = window.innerHeight
  const maxLeft = Math.max(0, w - barWidthPx.value - 20)
  const maxBottom = Math.max(0, h - 80)
  barLeft.value = Math.max(0, Math.min(barLeft.value, maxLeft))
  barBottom.value = Math.max(0, Math.min(barBottom.value, maxBottom))
}

function getEventXY(e) {
  if (e.touches?.length) return { x: e.touches[0].clientX, y: e.touches[0].clientY }
  if (e.changedTouches?.length) return { x: e.changedTouches[0].clientX, y: e.changedTouches[0].clientY }
  return { x: e.clientX, y: e.clientY }
}

function onBarDragStart(e) {
  if (e.type === 'mousedown' && e.button !== 0) return
  e.preventDefault()
  dragging.value = true
  const { x, y } = getEventXY(e)
  dragStart.value = { x, y, left: barLeft.value, bottom: barBottom.value }
}

function onBarDragMove(e) {
  if (!dragging.value) return
  const { x, y } = getEventXY(e)
  const dx = x - dragStart.value.x
  const dy = dragStart.value.y - y
  barLeft.value = dragStart.value.left + dx
  barBottom.value = dragStart.value.bottom + dy
  clampPosition()
}

function onBarDragEnd() {
  if (dragging.value) {
    dragging.value = false
    savePosition()
  }
}

onMounted(() => {
  loadSavedPosition()
  updateBarWidth()
  clampPosition()
  window.addEventListener('resize', updateBarWidth)
  window.addEventListener('mousemove', onBarDragMove)
  window.addEventListener('mouseup', onBarDragEnd)
  window.addEventListener('touchmove', onBarDragMove, { passive: true })
  window.addEventListener('touchend', onBarDragEnd)
})
onUnmounted(() => {
  window.removeEventListener('resize', updateBarWidth)
  window.removeEventListener('mousemove', onBarDragMove)
  window.removeEventListener('mouseup', onBarDragEnd)
  window.removeEventListener('touchmove', onBarDragMove)
  window.removeEventListener('touchend', onBarDragEnd)
})

const openPlayerPage = () => {
  if (playerStore.currentSong) router.push({ name: 'player' })
}

// 同时监听 audio 元素和 currentSong：首次点击时组件才挂载，audioRef 要等渲染后才有值，必须两者都有再设置 src
watch(
  () => [audioRef.value, playerStore.currentSong],
  ([audio, song]) => {
    if (!audio || !song?.audioUrl) return
    audio.volume = playerStore.volume
    audio.src = song.audioUrl
    if (playerStore.playing) {
      nextTick(() => {
        const p = audio.play()
        if (p && typeof p.catch === 'function') p.catch(() => {})
      })
    }
  },
  { immediate: true },
)

watch(
  () => playerStore.playing,
  (playing) => {
    if (!audioRef.value) return
    if (playing) {
      const p = audioRef.value.play()
      if (p && typeof p.catch === 'function') p.catch(() => {})
    } else {
      audioRef.value.pause()
    }
  },
  { immediate: true },
)

// 音频可播放时若处于播放状态则开始播放（解决首次点击不播的问题）
const onCanPlay = () => {
  if (!audioRef.value) return
  if (playerStore.playing) {
    const p = audioRef.value.play()
    if (p && typeof p.catch === 'function') p.catch(() => {})
  }
}

watch(
  () => playerStore.volume,
  (v) => {
    if (audioRef.value) audioRef.value.volume = v
  },
  { immediate: true },
)

const onTimeUpdate = () => {
  if (!audioRef.value || playerStore.isSeeking) return
  playerStore.setCurrentTime(audioRef.value.currentTime)
}

const onLoadedMetadata = () => {
  if (!audioRef.value) return
  playerStore.setDuration(audioRef.value.duration || 0)
}

const onEnded = () => {
  playerStore.playNext()
}

// 拖动过程中实时更新进度与音频
const handleProgressInput = (val) => {
  if (!audioRef.value || !playerStore.duration) return
  const target = (val / 100) * playerStore.duration
  playerStore.setCurrentTime(target)
  audioRef.value.currentTime = target
}

const handleProgressChange = (val) => {
  if (!audioRef.value || !playerStore.duration) return
  const target = (val / 100) * playerStore.duration
  audioRef.value.currentTime = target
  playerStore.setCurrentTime(target)
  playerStore.setIsSeeking(false)
}

const handleProgressStart = () => {
  playerStore.setIsSeeking(true)
}

const handleVolumeChange = (val) => {
  playerStore.setVolume(val)
}

const togglePlay = () => {
  if (!playerStore.currentSong) return
  playerStore.togglePlay()
}

const togglePlayMode = () => {
  playerStore.togglePlayMode()
  ElMessage.success(`已切换为${playerStore.playModeLabel}`)
}

const playSongFromList = (song) => {
  playerStore.playSong(song)
}
</script>

<template>
  <div>
    <!-- audio 始终挂载，避免首次点击时 v-if 才创建导致 ref 未就绪 -->
    <audio
      ref="audioRef"
      @timeupdate="onTimeUpdate"
      @loadedmetadata="onLoadedMetadata"
      @canplay="onCanPlay"
      @ended="onEnded"
    />
    <div
      class="player-bar"
      v-if="playerStore.currentSong"
      :style="{ left: barLeft + 'px', bottom: barBottom + 'px', width: barWidthPx + 'px' }"
    >
      <div
        class="player-bar-drag-handle"
        @mousedown="onBarDragStart"
        @touchstart="onBarDragStart"
      >
        <span class="drag-grip">
          <span /><span /><span />
        </span>
      </div>
      <div class="player-info" @click="openPlayerPage">
        <div
          class="player-cover"
          :style="playerStore.currentSong?.coverUrl ? { backgroundImage: `url(${playerStore.currentSong.coverUrl})`, backgroundSize: 'cover' } : {}"
        >
          <span v-if="playerStore.playing" class="player-cover-playing">
            <span class="bar" /><span class="bar" /><span class="bar" />
          </span>
        </div>
        <div class="player-meta">
          <div
            class="player-title"
            :title="playerStore.displayTitle(playerStore.currentSong?.title)"
          >
            {{ playerStore.displayTitle(playerStore.currentSong?.title) }}
          </div>
          <div class="player-sub">
            <span v-if="playerStore.playing" class="playing-dot" /> 正在播放
          </div>
        </div>
      </div>
      <div class="player-center">
        <div class="player-controls">
          <button type="button" class="bar-btn bar-btn-play" @click="togglePlay" title="播放/暂停">
            <svg v-if="!playerStore.playing" class="bar-icon" viewBox="0 0 24 24" fill="currentColor"><path d="M8 5v14l11-7z"/></svg>
            <svg v-else class="bar-icon" viewBox="0 0 24 24" fill="currentColor"><path d="M6 4h4v16H6V4zm8 0h4v16h-4V4z"/></svg>
          </button>
          <button type="button" class="bar-btn" @click="playerStore.playNext()" title="下一首">
            <svg class="bar-icon" viewBox="0 0 24 24" fill="currentColor"><path d="M6 18l8.5-6L6 6v12zM16 6v12h2V6h-2z"/></svg>
          </button>
          <button type="button" class="bar-btn bar-btn-mode" @click="togglePlayMode" :title="playerStore.playModeLabel">
            <svg v-if="playerStore.playMode === 'sequence'" class="bar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 5h14M4 12h14M4 19h8"/><circle cx="17" cy="19" r="2" fill="currentColor" stroke="none"/></svg>
            <svg v-else class="bar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M16 3h5v5M4 20L21 3M21 16v5h-5M15 15l6 6M4 4l5 5"/></svg>
          </button>
        </div>
        <div class="player-progress">
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
      </div>
      <div class="player-actions">
        <el-popover
          v-model:visible="playlistPopoverVisible"
          placement="top"
          trigger="click"
          popper-class="player-bar-popover"
        >
          <template #reference>
            <button type="button" class="bar-btn" title="播放列表">
              <svg class="bar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M8 6h13M8 12h13M8 18h13M3 6h.01M3 12h.01M3 18h.01"/></svg>
            </button>
          </template>
          <div class="playlist-pop">
            <div class="playlist-pop-title">播放列表 · {{ playerStore.playQueue.length }} 首</div>
            <div
              v-for="song in playerStore.playQueue"
              :key="song.id"
              class="playlist-pop-item"
              :class="{ active: playerStore.currentSong && playerStore.currentSong.id === song.id }"
              @click="playSongFromList(song)"
            >
              {{ playerStore.displayTitle(song.title) }}
            </div>
            <div v-if="!playerStore.playQueue.length" class="playlist-pop-empty">暂无歌曲</div>
          </div>
        </el-popover>
        <el-popover placement="top" trigger="click" popper-class="player-bar-popover">
          <template #reference>
            <button type="button" class="bar-btn" title="音量">
              <svg class="bar-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 5L6 9H2v6h4l5 4V5zM15.54 8.46a5 5 0 010 7.07M19.07 4.93a10 10 0 010 14.14"/></svg>
            </button>
          </template>
          <div class="volume-pop">
            <el-slider
              :model-value="playerStore.volume"
              :min="0"
              :max="1"
              :step="0.01"
              :show-tooltip="false"
              vertical
              height="88px"
              @change="handleVolumeChange"
            />
          </div>
        </el-popover>
      </div>
    </div>
  </div>
</template>

<style scoped>
.player-bar {
  position: fixed;
  padding: 10px 14px 10px 6px;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.98) 100%);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border: 1px solid rgba(148, 163, 184, 0.15);
  color: #e5e7eb;
  display: grid;
  grid-template-columns: 20px 1fr minmax(0, 1.4fr) 72px;
  gap: 10px;
  align-items: center;
  z-index: 1000;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.35), 0 0 0 1px rgba(255, 255, 255, 0.04) inset;
}

.player-bar-drag-handle {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: move;
  color: #64748b;
  user-select: none;
  touch-action: none;
}
.player-bar-drag-handle:hover {
  color: #94a3b8;
}
.drag-grip {
  display: flex;
  flex-direction: column;
  gap: 3px;
  align-items: center;
}
.drag-grip span {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: currentColor;
}

.player-info {
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  border-radius: 12px;
  padding: 4px 6px;
  transition: background 0.2s;
}
.player-info:hover {
  background: rgba(148, 163, 184, 0.08);
}

.player-cover {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: linear-gradient(135deg, #334155 0%, #1e293b 100%);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
}
.player-cover-playing {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.35);
}
.player-cover-playing .bar {
  width: 3px;
  height: 14px;
  margin: 0 2px;
  border-radius: 2px;
  background: #fff;
  animation: bar-bounce 0.6s ease-in-out infinite;
}
.player-cover-playing .bar:nth-child(1) { animation-delay: 0s; }
.player-cover-playing .bar:nth-child(2) { animation-delay: 0.15s; }
.player-cover-playing .bar:nth-child(3) { animation-delay: 0.3s; }
@keyframes bar-bounce {
  0%, 100% { transform: scaleY(0.5); }
  50% { transform: scaleY(1); }
}

.player-meta {
  flex: 1;
  min-width: 0;
}
.player-title {
  font-size: 14px;
  font-weight: 600;
  color: #f1f5f9;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.player-sub {
  margin-top: 2px;
  font-size: 12px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 6px;
}
.playing-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #38bdf8;
  animation: pulse-dot 1.2s ease-in-out infinite;
  flex-shrink: 0;
}
@keyframes pulse-dot {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.6; transform: scale(0.9); }
}

.player-center {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.player-controls {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
}

.bar-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: none;
  background: rgba(148, 163, 184, 0.15);
  color: #e5e7eb;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s, color 0.2s, transform 0.15s;
}
.bar-btn:hover {
  background: rgba(148, 163, 184, 0.28);
  color: #fff;
}
.bar-btn:active {
  transform: scale(0.96);
}
.bar-btn-play {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, #4f46e5 0%, #0ea5e9 100%);
  color: #fff;
}
.bar-btn-play:hover {
  background: linear-gradient(135deg, #6366f1 0%, #38bdf8 100%);
  color: #fff;
}
.bar-icon {
  width: 18px;
  height: 18px;
  display: block;
}
.bar-btn-play .bar-icon {
  width: 20px;
  height: 20px;
}
.bar-btn-mode .bar-icon {
  width: 16px;
  height: 16px;
}

.player-progress {
  display: grid;
  grid-template-columns: 32px minmax(0, 1fr) 32px;
  gap: 8px;
  align-items: center;
}
.player-progress :deep(.el-slider__runway) {
  background-color: rgba(148, 163, 184, 0.25);
  height: 4px;
  border-radius: 2px;
}
.player-progress :deep(.el-slider__bar) {
  background: linear-gradient(90deg, #4f46e5, #0ea5e9);
  border-radius: 2px;
}
.player-progress :deep(.el-slider__button) {
  width: 12px;
  height: 12px;
  border: 2px solid #fff;
  background: #0ea5e9;
}
.time {
  font-size: 11px;
  color: #94a3b8;
  flex-shrink: 0;
  text-align: center;
}

.player-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
}

.volume-pop {
  padding: 8px 12px;
}

.playlist-pop {
  max-height: 240px;
  min-width: 220px;
  overflow-y: auto;
  padding: 4px 0;
}
.playlist-pop-title {
  font-size: 12px;
  color: #94a3b8;
  padding: 6px 12px 8px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.2);
  margin-bottom: 4px;
}
.playlist-pop-item {
  padding: 10px 14px;
  font-size: 13px;
  color: #e5e7eb;
  cursor: pointer;
  border-radius: 8px;
  margin: 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: background 0.2s;
}
.playlist-pop-item:hover {
  background: rgba(148, 163, 184, 0.12);
}
.playlist-pop-item.active {
  background: rgba(79, 70, 229, 0.25);
  color: #c7d2fe;
  font-weight: 500;
}
.playlist-pop-empty {
  font-size: 13px;
  color: #64748b;
  padding: 16px 14px;
  text-align: center;
}
</style>

<style>
/* 播放栏弹出的 popover 全局样式，与深色主题一致 */
.player-bar-popover.el-popper {
  background: linear-gradient(180deg, #1e293b 0%, #0f172a 100%) !important;
  border: 1px solid rgba(148, 163, 184, 0.2) !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4) !important;
}
.player-bar-popover .el-slider__runway {
  background-color: rgba(148, 163, 184, 0.3) !important;
}
.player-bar-popover .el-slider__bar {
  background: linear-gradient(180deg, #4f46e5, #0ea5e9) !important;
}
</style>
