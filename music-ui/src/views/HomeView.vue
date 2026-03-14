<script setup>
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useAuthStore } from '../store/auth'
import { usePlayerStore } from '../store/player'
import { useRouter } from 'vue-router'

const authStore = useAuthStore()
const playerStore = usePlayerStore()
const router = useRouter()

const loadingUser = ref(false)
const userInfo = ref(null)

// 登录 / 注册弹窗相关
const authDialogVisible = ref(false)
const activeAuthTab = ref('login')

// 登录表单
const loginFormRef = ref()
const loginForm = ref({
  username: '',
  password: '',
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

// 注册表单
const registerFormRef = ref()
const registerForm = ref({
  username: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
})

const registerRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: ['blur', 'change'] },
  ],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (!value) {
          callback(new Error('请再次输入密码'))
        } else if (value !== registerForm.value.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
}

const sendingCode = ref(false)
const codeCountdown = ref(0)
let codeTimer = null

// 侧栏当前 tab：discover | playlists | recent
const currentTab = ref('discover')

// 歌曲列表（发现音乐）
const songs = ref([])
const loadingSongs = ref(false)

// 推荐歌单（发现页，最多 6 个，不足留空）
const recommendedPlaylists = ref([])
const loadingRecommend = ref(false)
const RECOMMEND_SLOTS = 6

// 推荐专辑（发现页，最多 6 个，不足留空）
const recommendedAlbums = ref([])
const loadingRecommendAlbums = ref(false)
const ALBUM_SLOTS = 6

// 我的歌单
const playlists = ref([])
const loadingPlaylists = ref(false)

// 最近播放（带歌曲信息）
const recentList = ref([])
const loadingRecent = ref(false)

// 搜索（歌曲 + 用户）
const searchKeyword = ref('')
const searchResults = ref([])
const searchUsers = ref([])
const loadingSearch = ref(false)
const hasSearched = ref(false)

const displayTitle = (title) => {
  if (!title) return ''
  return title.replace(/\.mp3$/i, '')
}

const fetchSongs = async () => {
  loadingSongs.value = true
  try {
    const res = await request.get('/song/list', {
      params: { pageNum: 1, pageSize: 20 },
    })
    const page = res.data?.data
    songs.value = page?.records || []
  } catch (e) {
    const msg = e.response?.data?.message || '获取歌曲列表失败'
    ElMessage.error(msg)
  } finally {
    loadingSongs.value = false
  }
}

const fetchRecommendPlaylists = async () => {
  loadingRecommend.value = true
  try {
    const res = await request.get('/playlist/recommend', { params: { limit: RECOMMEND_SLOTS } })
    const list = res.data?.data || []
    recommendedPlaylists.value = list
  } catch (e) {
    const msg = e.response?.data?.message || '获取推荐歌单失败'
    ElMessage.error(msg)
    recommendedPlaylists.value = []
  } finally {
    loadingRecommend.value = false
  }
}

// 固定 6 个槽位，有数据显示歌单卡片，无数据显示空位
const recommendSlots = computed(() => {
  const list = recommendedPlaylists.value.slice(0, RECOMMEND_SLOTS)
  const padded = [...list]
  while (padded.length < RECOMMEND_SLOTS) {
    padded.push(null)
  }
  return padded
})

const fetchRecommendAlbums = async () => {
  loadingRecommendAlbums.value = true
  try {
    const res = await request.get('/album/recommend', { params: { limit: ALBUM_SLOTS } })
    recommendedAlbums.value = res.data?.data || []
  } catch (e) {
    const msg = e.response?.data?.message || '获取推荐专辑失败'
    ElMessage.error(msg)
    recommendedAlbums.value = []
  } finally {
    loadingRecommendAlbums.value = false
  }
}

// 固定 6 个专辑槽位，不足留空
const albumSlots = computed(() => {
  const list = recommendedAlbums.value.slice(0, ALBUM_SLOTS)
  const padded = [...list]
  while (padded.length < ALBUM_SLOTS) {
    padded.push(null)
  }
  return padded
})

const fetchPlaylists = async () => {
  if (!userInfo.value?.id) return
  loadingPlaylists.value = true
  try {
    const res = await request.get('/playlist/list', { params: { id: userInfo.value.id } })
    playlists.value = res.data?.data || []
  } catch (e) {
    const msg = e.response?.data?.message || '获取歌单失败'
    ElMessage.error(msg)
  } finally {
    loadingPlaylists.value = false
  }
}

const fetchRecentSongs = async () => {
  if (!userInfo.value?.id) return
  loadingRecent.value = true
  try {
    const res = await request.get('/play-history/recentWithSongs', {
      params: { id: userInfo.value.id },
    })
    recentList.value = res.data?.data || []
  } catch (e) {
    const msg = e.response?.data?.message || '获取最近播放失败'
    ElMessage.error(msg)
  } finally {
    loadingRecent.value = false
  }
}

const doSearch = async () => {
  const kw = searchKeyword.value?.trim()
  if (kw === '') {
    hasSearched.value = false
    searchResults.value = []
    searchUsers.value = []
    return
  }
  loadingSearch.value = true
  hasSearched.value = true
  searchResults.value = []
  searchUsers.value = []
  try {
    const [songRes, userRes] = await Promise.all([
      request.get('/song/search', { params: { keyword: kw, pageNum: 1, pageSize: 30 } }),
      request.get('/user/search', { params: { keyword: kw } }),
    ])
    const page = songRes.data?.data
    searchResults.value = page?.records || []
    searchUsers.value = userRes.data?.data || []
  } catch (e) {
    const msg = e.response?.data?.message || '搜索失败'
    ElMessage.error(msg)
  } finally {
    loadingSearch.value = false
  }
}

const goUserHome = (id) => {
  router.push({ name: 'user-home', params: { id } })
}

// 收藏到歌单（首页歌曲行）
const showAddToPlaylistDialog = ref(false)
const songToAdd = ref(null)
const addToPlaylistList = ref([])
const loadingAddToPlaylist = ref(false)
const addingPlaylistId = ref(null)

const openAddToPlaylist = async (song) => {
  if (!authStore.isAuthenticated) {
    ElMessage.warning('请先登录后收藏到歌单')
    return
  }
  if (!song?.id) return
  songToAdd.value = song
  showAddToPlaylistDialog.value = true
  loadingAddToPlaylist.value = true
  addToPlaylistList.value = []
  try {
    if (!userInfo.value?.id) {
      const userRes = await request.get('/user/getUserInfo')
      userInfo.value = userRes.data?.data || null
    }
    const userId = userInfo.value?.id
    if (!userId) return
    const listRes = await request.get('/playlist/list', { params: { id: userId } })
    addToPlaylistList.value = listRes.data?.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '获取歌单失败')
  } finally {
    loadingAddToPlaylist.value = false
  }
}

const addSongToPlaylist = async (playlistId) => {
  if (!songToAdd.value?.id) return
  addingPlaylistId.value = playlistId
  try {
    await request.post('/playlist-song/add', {
      playlistId,
      songId: songToAdd.value.id,
    })
    ElMessage.success('已加入歌单')
    showAddToPlaylistDialog.value = false
    songToAdd.value = null
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '添加失败')
  } finally {
    addingPlaylistId.value = null
  }
}

const switchTab = (tab) => {
  currentTab.value = tab
  if (tab === 'playlists' && authStore.isAuthenticated && userInfo.value?.id) {
    fetchPlaylists()
  }
  if (tab === 'recent' && authStore.isAuthenticated && userInfo.value?.id) {
    fetchRecentSongs()
  }
}

const playSong = (song) => {
  if (!song?.audioUrl) {
    ElMessage.warning('该歌曲暂时没有音频地址')
    return
  }
  playerStore.playSong(song)
}

const fetchUserInfo = async () => {
  loadingUser.value = true
  try {
    const res = await request.get('/user/getUserInfo')
    // 后端 Result.success(ResponseEnum.SUCCESS, userService.fomrmatone(user));
    userInfo.value = res.data?.data || null
  } catch (e) {
    const msg = e.response?.data?.message || '获取用户信息失败'
    ElMessage.error(msg)
  } finally {
    loadingUser.value = false
  }
}

const openAuthDialog = () => {
  // 如果已登录，点击头像进入个人中心；未登录则弹出登录/注册弹窗
  if (authStore.isAuthenticated) {
    router.push('/profile')
  } else {
    authDialogVisible.value = true
  }
}

const startCodeCountdown = () => {
  codeCountdown.value = 120
  codeTimer = window.setInterval(() => {
    if (codeCountdown.value <= 1) {
      window.clearInterval(codeTimer)
      codeCountdown.value = 0
    } else {
      codeCountdown.value -= 1
    }
  }, 1000)
}

const handleSendCode = async () => {
  if (!registerForm.value.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (codeCountdown.value > 0 || sendingCode.value) return

  sendingCode.value = true
  try {
    await request.get('/sendEmailCode', {
      params: { email: registerForm.value.email },
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCodeCountdown()
  } catch (e) {
    const msg = e.response?.data?.message || '发送验证码失败'
    ElMessage.error(msg)
  } finally {
    sendingCode.value = false
  }
}

const handleLogin = () => {
  if (!loginFormRef.value) return
  loginFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const res = await request.post('/login', {
        username: loginForm.value.username,
        password: loginForm.value.password,
      })
      const headerToken = res.headers['token'] || res.headers['Token']
      const bodyToken = res.data?.data
      const token = headerToken || bodyToken
      if (!token) {
        ElMessage.error('登录成功，但未获取到 token')
        return
      }
      authStore.setToken(token)
      ElMessage.success('登录成功')
      authDialogVisible.value = false
      await fetchUserInfo()
      router.push('/')
    } catch (e) {
      const msg = e.response?.data?.message || '登录失败'
      ElMessage.error(msg)
    }
  })
}

const handleRegister = () => {
  if (!registerFormRef.value) return
  registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await request.post('/register', {
        username: registerForm.value.username,
        password: registerForm.value.password,
        email: registerForm.value.email,
        code: registerForm.value.code,
      })
      ElMessage.success('注册成功，请登录')
      activeAuthTab.value = 'login'
    } catch (e) {
      const msg = e.response?.data?.message || '注册失败'
      ElMessage.error(msg)
    }
  })
}

onMounted(async () => {
  if (authStore.isAuthenticated) {
    await fetchUserInfo()
  }
  fetchSongs()
  // 推荐歌单、推荐专辑：未登录也可获取
  fetchRecommendPlaylists()
  fetchRecommendAlbums()
})
</script>

<template>
  <div class="home-layout">
    <aside class="sidebar">
      <div class="logo">
        <span class="dot" />
        <span class="brand">Music Listen</span>
      </div>

      <nav class="nav">
        <div class="nav-group">
          <p class="nav-title">在线音乐</p>
          <ul>
            <li
              class="nav-item"
              :class="{ 'nav-item--active': currentTab === 'discover' }"
              @click="switchTab('discover')"
            >
              <span>发现音乐</span>
            </li>
            <li
              class="nav-item"
              :class="{ 'nav-item--active': currentTab === 'playlists' }"
              @click="switchTab('playlists')"
            >
              <span>我的歌单</span>
            </li>
            <li
              class="nav-item"
              :class="{ 'nav-item--active': currentTab === 'recent' }"
              @click="switchTab('recent')"
            >
              <span>最近播放</span>
            </li>
          </ul>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info" @click="openAuthDialog">
          <el-avatar
            class="avatar"
            :size="32"
            :src="userInfo?.avatarUrl"
          >
            {{ (userInfo?.username || '访').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="meta">
            <div class="name" :title="userInfo?.username || '未登录'">
              {{ userInfo?.username || '未登录' }}
            </div>
            <div class="role">
              {{
                userInfo
                  ? userInfo.isSinger
                    ? '认证歌手'
                    : '普通用户'
                  : '请登录以同步数据'
              }}
            </div>
          </div>
        </div>
      </div>
    </aside>

    <main class="main">
      <header class="main-header">
        <div class="main-header-left">
          <h1>{{ currentTab === 'discover' ? '发现音乐' : currentTab === 'playlists' ? '我的歌单' : '最近播放' }}</h1>
          <p v-if="currentTab === 'discover'">根据你的喜好，推荐精选歌单与最新专辑</p>
          <p v-else-if="currentTab === 'playlists'">管理并播放你的歌单</p>
          <p v-else>你最近听过的歌曲</p>
        </div>
        <div class="search-bar-wrap">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索歌曲、用户"
            clearable
            class="search-input"
            @keyup.enter="doSearch"
          >
            <template #append>
              <el-button :loading="loadingSearch" @click="doSearch">搜索</el-button>
            </template>
          </el-input>
        </div>
      </header>

      <!-- 搜索结果（有搜索关键词时优先展示：歌曲 + 用户） -->
      <section v-if="hasSearched" class="section">
        <div class="section-header">
          <h2>搜索结果</h2>
          <span class="section-subtitle">关键词「{{ searchKeyword }}」</span>
        </div>
        <div v-if="loadingSearch" class="songs-loading">搜索中...</div>
        <template v-else>
          <!-- 用户 -->
          <div v-if="searchUsers.length" class="search-block">
            <h3 class="search-block-title">用户</h3>
            <div class="search-users">
              <div
                v-for="u in searchUsers"
                :key="u.id"
                class="user-row"
                @click="goUserHome(u.id)"
              >
                <el-avatar :size="40" :src="u.avatarUrl" class="user-avatar">
                  {{ (u.username || '').charAt(0).toUpperCase() }}
                </el-avatar>
                <div class="user-meta">
                  <span class="user-name">{{ u.username }}</span>
                  <span class="user-tag">{{ u.isSinger ? '认证歌手' : '用户' }}</span>
                </div>
                <span class="user-go">进入主页 →</span>
              </div>
            </div>
          </div>
          <!-- 歌曲 -->
          <div class="search-block">
            <h3 class="search-block-title">歌曲</h3>
            <div v-if="!searchResults.length" class="songs-empty">未找到相关歌曲</div>
            <el-scrollbar v-else class="songs-list">
              <div
                v-for="song in searchResults"
                :key="song.id"
                class="song-row"
                @click="playSong(song)"
              >
                <div class="song-cover">
                  <img v-if="song.coverUrl" :src="song.coverUrl" alt="" />
                  <div v-else class="song-cover-placeholder" />
                </div>
                <div class="song-main">
                  <div class="song-title" :title="displayTitle(song.title)">
                    {{ displayTitle(song.title) }}
                  </div>
                  <div class="song-meta">播放量：{{ song.playCount }}</div>
                </div>
                <span class="song-actions">
                  <el-button type="primary" link @click.stop="playSong(song)">播放</el-button>
                  <el-button link @click.stop="openAddToPlaylist(song)">收藏</el-button>
                </span>
              </div>
            </el-scrollbar>
          </div>
          <div
            v-if="!searchUsers.length && !searchResults.length"
            class="songs-empty"
          >
            未找到相关歌曲或用户
          </div>
        </template>
      </section>

      <!-- 发现音乐 -->
      <template v-if="currentTab === 'discover' && !hasSearched">
        <section class="section">
          <div class="section-header">
            <h2>推荐歌单</h2>
            <span class="section-subtitle">随机为你挑选的用户歌单</span>
          </div>
          <div v-if="loadingRecommend" class="songs-loading">推荐歌单加载中...</div>
          <div v-else class="card-grid">
            <div
              v-for="(pl, idx) in recommendSlots"
              :key="pl ? pl.id : 'empty-' + idx"
              class="playlist-card"
              :class="{ 'playlist-card--clickable': pl, 'playlist-card--empty': !pl }"
              @click="pl && router.push({ name: 'playlist-detail', params: { id: pl.id } })"
            >
              <div class="cover">
                <img v-if="pl?.coverUrl" :src="pl.coverUrl" alt="" />
              </div>
              <div class="info">
                <div class="name" :title="pl?.name">{{ pl ? pl.name : '—' }}</div>
                <div class="meta">{{ pl ? (pl.type === 2 ? '收藏歌单' : '创建歌单') : '暂无' }}</div>
              </div>
            </div>
          </div>
        </section>

        <section class="section">
          <div class="section-header">
            <h2>热门歌曲</h2>
            <span class="section-subtitle">点击立即播放你喜欢的歌曲</span>
          </div>
          <div v-if="loadingSongs" class="songs-loading">歌曲加载中...</div>
          <div v-else-if="!songs.length" class="songs-empty">暂无歌曲数据，请先通过后台上传歌曲。</div>
          <el-scrollbar v-else class="songs-list">
            <div
              v-for="song in songs"
              :key="song.id"
              class="song-row"
              @click="playSong(song)"
            >
              <div class="song-cover">
                <img v-if="song.coverUrl" :src="song.coverUrl" alt="" />
                <div v-else class="song-cover-placeholder" />
              </div>
              <div class="song-main">
                <div class="song-title" :title="displayTitle(song.title)">
                  {{ displayTitle(song.title) }}
                </div>
                <div class="song-meta">播放量：{{ song.playCount }}</div>
              </div>
              <span class="song-actions">
                <el-button type="primary" link @click.stop="playSong(song)">播放</el-button>
                <el-button link @click.stop="openAddToPlaylist(song)">收藏</el-button>
              </span>
            </div>
          </el-scrollbar>
        </section>

        <section class="section">
          <div class="section-header">
            <h2>最新专辑</h2>
            <span class="section-subtitle">随机为你挑选的专辑</span>
          </div>
          <div v-if="loadingRecommendAlbums" class="songs-loading">推荐专辑加载中...</div>
          <div v-else class="card-grid card-grid--compact">
            <div
              v-for="(album, idx) in albumSlots"
              :key="album ? album.id : 'empty-' + idx"
              class="album-card"
              :class="{ 'album-card--clickable': album, 'album-card--empty': !album }"
              @click="album && router.push({ name: 'album-detail', params: { id: album.id } })"
            >
              <div class="cover-small">
                <img v-if="album?.coverUrl" :src="album.coverUrl" alt="" />
              </div>
              <div class="info">
                <div class="name" :title="album?.title">{{ album ? album.title : '—' }}</div>
                <div class="meta">{{ album ? '专辑' : '暂无' }}</div>
              </div>
            </div>
          </div>
        </section>
      </template>

      <!-- 我的歌单 -->
      <section v-if="currentTab === 'playlists' && !hasSearched" class="section">
        <div v-if="!authStore.isAuthenticated" class="songs-empty">
          请先登录后查看我的歌单
        </div>
        <div v-else-if="loadingPlaylists" class="songs-loading">歌单加载中...</div>
        <div v-else-if="!playlists.length" class="songs-empty">
          暂无歌单，去
          <el-button type="primary" link @click="router.push('/profile')">个人中心</el-button>
          创建歌单吧
        </div>
        <div v-else class="card-grid">
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
            </div>
          </div>
        </div>
      </section>

      <!-- 最近播放 -->
      <section v-if="currentTab === 'recent' && !hasSearched" class="section">
        <div v-if="!authStore.isAuthenticated" class="songs-empty">
          请先登录后查看最近播放
        </div>
        <div v-else-if="loadingRecent" class="songs-loading">加载中...</div>
        <div v-else-if="!recentList.length" class="songs-empty">暂无播放记录，去发现音乐听歌吧</div>
        <el-scrollbar v-else class="songs-list">
          <div
            v-for="item in recentList"
            :key="item.song?.id"
            class="song-row"
            @click="playSong(item.song)"
          >
            <div class="song-cover">
              <img v-if="item.song?.coverUrl" :src="item.song.coverUrl" alt="" />
              <div v-else class="song-cover-placeholder" />
            </div>
            <div class="song-main">
              <div class="song-title" :title="displayTitle(item.song?.title)">
                {{ displayTitle(item.song?.title) }}
              </div>
              <div class="song-meta">播放量：{{ item.song?.playCount }}</div>
            </div>
            <span class="song-actions">
              <el-button type="primary" link @click.stop="playSong(item.song)">播放</el-button>
              <el-button link @click.stop="openAddToPlaylist(item.song)">收藏</el-button>
            </span>
          </div>
        </el-scrollbar>
      </section>
    </main>

    <!-- 收藏到歌单弹窗（首页） -->
    <el-dialog
      v-model="showAddToPlaylistDialog"
      title="收藏到歌单"
      width="360px"
      class="add-to-playlist-dialog"
    >
      <div v-if="loadingAddToPlaylist" class="dialog-loading">加载中...</div>
      <div v-else-if="!addToPlaylistList.length" class="dialog-empty">
        暂无歌单，请先在个人中心创建歌单
      </div>
      <div v-else class="playlist-pick-list">
        <div
          v-for="pl in addToPlaylistList"
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

    <!-- 登录 / 注册弹窗 -->
    <el-dialog
      v-model="authDialogVisible"
      width="420px"
      :show-close="true"
      destroy-on-close
      align-center
    >
      <template #header>
        <div class="auth-header">
          <h3>账号中心</h3>
          <p>登录或注册以同步你的音乐数据</p>
        </div>
      </template>

      <el-tabs v-model="activeAuthTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            label-position="top"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="loginForm.username"
                placeholder="请输入用户名"
                autocomplete="username"
              />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
                autocomplete="current-password"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" style="width: 100%" @click="handleLogin">
                登录
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form
            ref="registerFormRef"
            :model="registerForm"
            :rules="registerRules"
            label-position="top"
          >
            <el-form-item label="用户名" prop="username">
              <el-input
                v-model="registerForm.username"
                placeholder="请输入用户名"
                autocomplete="username"
              />
            </el-form-item>
            <el-form-item label="邮箱" prop="email">
              <el-input
                v-model="registerForm.email"
                placeholder="请输入邮箱"
                autocomplete="email"
              />
            </el-form-item>
            <el-form-item label="邮箱验证码" prop="code">
              <div class="code-line">
                <el-input
                  v-model="registerForm.code"
                  placeholder="请输入验证码"
                  maxlength="6"
                />
                <el-button
                  class="code-btn"
                  :loading="sendingCode"
                  :disabled="!!codeCountdown"
                  @click="handleSendCode"
                >
                  <span v-if="!codeCountdown">获取验证码</span>
                  <span v-else>{{ codeCountdown }}s 后重发</span>
                </el-button>
              </div>
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input
                v-model="registerForm.password"
                type="password"
                show-password
                placeholder="请输入密码"
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input
                v-model="registerForm.confirmPassword"
                type="password"
                show-password
                placeholder="请再次输入密码"
                autocomplete="new-password"
              />
            </el-form-item>
            <el-form-item>
              <el-button
                type="primary"
                style="width: 100%"
                @click="handleRegister"
              >
                注册
              </el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<style scoped>
.home-layout {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  background: radial-gradient(circle at top left, #020617, #020617 40%, #020617);
  color: #e5e7eb;
}

.sidebar {
  border-right: 1px solid rgba(148, 163, 184, 0.25);
  padding: 18px 16px 16px;
  display: flex;
  flex-direction: column;
  backdrop-filter: blur(18px);
  background: radial-gradient(circle at top left, #020617, #020617 40%, #020617);
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 24px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: radial-gradient(circle, #38bdf8, #6366f1);
}

.brand {
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.nav {
  flex: 1;
}

.nav-group {
  margin-bottom: 24px;
}

.nav-title {
  margin: 0 0 8px;
  font-size: 12px;
  text-transform: uppercase;
  color: #6b7280;
  letter-spacing: 0.14em;
}

.nav-group ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.nav-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 14px;
  color: #9ca3af;
  cursor: pointer;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.nav-item span {
  flex: 1;
}

.nav-item:hover {
  background-color: rgba(148, 163, 184, 0.12);
  color: #e5e7eb;
}

.nav-item--active {
  background: linear-gradient(90deg, #4f46e5, #0ea5e9);
  color: #f9fafb;
}

.sidebar-footer {
  margin-top: 12px;
  border-top: 1px solid rgba(148, 163, 184, 0.25);
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
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
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.role {
  font-size: 11px;
  color: #9ca3af;
}

.auth-header h3 {
  margin: 0 0 4px;
  font-size: 18px;
  font-weight: 600;
}

.auth-header p {
  margin: 0;
  font-size: 12px;
  color: #9ca3af;
}

.code-line {
  display: flex;
  gap: 8px;
}

.code-btn {
  white-space: nowrap;
}

.main {
  padding: 20px 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  background: radial-gradient(circle at top left, #020617, #020617 40%, #020617);
}

.main-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.main-header-left {
  flex: 1;
  min-width: 0;
}

.search-bar-wrap {
  flex-shrink: 0;
  width: 280px;
}

.search-bar-wrap .search-input {
  width: 100%;
}

.main-header h1 {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.main-header p {
  margin: 0;
  font-size: 13px;
  color: #9ca3af;
}

.section {
  background: radial-gradient(circle at top left, #020617, #020617);
  border-radius: 16px;
  padding: 16px 16px 18px;
  border: 1px solid rgba(148, 163, 184, 0.3);
}

.section-header {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-bottom: 16px;
}

.section-header h2 {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  letter-spacing: -0.01em;
}

.section-subtitle {
  font-size: 12px;
  color: #9ca3af;
}

.search-block {
  margin-bottom: 20px;
}

.search-block:last-child {
  margin-bottom: 0;
}

.search-block-title {
  margin: 0 0 10px;
  font-size: 14px;
  font-weight: 600;
  color: #94a3b8;
}

.search-users {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.user-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.15s;
}

.user-row:hover {
  background-color: rgba(148, 163, 184, 0.15);
}

.user-avatar {
  background: radial-gradient(circle at 30% 20%, #38bdf8, #4f46e5);
  flex-shrink: 0;
}

.user-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #e5e7eb;
}

.user-tag {
  font-size: 11px;
  color: #9ca3af;
}

.user-go {
  font-size: 12px;
  color: #94a3b8;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 12px;
}

.card-grid--compact {
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
}

.playlist-card,
.album-card {
  border-radius: 12px;
  background: linear-gradient(145deg, rgba(15, 23, 42, 0.9), rgba(15, 23, 42, 0.95));
  border: 1px solid rgba(148, 163, 184, 0.35);
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition:
    transform 0.15s ease,
    box-shadow 0.15s ease,
    border-color 0.15s ease;
}

.playlist-card:hover,
.album-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 18px 45px rgba(15, 23, 42, 0.6);
  border-color: rgba(59, 130, 246, 0.7);
}

.cover {
  width: 100%;
  aspect-ratio: 1 / 1;
  border-radius: 10px;
  background-image: linear-gradient(135deg, #4f46e5, #ec4899, #22c55e);
  background-size: 200% 200%;
  animation: gradientMove 6s ease infinite;
  overflow: hidden;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.playlist-card--clickable {
  cursor: pointer;
}

.playlist-card--empty {
  cursor: default;
  opacity: 0.6;
}
.playlist-card--empty:hover {
  transform: none;
  box-shadow: none;
  border-color: rgba(148, 163, 184, 0.35);
}

.cover-small {
  width: 100%;
  aspect-ratio: 16 / 9;
  border-radius: 8px;
  background-image: linear-gradient(135deg, #0ea5e9, #6366f1);
  background-size: 180% 180%;
  animation: gradientMove 6s ease infinite;
  overflow: hidden;
}
.cover-small img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.album-card--clickable {
  cursor: pointer;
}
.album-card--empty {
  cursor: default;
  opacity: 0.6;
}
.album-card--empty:hover {
  transform: none;
  box-shadow: none;
  border-color: rgba(148, 163, 184, 0.35);
}

.info .name {
  font-size: 14px;
  font-weight: 500;
  color: #e5e7eb;
}

.info .meta {
  margin-top: 2px;
  font-size: 12px;
  color: #9ca3af;
}

.songs-loading,
.songs-empty {
  font-size: 13px;
  color: #9ca3af;
}

.songs-list {
  max-height: 260px;
}

.song-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 4px;
  border-bottom: 1px solid rgba(148, 163, 184, 0.25);
  cursor: pointer;
}

.song-row:last-of-type {
  border-bottom: none;
}

.song-row:hover {
  background-color: rgba(15, 23, 42, 0.6);
}

.song-cover {
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  border-radius: 6px;
  overflow: hidden;
  margin-right: 12px;
  background: rgba(30, 41, 59, 0.8);
}

.song-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.song-cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #334155, #475569);
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

.song-action,
.song-actions {
  flex-shrink: 0;
  margin-left: 8px;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.dialog-loading,
.dialog-empty {
  padding: 24px;
  text-align: center;
  color: #9ca3af;
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

@keyframes gradientMove {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

@media (max-width: 768px) {
  .home-layout {
    grid-template-columns: 1fr;
  }

  .sidebar {
    display: none;
  }

  .main {
    padding-inline: 16px;
  }
}
</style>

