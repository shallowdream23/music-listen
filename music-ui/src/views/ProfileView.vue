<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useDownloadStore } from '../store/download'

const router = useRouter()
const authStore = useAuthStore()
const downloadStore = useDownloadStore()
const loading = ref(false)
const userInfo = ref(null)

// 统计信息：关注、粉丝、歌单、播放历史
const statsLoading = ref(false)
const stats = ref({
  followCount: 0,
  followerCount: 0,
  playlistCount: 0,
  historyCount: 0,
})

// 歌单列表（后端 type: 1 创建歌单 2 收藏歌单）
const playlists = ref([])
const createdPlaylists = computed(() => playlists.value.filter((p) => p.type !== 2))
const collectedPlaylists = computed(() => playlists.value.filter((p) => p.type === 2))

// 更新账户信息 / 更改密码弹窗
const updateUserDialogVisible = ref(false)
const passwordDialogVisible = ref(false)

const updateUserForm = ref({
  id: null,
  username: '',
  avatarUrl: '',
  profile: '',
})
const updateUserRules = {
  username: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' },
  ],
}
const updateUserFormRef = ref()
const avatarUploading = ref(false)
const avatarFileInput = ref(null)

const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
  code: '',
})
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入旧密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (_, value, callback) => {
        if (value !== passwordForm.value.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
  code: [{ required: true, message: '请输入邮箱验证码', trigger: 'blur' }],
}
const passwordFormRef = ref()
const sendingCode = ref(false)
const codeCountdown = ref(0)
let codeTimer = null

// 创建 / 收藏歌单弹窗
const createDialogVisible = ref(false)
const collectDialogVisible = ref(false)

const createForm = ref({
  name: '',
  coverUrl: '',
  isPublic: true,
})

const createRules = {
  name: [{ required: true, message: '请输入歌单名称', trigger: 'blur' }],
}

const collectForm = ref({
  playlistId: '',
})

const collectRules = {
  playlistId: [{ required: true, message: '请输入歌单 ID', trigger: 'blur' }],
}

const createFormRef = ref()
const collectFormRef = ref()
const createPlaylistCoverFileInput = ref(null)
const createPlaylistCoverUploading = ref(false)

// 下载设置：通过选择文件夹设置（downloadStore）
const choosingFolder = ref(false)
const isFolderPickerSupported = ref(true)

const triggerCreatePlaylistCoverUpload = () => createPlaylistCoverFileInput.value?.click()
const onCreatePlaylistCoverFileChange = async (e) => {
  const file = e.target?.files?.[0]
  if (!file) return
  createPlaylistCoverUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/user/uploadImage', formData)
    const url = res.data?.data
    if (url) createForm.value.coverUrl = url
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '封面上传失败')
  } finally {
    createPlaylistCoverUploading.value = false
    e.target.value = ''
  }
}

// 我的上传（歌曲）
const myUploads = ref([])
const myUploadsLoading = ref(false)
const uploadDialogVisible = ref(false)
const uploadForm = ref({
  file: null,
  title: '',
  albumName: '',
  coverUrl: '',
  lyricFile: null, // 歌词文件（.lrc 等）
})
const uploadFormRef = ref()
const uploadRules = {
  file: [{ required: true, message: '请选择音频文件', trigger: 'change' }],
}
const uploading = ref(false)
const uploadFileInput = ref(null)
const lyricFileInput = ref(null)

const fetchUserInfo = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/getUserInfo')
    userInfo.value = res.data?.data || null
    await downloadStore.loadStoredHandle()

    // 基于用户 ID 拉取统计数据、我的上传、我的专辑（歌手）
    if (userInfo.value?.id) {
      await fetchStats(userInfo.value.id)
      await fetchMyUploads()
      if (userInfo.value.isSinger === 1) await fetchMyAlbums()
    }
  } catch (e) {
    const msg = e.response?.data?.message || '获取用户信息失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const onChooseFolder = async () => {
  choosingFolder.value = true
  try {
    const result = await downloadStore.chooseFolder()
    if (result.ok) {
      ElMessage.success(result.name ? `已选择文件夹：${result.name}` : '已选择文件夹')
    } else if (result.message && !result.message.includes('取消')) {
      ElMessage.warning(result.message)
    }
  } finally {
    choosingFolder.value = false
  }
}

const onClearDownloadFolder = async () => {
  await downloadStore.clearFolder()
  ElMessage.success('已清除下载文件夹选择')
}

// 获取统计数据
const fetchStats = async (userId) => {
  statsLoading.value = true
  try {
    // 关注/粉丝数量
    const followRes = await request.get('/follow/tabCount', {
      params: { id: userId },
    })
    const followData = followRes.data?.data || {}
    stats.value.followCount = followData.followCount ?? followData.follow ?? 0
    stats.value.followerCount =
      followData.fansCount ?? followData.followerCount ?? followData.followee ?? 0

    // 歌单数量及列表
    const playlistRes = await request.get('/playlist/list', {
      params: { id: userId },
    })
    const playlistData = playlistRes.data?.data || []
    playlists.value = Array.isArray(playlistData) ? playlistData : []
    stats.value.playlistCount = playlists.value.length

    // 播放历史数量
    const historyRes = await request.get('/play-history/list', {
      params: { id: userId },
    })
    const histories = historyRes.data?.data || []
    stats.value.historyCount = Array.isArray(histories) ? histories.length : 0
  } catch (e) {
    // 统计信息失败不影响基本信息展示
    // 仅轻量提示一次
    // console 可帮助调试
    // eslint-disable-next-line no-console
    console.warn('获取统计信息失败', e)
  } finally {
    statsLoading.value = false
  }
}

const fetchMyUploads = async () => {
  myUploadsLoading.value = true
  try {
    const res = await request.get('/song/my-uploads')
    myUploads.value = res.data?.data || []
  } catch {
    myUploads.value = []
  } finally {
    myUploadsLoading.value = false
  }
}

// 我的专辑（歌手）
const myAlbums = ref([])
const myAlbumsLoading = ref(false)
const albumCreateVisible = ref(false)
const albumCreateForm = ref({ title: '', coverUrl: '', description: '' })
const albumCreateRules = { title: [{ required: true, message: '请输入专辑名', trigger: 'blur' }] }
const albumCreateRef = ref()
const albumCreateSubmitting = ref(false)
const albumCoverFileInput = ref(null)
const albumCoverUploading = ref(false)
const albumDetailVisible = ref(false)
const currentAlbum = ref(null)
const albumSongs = ref([])
const albumSongsLoading = ref(false)
const addSongToAlbumVisible = ref(false)
const addSongToAlbumSubmitting = ref(false)

const fetchMyAlbums = async () => {
  if (!userInfo.value?.id) return
  myAlbumsLoading.value = true
  try {
    const res = await request.get('/album/listById', { params: { id: userInfo.value.id } })
    myAlbums.value = res.data?.data || []
  } catch {
    myAlbums.value = []
  } finally {
    myAlbumsLoading.value = false
  }
}

const openAlbumCreate = () => {
  albumCreateForm.value = { title: '', coverUrl: '', description: '' }
  albumCreateVisible.value = true
}

const triggerAlbumCoverUpload = () => albumCoverFileInput.value?.click()

const onAlbumCoverFileChange = async (e) => {
  const file = e.target?.files?.[0]
  if (!file) return
  albumCoverUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/user/uploadImage', formData)
    const url = res.data?.data
    if (url) albumCreateForm.value.coverUrl = url
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '封面上传失败')
  } finally {
    albumCoverUploading.value = false
    e.target.value = ''
  }
}

const submitAlbumCreate = async () => {
  if (!albumCreateRef.value) return
  await albumCreateRef.value.validate(async (valid) => {
    if (!valid) return
    albumCreateSubmitting.value = true
    try {
      await request.post('/album/add', { ...albumCreateForm.value, singerId: userInfo.value.id })
      ElMessage.success('专辑创建成功，等待审核')
      albumCreateVisible.value = false
      await fetchMyAlbums()
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '创建失败')
    } finally {
      albumCreateSubmitting.value = false
    }
  })
}

const goToAlbumDetail = (album) => {
  if (album?.id) router.push({ name: 'album-detail', params: { id: String(album.id) } })
}

const openAlbumDetail = async (album) => {
  currentAlbum.value = album
  albumDetailVisible.value = true
  albumSongsLoading.value = true
  try {
    const res = await request.get('/album', { params: { id: album.id } })
    albumSongs.value = res.data?.data || []
  } catch {
    albumSongs.value = []
  } finally {
    albumSongsLoading.value = false
  }
}

const openAddSongToAlbum = () => {
  addSongToAlbumVisible.value = true
}

const myUploadsNotInAlbum = () => {
  const inAlbum = new Set((albumSongs.value || []).map((s) => s.id))
  return myUploads.value.filter((s) => !inAlbum.has(s.id))
}

const addSongToAlbum = async (song) => {
  if (!currentAlbum.value?.id) return
  addSongToAlbumSubmitting.value = true
  try {
    await request.post(`/album/${currentAlbum.value.id}/songs`, { songId: song.id })
    ElMessage.success('已加入专辑')
    addSongToAlbumVisible.value = false
    const res = await request.get('/album', { params: { id: currentAlbum.value.id } })
    albumSongs.value = res.data?.data || []
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  } finally {
    addSongToAlbumSubmitting.value = false
  }
}

const removeSongFromAlbum = async (song) => {
  try {
    await request.post(`/album/${currentAlbum.value.id}/songs/remove`, { songId: song.id })
    ElMessage.success('已从专辑移除')
    albumSongs.value = albumSongs.value.filter((s) => s.id !== song.id)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

const albumStatusText = (s) => (s === 1 ? '已通过' : '待审核')

const songCoverFileInput = ref(null)
const songCoverUploading = ref(false)

const openUploadDialog = () => {
  uploadForm.value = { file: null, title: '', albumName: '', coverUrl: '', lyricFile: null }
  uploadDialogVisible.value = true
}

const triggerSongCoverUpload = () => songCoverFileInput.value?.click()
const onSongCoverFileChange = async (e) => {
  const file = e.target?.files?.[0]
  if (!file) return
  songCoverUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/user/uploadImage', formData)
    const url = res.data?.data
    if (url) uploadForm.value.coverUrl = url
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '封面上传失败')
  } finally {
    songCoverUploading.value = false
    e.target.value = ''
  }
}

const onUploadFileChange = (e) => {
  const f = e.target?.files?.[0]
  uploadForm.value.file = f || null
  e.target.value = ''
}

const onLyricFileChange = (e) => {
  const f = e.target?.files?.[0]
  uploadForm.value.lyricFile = f || null
  e.target.value = ''
}

/** 将歌词文件内容读成字符串 */
const readLyricFileAsText = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result ?? '')
    reader.onerror = () => reject(new Error('读取歌词文件失败'))
    reader.readAsText(file, 'UTF-8')
  })
}

const handleUploadSong = async () => {
  if (!uploadForm.value.file) {
    ElMessage.warning('请选择音频文件')
    return
  }
  uploading.value = true
  try {
    let lyricText = null
    if (uploadForm.value.lyricFile) {
      lyricText = await readLyricFileAsText(uploadForm.value.lyricFile)
      lyricText = lyricText?.trim() || null
    }
    const formData = new FormData()
    formData.append('file', uploadForm.value.file)
    const songDto = {
      title: uploadForm.value.title?.trim() || null,
      albumName: uploadForm.value.albumName?.trim() || null,
      coverUrl: uploadForm.value.coverUrl?.trim() || null,
      lyric: lyricText,
    }
    formData.append('songDto', new Blob([JSON.stringify(songDto)], { type: 'application/json' }))
    await request.post('/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    ElMessage.success('上传成功，歌曲将进入待审核，通过后会在首页展示')
    uploadDialogVisible.value = false
    await fetchMyUploads()
  } catch (e) {
    const msg = e.response?.data?.message || '上传失败'
    ElMessage.error(msg)
  } finally {
    uploading.value = false
  }
}

const statusText = (status) => (status === 1 ? '已上架' : '待审核')

// 我的上传：编辑 / 删除
const editSongDialogVisible = ref(false)
const editSongForm = ref({
  id: null,
  title: '',
  coverUrl: '',
  lyricFile: null,
  existingLyric: '',
})
const editSongCoverFileInput = ref(null)
const editSongCoverUploading = ref(false)
const editSongLyricFileInput = ref(null)
const editSongSubmitting = ref(false)

const openEditSong = (row) => {
  editSongForm.value = {
    id: row.id,
    title: row.title || '',
    coverUrl: row.coverUrl || '',
    lyricFile: null,
    existingLyric: row.lyric != null ? row.lyric : '',
  }
  editSongDialogVisible.value = true
}

const triggerEditSongCoverUpload = () => editSongCoverFileInput.value?.click()
const onEditSongCoverFileChange = async (e) => {
  const file = e.target?.files?.[0]
  if (!file) return
  editSongCoverUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/user/uploadImage', formData)
    const url = res.data?.data
    if (url) editSongForm.value.coverUrl = url
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '封面上传失败')
  } finally {
    editSongCoverUploading.value = false
    e.target.value = ''
  }
}

const onEditSongLyricFileChange = (e) => {
  const f = e.target?.files?.[0]
  editSongForm.value.lyricFile = f || null
  e.target.value = ''
}

const submitEditSong = async () => {
  editSongSubmitting.value = true
  try {
    let lyricText = editSongForm.value.existingLyric ?? ''
    if (editSongForm.value.lyricFile) {
      lyricText = await readLyricFileAsText(editSongForm.value.lyricFile)
      lyricText = (lyricText || '').trim()
    }
    await request.put(`/song/${editSongForm.value.id}`, {
      id: editSongForm.value.id,
      title: (editSongForm.value.title || '').trim() || undefined,
      coverUrl: editSongForm.value.coverUrl?.trim() || undefined,
      lyric: lyricText || undefined,
    })
    ElMessage.success('保存成功')
    editSongDialogVisible.value = false
    await fetchMyUploads()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    editSongSubmitting.value = false
  }
}

const handleDeleteSong = async (row) => {
  try {
    await ElMessageBox.confirm('确定删除该歌曲吗？删除后无法恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await request.delete(`/song/${row.id}`)
    ElMessage.success('已删除')
    await fetchMyUploads()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '删除失败')
  }
}

onMounted(async () => {
  if (typeof window !== 'undefined') isFolderPickerSupported.value = !!window.showDirectoryPicker
  await fetchUserInfo()
})

const goBackHome = () => {
  router.push('/')
}

// 跳转关注/粉丝列表
const goFollowList = () => {
  router.push({ name: 'follow-list' })
}

const goFollowerList = () => {
  router.push({ name: 'follower-list' })
}

const goPlaylistDetail = (id) => {
  if (id) router.push({ name: 'playlist-detail', params: { id } })
}

// 打开创建 / 收藏歌单弹窗
const openCreateDialog = () => {
  createDialogVisible.value = true
}

const openCollectDialog = () => {
  collectDialogVisible.value = true
}

// 退出登录
const handleLogout = () => {
  authStore.logout()
  userInfo.value = null
  router.push('/')
  ElMessage.success('已退出登录')
}

// 打开更新账户信息弹窗（回填当前信息）
const openUpdateUserDialog = () => {
  if (!userInfo.value) return
  updateUserForm.value = {
    id: userInfo.value.id,
    username: userInfo.value.username || '',
    avatarUrl: userInfo.value.avatarUrl || '',
    profile: userInfo.value.profile || '',
  }
  updateUserDialogVisible.value = true
}

// 触发选择头像文件
const triggerAvatarUpload = () => {
  avatarFileInput.value?.click()
}

// 选择头像文件后上传
const onAvatarFileChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file || !file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await request.post('/user/uploadAvatar', formData)
    const url = res.data?.data
    if (url) {
      updateUserForm.value.avatarUrl = url
      ElMessage.success('头像上传成功')
    }
  } catch (err) {
    const msg = err.response?.data?.message || '头像上传失败'
    ElMessage.error(msg)
  } finally {
    avatarUploading.value = false
    e.target.value = ''
  }
}

// 提交更新账户信息
const handleUpdateUser = () => {
  if (!updateUserFormRef.value) return
  updateUserFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      const payload = {
        id: updateUserForm.value.id,
        username: updateUserForm.value.username,
        avatarUrl: updateUserForm.value.avatarUrl ?? '',
        profile: updateUserForm.value.profile ?? '',
      }
      await request.post('/user/updateUserInfo', payload)
      ElMessage.success('更新成功')
      updateUserDialogVisible.value = false
      await fetchUserInfo()
    } catch (e) {
      const msg = e.response?.data?.message || '更新失败'
      ElMessage.error(msg)
    }
  })
}

// 打开更改密码弹窗
const openPasswordDialog = () => {
  passwordForm.value = {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    code: '',
  }
  codeCountdown.value = 0
  if (codeTimer) {
    clearInterval(codeTimer)
    codeTimer = null
  }
  passwordDialogVisible.value = true
}

const startCodeCountdown = () => {
  codeCountdown.value = 120
  codeTimer = setInterval(() => {
    if (codeCountdown.value <= 1) {
      clearInterval(codeTimer)
      codeTimer = null
      codeCountdown.value = 0
    } else {
      codeCountdown.value -= 1
    }
  }, 1000)
}

// 获取修改密码验证码
const handleSendPasswordCode = async () => {
  if (sendingCode.value || codeCountdown.value > 0) return
  sendingCode.value = true
  try {
    await request.get('/user/sendPasswordCode')
    ElMessage.success('验证码已发送，请查收邮箱')
    startCodeCountdown()
  } catch (e) {
    const msg = e.response?.data?.message || '发送验证码失败'
    ElMessage.error(msg)
  } finally {
    sendingCode.value = false
  }
}

// 提交更改密码
const handleUpdatePassword = () => {
  if (!passwordFormRef.value) return
  passwordFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await request.post('/user/updatePassword', {
        userId: userInfo.value.id,
        oldPassword: passwordForm.value.oldPassword,
        newPassword: passwordForm.value.newPassword,
        code: passwordForm.value.code,
      })
      ElMessage.success('密码修改成功，请重新登录')
      passwordDialogVisible.value = false
      handleLogout()
    } catch (e) {
      const msg = e.response?.data?.message || '修改密码失败'
      ElMessage.error(msg)
    }
  })
}

// 提交创建歌单
const handleCreatePlaylist = () => {
  if (!createFormRef.value) return
  createFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await request.post('/playlist/create', {
        name: createForm.value.name,
        coverUrl: createForm.value.coverUrl || null,
        isPublic: createForm.value.isPublic ? 1 : 0,
      })
      ElMessage.success('创建歌单成功')
      createDialogVisible.value = false
      // 重新加载歌单列表
      if (userInfo.value?.id) {
        await fetchStats(userInfo.value.id)
      }
      // 重置表单
      createForm.value = { name: '', coverUrl: '', isPublic: true }
    } catch (e) {
      const msg = e.response?.data?.message || '创建歌单失败'
      ElMessage.error(msg)
    }
  })
}

// 提交收藏别人的歌单
const handleCollectPlaylist = () => {
  if (!collectFormRef.value) return
  collectFormRef.value.validate(async (valid) => {
    if (!valid) return
    try {
      await request.post('/playlist/collect', {
        playlistId: Number(collectForm.value.playlistId),
      })
      ElMessage.success('收藏歌单成功')
      collectDialogVisible.value = false
      if (userInfo.value?.id) {
        await fetchStats(userInfo.value.id)
      }
      collectForm.value = {
        playlistId: '',
      }
    } catch (e) {
      const msg = e.response?.data?.message || '收藏歌单失败'
      ElMessage.error(msg)
    }
  })
}
</script>

<template>
  <div class="profile-page">
    <header class="header">
      <div>
        <h1>个人中心</h1>
        <p>管理你的账号信息和个人资料</p>
      </div>
      <div class="header-actions">
        <el-button @click="goBackHome">返回首页</el-button>
        <el-button v-if="authStore.isAdmin" type="primary" @click="router.push('/admin')">管理后台</el-button>
        <el-button @click="openUpdateUserDialog">更新账户信息</el-button>
        <el-button @click="openPasswordDialog">更改密码</el-button>
        <el-button type="danger" plain @click="handleLogout">退出登录</el-button>
      </div>
    </header>

    <main class="content">
      <!-- 顶部：基本信息 -->
      <el-card class="card card-basic" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>基本信息</span>
          </div>
        </template>

        <div v-if="loading" class="loading">
          加载中...
        </div>
        <div v-else-if="userInfo" class="basic-info">
          <el-avatar
            :size="64"
            :src="userInfo.avatarUrl"
            class="avatar"
          >
            {{ (userInfo.username || '').charAt(0).toUpperCase() }}
          </el-avatar>
          <div class="meta">
            <div class="name">
              {{ userInfo.username }}
            </div>
            <div class="desc">
              {{ userInfo.profile || '这个人很懒，还没有写简介。' }}
            </div>
            <div class="tags">
              <el-tag size="small" type="success">
                {{ userInfo.isSinger ? '认证歌手' : '普通用户' }}
              </el-tag>
            </div>
          </div>
        </div>
        <div v-else class="empty">
          未获取到用户信息
        </div>
      </el-card>

      <!-- 中间：账号概览 -->
      <el-card class="card card-stats" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>账号概览</span>
          </div>
        </template>
        <div v-if="statsLoading" class="loading">统计信息加载中...</div>
        <div v-else class="stats-grid">
          <div class="stat-item stat-item--link" @click="goFollowList">
            <div class="stat-label">关注</div>
            <div class="stat-value">{{ stats.followCount }}</div>
          </div>
          <div class="stat-item stat-item--link" @click="goFollowerList">
            <div class="stat-label">粉丝</div>
            <div class="stat-value">{{ stats.followerCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">歌单</div>
            <div class="stat-value">{{ stats.playlistCount }}</div>
          </div>
          <div class="stat-item">
            <div class="stat-label">播放记录</div>
            <div class="stat-value">{{ stats.historyCount }}</div>
          </div>
        </div>
      </el-card>

      <!-- 下载设置 -->
      <el-card class="card card-download" shadow="hover">
        <template #header>
          <div class="card-header">
            <span>下载设置</span>
          </div>
        </template>
        <div class="download-settings">
          <p class="download-hint">选择电脑上的文件夹后，在播放页点击「下载」将把歌曲保存到该文件夹。未选择时保存到浏览器默认下载位置。</p>
          <div class="download-path-row">
            <el-button type="primary" :loading="choosingFolder" @click="onChooseFolder">
              {{ downloadStore.hasChosenFolder ? '重新选择文件夹' : '从电脑选择文件夹' }}
            </el-button>
            <el-button v-if="downloadStore.hasChosenFolder" @click="onClearDownloadFolder">清除选择</el-button>
            <span v-if="downloadStore.hasChosenFolder" class="download-folder-name">已选：{{ downloadStore.directoryName || '当前文件夹' }}</span>
          </div>
          <p v-if="!isFolderPickerSupported" class="download-unsupported">当前浏览器不支持选择文件夹，请使用 Chrome 或 Edge。未选择时下载将保存到浏览器默认下载目录。</p>
        </div>
      </el-card>

      <!-- 我创建的歌单 -->
      <el-card class="card card-activity" shadow="hover">
        <template #header>
          <div class="card-header card-header--with-actions">
            <span>我创建的歌单</span>
            <div class="card-actions">
              <el-button size="small" type="primary" @click="openCreateDialog">
                创建歌单
              </el-button>
            </div>
          </div>
        </template>
        <div v-if="!createdPlaylists.length" class="empty">
          暂无创建的歌单，点击「创建歌单」开始。
        </div>
        <div v-else class="playlist-grid">
          <div
            v-for="item in createdPlaylists"
            :key="item.id"
            class="playlist-item playlist-item--clickable"
            @click="goPlaylistDetail(item.id)"
          >
            <div class="playlist-cover">
              <img v-if="item.coverUrl" :src="item.coverUrl" alt="cover" />
              <div v-else class="fallback" />
            </div>
            <div class="playlist-meta">
              <div class="playlist-name" :title="item.name">{{ item.name }}</div>
              <div class="playlist-type">创建歌单</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 我收藏的歌单 -->
      <el-card class="card card-activity" shadow="hover">
        <template #header>
          <div class="card-header card-header--with-actions">
            <span>我收藏的歌单</span>
            <div class="card-actions">
              <el-button size="small" type="primary" @click="openCollectDialog">
                收藏歌单
              </el-button>
            </div>
          </div>
        </template>
        <div v-if="!collectedPlaylists.length" class="empty">
          暂无收藏的歌单，点击「收藏歌单」输入歌单 ID 收藏别人的歌单。
        </div>
        <div v-else class="playlist-grid">
          <div
            v-for="item in collectedPlaylists"
            :key="item.id"
            class="playlist-item playlist-item--clickable"
            @click="goPlaylistDetail(item.id)"
          >
            <div class="playlist-cover">
              <img v-if="item.coverUrl" :src="item.coverUrl" alt="cover" />
              <div v-else class="fallback" />
            </div>
            <div class="playlist-meta">
              <div class="playlist-name" :title="item.name">{{ item.name }}</div>
              <div class="playlist-type">收藏歌单</div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 我的上传（歌曲，需审核） -->
      <el-card class="card card-activity" shadow="hover">
        <template #header>
          <div class="card-header card-header--with-actions">
            <span>我的上传</span>
            <div class="card-actions">
              <el-button size="small" type="primary" @click="openUploadDialog">
                上传歌曲
              </el-button>
            </div>
          </div>
        </template>
        <p class="section-desc">上传的歌曲需审核通过后才会在首页展示</p>
        <div v-if="myUploadsLoading" class="loading">加载中...</div>
        <div v-else-if="!myUploads.length" class="empty">
          暂无上传，点击「上传歌曲」发布你的作品
        </div>
        <el-table v-else :data="myUploads" stripe class="uploads-table">
          <el-table-column label="封面" width="72" align="center">
            <template #default="{ row }">
              <div class="upload-cover-cell">
                <img v-if="row.coverUrl" :src="row.coverUrl" alt="" class="upload-cover-img" />
                <div v-else class="upload-cover-placeholder" />
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="歌曲名称" min-width="160" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
                {{ statusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="playCount" label="播放量" width="90" />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="openEditSong(row)">编辑</el-button>
              <el-button type="danger" link size="small" @click="handleDeleteSong(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>

      <!-- 我的专辑（仅歌手可见） -->
      <el-card v-if="userInfo && userInfo.isSinger === 1" class="card card-activity" shadow="hover">
        <template #header>
          <div class="card-header card-header--with-actions">
            <span>我的专辑</span>
            <div class="card-actions">
              <el-button size="small" type="primary" @click="openAlbumCreate">
                创建专辑
              </el-button>
            </div>
          </div>
        </template>
        <p class="section-desc">创建专辑后可把已上传的歌曲加入专辑，专辑需审核通过后展示</p>
        <div v-if="myAlbumsLoading" class="loading">加载中...</div>
        <div v-else-if="!myAlbums.length" class="empty">
          暂无专辑，点击「创建专辑」开始
        </div>
        <div v-else class="playlist-grid">
          <div
            v-for="item in myAlbums"
            :key="item.id"
            class="playlist-item playlist-item--clickable album-card"
            @click="goToAlbumDetail(item)"
          >
            <div class="playlist-cover">
              <img v-if="item.coverUrl" :src="item.coverUrl" alt="cover" />
              <div v-else class="fallback" />
            </div>
            <div class="playlist-meta">
              <div class="playlist-name" :title="item.title">{{ item.title }}</div>
              <div class="playlist-type">
                <el-tag :type="item.status === 1 ? 'success' : 'warning'" size="small">
                  {{ albumStatusText(item.status) }}
                </el-tag>
                <el-button
                  v-if="userInfo && userInfo.isSinger === 1"
                  type="primary"
                  link
                  size="small"
                  class="album-manage-btn"
                  @click.stop="openAlbumDetail(item)"
                >
                  管理
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <!-- 创建专辑弹窗 -->
      <el-dialog v-model="albumCreateVisible" title="创建专辑" width="440px" :close-on-click-modal="false">
        <el-form ref="albumCreateRef" :model="albumCreateForm" :rules="albumCreateRules" label-position="top">
          <el-form-item label="专辑名" prop="title">
            <el-input v-model="albumCreateForm.title" placeholder="请输入专辑名" />
          </el-form-item>
          <el-form-item label="专辑封面（可选）">
            <div class="cover-upload-wrap" @click="triggerAlbumCoverUpload">
              <div class="cover-upload-preview">
                <img v-if="albumCreateForm.coverUrl" :src="albumCreateForm.coverUrl" alt="封面" class="cover-upload-img" />
                <span v-else class="cover-upload-placeholder">点击上传图片</span>
              </div>
              <div v-if="albumCoverUploading" class="cover-upload-mask">上传中...</div>
            </div>
            <input
              ref="albumCoverFileInput"
              type="file"
              accept="image/*"
              class="cover-file-input"
              @change="onAlbumCoverFileChange"
            />
          </el-form-item>
          <el-form-item label="简介（可选）">
            <el-input v-model="albumCreateForm.description" type="textarea" :rows="3" placeholder="专辑简介" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="albumCreateVisible = false">取消</el-button>
          <el-button type="primary" :loading="albumCreateSubmitting" @click="submitAlbumCreate">创建</el-button>
        </template>
      </el-dialog>

      <!-- 专辑详情（歌曲列表 + 添加歌曲） -->
      <el-dialog
        v-model="albumDetailVisible"
        :title="currentAlbum ? currentAlbum.title + ' · 专辑歌曲' : '专辑'"
        width="520px"
        class="album-detail-dialog"
      >
        <div v-if="currentAlbum" class="album-detail">
          <div v-if="albumSongsLoading" class="loading">加载中...</div>
          <template v-else>
            <div class="album-detail-actions">
              <el-button size="small" type="primary" @click="openAddSongToAlbum">添加歌曲到专辑</el-button>
            </div>
            <div v-if="!albumSongs.length" class="empty">暂无歌曲，点击「添加歌曲到专辑」从你的上传中选择</div>
            <ul v-else class="album-songs-list">
              <li v-for="s in albumSongs" :key="s.id" class="album-songs-item">
                <span class="song-title">{{ s.title }}</span>
                <el-button type="danger" link size="small" @click="removeSongFromAlbum(s)">移出专辑</el-button>
              </li>
            </ul>
          </template>
        </div>
        <template #footer>
          <el-button @click="albumDetailVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <!-- 选择歌曲加入专辑 -->
      <el-dialog v-model="addSongToAlbumVisible" title="添加歌曲到专辑" width="460px">
        <div v-if="!myUploadsNotInAlbum().length" class="empty">没有可添加的歌曲（已上传的歌曲若已在专辑中则不显示）</div>
        <ul v-else class="album-songs-list">
          <li v-for="s in myUploadsNotInAlbum()" :key="s.id" class="album-songs-item">
            <span class="song-title">{{ s.title }}</span>
            <el-button type="primary" link size="small" :loading="addSongToAlbumSubmitting" @click="addSongToAlbum(s)">
              加入
            </el-button>
          </li>
        </ul>
      </el-dialog>

      <!-- 创建歌单弹窗 -->
      <el-dialog
        v-model="createDialogVisible"
        width="420px"
        title="创建歌单"
      >
        <el-form
          ref="createFormRef"
          :model="createForm"
          :rules="createRules"
          label-position="top"
        >
          <el-form-item label="歌单名称" prop="name">
            <el-input v-model="createForm.name" placeholder="请输入歌单名称" />
          </el-form-item>
          <el-form-item label="歌单封面（可选）">
            <div class="cover-upload-wrap" @click="triggerCreatePlaylistCoverUpload">
              <div class="cover-upload-preview">
                <img v-if="createForm.coverUrl" :src="createForm.coverUrl" alt="封面" class="cover-upload-img" />
                <span v-else class="cover-upload-placeholder">点击上传图片</span>
              </div>
              <div v-if="createPlaylistCoverUploading" class="cover-upload-mask">上传中...</div>
            </div>
            <input
              ref="createPlaylistCoverFileInput"
              type="file"
              accept="image/*"
              class="cover-file-input"
              @change="onCreatePlaylistCoverFileChange"
            />
          </el-form-item>
          <el-form-item label="是否公开">
            <el-switch v-model="createForm.isPublic" />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="createDialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="handleCreatePlaylist">
              确 定
            </el-button>
          </span>
        </template>
      </el-dialog>

      <!-- 上传歌曲弹窗 -->
      <el-dialog
        v-model="uploadDialogVisible"
        title="上传歌曲"
        width="460px"
        :close-on-click-modal="false"
      >
        <el-form
          ref="uploadFormRef"
          :model="uploadForm"
          :rules="uploadRules"
          label-position="top"
        >
          <el-form-item label="音频文件" prop="file" required>
            <div class="upload-file-wrap" @click="uploadFileInput?.click()">
              <span v-if="!uploadForm.file" class="upload-placeholder">点击选择 MP3 等音频文件</span>
              <span v-else class="upload-filename">{{ uploadForm.file.name }}</span>
            </div>
            <input
              ref="uploadFileInput"
              type="file"
              accept="audio/*"
              class="upload-file-input"
              @change="onUploadFileChange"
            />
          </el-form-item>
          <el-form-item label="歌曲名称（可选，不填则用文件名）">
            <el-input v-model="uploadForm.title" placeholder="便于展示的名称" />
          </el-form-item>
          <el-form-item label="专辑名（可选）">
            <el-input v-model="uploadForm.albumName" placeholder="所属专辑" />
          </el-form-item>
          <el-form-item label="歌曲封面（可选）">
            <div class="cover-upload-wrap cover-upload-wrap--small" @click="triggerSongCoverUpload">
              <div class="cover-upload-preview">
                <img v-if="uploadForm.coverUrl" :src="uploadForm.coverUrl" alt="封面" class="cover-upload-img" />
                <span v-else class="cover-upload-placeholder">点击上传</span>
              </div>
              <div v-if="songCoverUploading" class="cover-upload-mask">上传中...</div>
            </div>
            <input
              ref="songCoverFileInput"
              type="file"
              accept="image/*"
              class="cover-file-input"
              @change="onSongCoverFileChange"
            />
          </el-form-item>
          <el-form-item label="歌词文件（可选，支持 .lrc / .txt）">
            <div class="upload-file-wrap lyric-file-wrap" @click="lyricFileInput?.click()">
              <span v-if="!uploadForm.lyricFile" class="upload-placeholder">点击选择 LRC 或 TXT 歌词文件</span>
              <span v-else class="upload-filename">{{ uploadForm.lyricFile.name }}</span>
            </div>
            <input
              ref="lyricFileInput"
              type="file"
              accept=".lrc,.txt,text/plain"
              class="upload-file-input"
              @change="onLyricFileChange"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="uploadDialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="uploading" @click="handleUploadSong">
            上 传
          </el-button>
        </template>
      </el-dialog>

      <!-- 编辑歌曲弹窗 -->
      <el-dialog v-model="editSongDialogVisible" title="编辑歌曲" width="460px" :close-on-click-modal="false">
        <el-form :model="editSongForm" label-position="top">
          <el-form-item label="歌曲名称">
            <el-input v-model="editSongForm.title" placeholder="歌曲名称" />
          </el-form-item>
          <el-form-item label="歌曲封面">
            <div class="cover-upload-wrap cover-upload-wrap--small" @click="triggerEditSongCoverUpload">
              <div class="cover-upload-preview">
                <img v-if="editSongForm.coverUrl" :src="editSongForm.coverUrl" alt="封面" class="cover-upload-img" />
                <span v-else class="cover-upload-placeholder">点击上传</span>
              </div>
              <div v-if="editSongCoverUploading" class="cover-upload-mask">上传中...</div>
            </div>
            <input
              ref="editSongCoverFileInput"
              type="file"
              accept="image/*"
              class="cover-file-input"
              @change="onEditSongCoverFileChange"
            />
          </el-form-item>
          <el-form-item label="歌词">
            <div class="upload-file-wrap lyric-file-wrap" @click="editSongLyricFileInput?.click()">
              <span v-if="!editSongForm.lyricFile" class="upload-placeholder">
                {{ editSongForm.existingLyric ? '当前已有歌词，点击上传 LRC 覆盖' : '点击选择 LRC 或 TXT 歌词文件' }}
              </span>
              <span v-else class="upload-filename">{{ editSongForm.lyricFile.name }}</span>
            </div>
            <input
              ref="editSongLyricFileInput"
              type="file"
              accept=".lrc,.txt,text/plain"
              class="upload-file-input"
              @change="onEditSongLyricFileChange"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="editSongDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="editSongSubmitting" @click="submitEditSong">保存</el-button>
        </template>
      </el-dialog>

      <!-- 收藏歌单弹窗 -->
      <el-dialog
        v-model="collectDialogVisible"
        width="420px"
        title="收藏别人的歌单"
      >
        <el-form
          ref="collectFormRef"
          :model="collectForm"
          :rules="collectRules"
          label-position="top"
        >
          <el-form-item label="歌单 ID" prop="playlistId">
            <el-input
              v-model="collectForm.playlistId"
              placeholder="请输入要收藏的歌单 ID"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="collectDialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="handleCollectPlaylist">
              确 定
            </el-button>
          </span>
        </template>
      </el-dialog>

      <!-- 更新账户信息弹窗 -->
      <el-dialog
        v-model="updateUserDialogVisible"
        width="420px"
        title="更新账户信息"
      >
        <el-form
          ref="updateUserFormRef"
          :model="updateUserForm"
          :rules="updateUserRules"
          label-position="top"
        >
          <el-form-item label="头像" prop="avatarUrl">
            <div class="avatar-upload-wrap" @click="triggerAvatarUpload">
              <el-avatar
                :size="80"
                :src="updateUserForm.avatarUrl || userInfo?.avatarUrl"
                class="avatar-circle"
              >
                {{ (updateUserForm.username || userInfo?.username || '').charAt(0).toUpperCase() }}
              </el-avatar>
              <div v-if="avatarUploading" class="avatar-upload-mask">
                <span class="loading-text">上传中...</span>
              </div>
              <div v-else class="avatar-upload-hint">点击上传</div>
            </div>
            <input
              ref="avatarFileInput"
              type="file"
              accept="image/*"
              class="avatar-file-input"
              @change="onAvatarFileChange"
            />
          </el-form-item>
          <el-form-item label="昵称" prop="username">
            <el-input
              v-model="updateUserForm.username"
              placeholder="请输入昵称"
            />
          </el-form-item>
          <el-form-item label="简介（可选）" prop="profile">
            <el-input
              v-model="updateUserForm.profile"
              type="textarea"
              :rows="3"
              placeholder="写一句简介吧"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="updateUserDialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="handleUpdateUser">保 存</el-button>
          </span>
        </template>
      </el-dialog>

      <!-- 更改密码弹窗 -->
      <el-dialog
        v-model="passwordDialogVisible"
        width="420px"
        title="更改密码"
      >
        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </el-form-item>
          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="请输入新密码（至少 6 位）"
            />
          </el-form-item>
          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
            />
          </el-form-item>
          <el-form-item label="邮箱验证码" prop="code">
            <div class="code-line">
              <el-input
                v-model="passwordForm.code"
                placeholder="请输入验证码"
                maxlength="6"
              />
              <el-button
                class="code-btn"
                :loading="sendingCode"
                :disabled="!!codeCountdown"
                @click="handleSendPasswordCode"
              >
                <span v-if="!codeCountdown">获取验证码</span>
                <span v-else>{{ codeCountdown }}s 后重发</span>
              </el-button>
            </div>
          </el-form-item>
        </el-form>
        <template #footer>
          <span class="dialog-footer">
            <el-button @click="passwordDialogVisible = false">取 消</el-button>
            <el-button type="primary" @click="handleUpdatePassword">
              确认修改
            </el-button>
          </span>
        </template>
      </el-dialog>
    </main>
  </div>
</template>

<style scoped>
.profile-page {
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
  flex-wrap: wrap;
  gap: 12px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.code-line {
  display: flex;
  gap: 8px;
  width: 100%;
}

.code-line .el-input {
  flex: 1;
}

.code-btn {
  white-space: nowrap;
}

/* 更新账户信息弹窗内头像上传 */
.avatar-upload-wrap {
  position: relative;
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  cursor: pointer;
  width: 80px;
}

.avatar-upload-wrap .avatar-circle {
  display: block;
  border: 2px dashed rgba(148, 163, 184, 0.5);
  background: radial-gradient(circle at 30% 20%, #38bdf8, #4f46e5);
  flex-shrink: 0;
}

.avatar-upload-wrap:hover .avatar-circle {
  border-color: rgba(59, 130, 246, 0.8);
}

.avatar-upload-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.avatar-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.avatar-upload-mask {
  position: absolute;
  left: 0;
  top: 0;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.avatar-upload-mask .loading-text {
  font-size: 12px;
}

.header h1 {
  margin: 0 0 4px;
  font-size: 24px;
  font-weight: 600;
}

.header p {
  margin: 0;
  font-size: 13px;
  color: #9ca3af;
}

.content {
  max-width: 720px;
}

.card {
  border-radius: 16px;
}

.card-header span {
  font-weight: 500;
}

.card-header--with-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.card-actions {
  display: flex;
  gap: 8px;
}

.basic-info {
  display: flex;
  gap: 16px;
  align-items: center;
}

.avatar {
  background: radial-gradient(circle at 30% 20%, #38bdf8, #4f46e5);
}

.meta {
  flex: 1;
}

.name {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #0f172a;
}

.desc {
  font-size: 13px;
  color: #6b7280;
  margin-bottom: 8px;
}

.tags {
  display: flex;
  gap: 8px;
}

.section-desc {
  margin: 0 0 12px;
  font-size: 13px;
  color: #6b7280;
}

.upload-file-wrap {
  padding: 10px 14px;
  border: 1px dashed rgba(148, 163, 184, 0.5);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #6b7280;
}

.upload-placeholder {
  display: block;
}

.upload-filename {
  display: block;
  color: #111827;
}

.upload-file-input {
  display: none;
}

.uploads-table {
  width: 100%;
}

.loading,
.empty {
  font-size: 13px;
  color: #6b7280;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.stat-item {
  padding: 10px 12px;
  border-radius: 12px;
  background-color: #f9fafb;
}

.stat-item--link {
  cursor: pointer;
  transition: background-color 0.15s ease, transform 0.15s ease;
}

.stat-item--link:hover {
  background-color: #eff6ff;
  transform: translateY(-1px);
}

.stat-label {
  font-size: 12px;
  color: #6b7280;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 18px;
  font-weight: 600;
  color: #111827;
}

.download-settings {
  max-width: 560px;
}
.download-hint {
  font-size: 13px;
  color: #6b7280;
  margin: 0 0 12px;
  line-height: 1.5;
}
.download-path-row {
  display: flex;
  gap: 12px;
  align-items: center;
}
.download-path-input {
  flex: 1;
  min-width: 0;
}
.download-folder-name {
  font-size: 13px;
  color: #374151;
  margin-left: 8px;
}
.download-unsupported {
  font-size: 12px;
  color: #9ca3af;
  margin: 12px 0 0;
}

.playlist-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
}

.playlist-item {
  display: flex;
  gap: 8px;
  padding: 8px;
  border-radius: 12px;
  background-color: #f9fafb;
}

.playlist-item--clickable {
  cursor: pointer;
}

.playlist-item--clickable:hover {
  background-color: #eff6ff;
}

.playlist-cover {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  overflow: hidden;
  background: radial-gradient(circle at 30% 20%, #38bdf8, #4f46e5);
  flex-shrink: 0;
}

.playlist-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.playlist-cover .fallback {
  width: 100%;
  height: 100%;
}

.playlist-meta {
  flex: 1;
  min-width: 0;
}

.playlist-name {
  font-size: 14px;
  font-weight: 500;
  color: #111827;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.playlist-type {
  margin-top: 2px;
  font-size: 12px;
  color: #6b7280;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.album-manage-btn {
  padding: 0 4px;
}

.album-detail-actions {
  margin-bottom: 12px;
}

.album-songs-list {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 320px;
  overflow-y: auto;
}

.album-songs-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f3f4f6;
}

.album-songs-item .song-title {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cover-upload-wrap {
  position: relative;
  width: 120px;
  height: 120px;
  border: 1px dashed #d1d5db;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
}

.cover-upload-wrap:hover {
  border-color: #38bdf8;
}

.cover-upload-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f9fafb;
}

.cover-upload-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-upload-placeholder {
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
  padding: 8px;
}

.cover-upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.cover-upload-wrap--small {
  width: 96px;
  height: 96px;
}

.upload-cover-cell {
  width: 48px;
  height: 48px;
  margin: 0 auto;
  border-radius: 6px;
  overflow: hidden;
  background: #f3f4f6;
}

.upload-cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.upload-cover-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e5e7eb, #d1d5db);
}
</style>

