import { defineStore } from 'pinia'
import request from '../utils/request'

export const usePlayerStore = defineStore('player', {
  state: () => ({
    playQueue: [],
    currentSong: null,
    playing: false,
    volume: 0.8,
    playMode: 'sequence', // sequence | shuffle
    currentTime: 0,
    duration: 0,
    isSeeking: false,
  }),
  getters: {
    currentIndex(state) {
      if (!state.currentSong) return -1
      return state.playQueue.findIndex((s) => s.id === state.currentSong.id)
    },
    progressPercent(state) {
      if (!state.duration) return 0
      return (state.currentTime / state.duration) * 100
    },
    formattedCurrentTime(state) {
      return formatTime(state.currentTime)
    },
    formattedDuration(state) {
      return formatTime(state.duration)
    },
    playModeLabel(state) {
      return state.playMode === 'sequence' ? '顺序播放' : '随机播放'
    },
    playModeIcon(state) {
      return state.playMode === 'sequence' ? '🔁' : '🔀'
    },
  },
  actions: {
    displayTitle(title) {
      if (!title) return ''
      return title.replace(/\.mp3$/i, '')
    },
    setCurrentTime(t) {
      this.currentTime = t
    },
    setDuration(d) {
      this.duration = d
    },
    setIsSeeking(v) {
      this.isSeeking = v
    },
    setVolume(v) {
      this.volume = v
    },
    playSong(song) {
      if (!song?.audioUrl) return
      // 记录播放量，并用返回的新播放量更新当前展示（主页等与 song 同引用处会同步）
      if (song?.id) {
        request.post(`/song/${song.id}/play`).then((res) => {
          const newCount = res?.data?.data
          if (newCount != null && this.currentSong && this.currentSong.id === song.id) {
            this.currentSong.playCount = newCount
          }
        }).catch(() => {})
      }
      // 已登录时记录播放历史（静默失败，不影响播放）
      if (song?.id && typeof window !== 'undefined' && window.localStorage?.getItem('music_token')) {
        request.post('/play-history/addCurrent', { songId: song.id }).catch(() => {})
      }
      if (!this.playQueue.length) {
        this.playQueue = [song]
        this.currentSong = song
        this.playing = true
        return
      }
      const existIdx = this.playQueue.findIndex((s) => s.id === song.id)
      if (existIdx !== -1) {
        this.playByIndex(existIdx)
        return
      }
      this.playQueue.push(song)
      this.playByIndex(this.playQueue.length - 1)
    },
    playByIndex(index) {
      if (!this.playQueue.length) return
      const safeIndex =
        ((index % this.playQueue.length) + this.playQueue.length) %
        this.playQueue.length
      this.currentSong = this.playQueue[safeIndex]
      this.playing = true
      const song = this.currentSong
      if (song?.id) {
        request.post(`/song/${song.id}/play`).then((res) => {
          const newCount = res?.data?.data
          if (newCount != null && this.currentSong && this.currentSong.id === song.id) {
            this.currentSong.playCount = newCount
          }
        }).catch(() => {})
      }
      if (song?.id && typeof window !== 'undefined' && window.localStorage?.getItem('music_token')) {
        request.post('/play-history/addCurrent', { songId: song.id }).catch(() => {})
      }
    },
    togglePlay() {
      this.playing = !this.playing
    },
    setPlaying(v) {
      this.playing = v
    },
    playNext() {
      if (!this.playQueue.length) return
      if (this.playMode === 'shuffle') {
        if (this.playQueue.length === 1) {
          this.playByIndex(0)
          return
        }
        let idx
        const cur = this.currentIndex
        do {
          idx = Math.floor(Math.random() * this.playQueue.length)
        } while (idx === cur && this.playQueue.length > 1)
        this.playByIndex(idx)
      } else {
        const nextIndex = this.currentIndex === -1 ? 0 : this.currentIndex + 1
        this.playByIndex(nextIndex)
      }
    },
    playPrev() {
      if (!this.playQueue.length) return
      if (this.playMode === 'shuffle') {
        if (this.playQueue.length === 1) {
          this.playByIndex(0)
          return
        }
        let idx
        const cur = this.currentIndex
        do {
          idx = Math.floor(Math.random() * this.playQueue.length)
        } while (idx === cur && this.playQueue.length > 1)
        this.playByIndex(idx)
      } else {
        const prevIndex = this.currentIndex <= 0 ? this.playQueue.length - 1 : this.currentIndex - 1
        this.playByIndex(prevIndex)
      }
    },
    togglePlayMode() {
      this.playMode = this.playMode === 'sequence' ? 'shuffle' : 'sequence'
    },
  },
})

function formatTime(sec) {
  if (!sec || Number.isNaN(sec)) return '00:00'
  const m = Math.floor(sec / 60)
  const s = Math.floor(sec % 60)
  return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`
}
