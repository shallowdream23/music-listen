<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import adminRequest from '../../utils/adminRequest'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref('') // '' 全部, 0 待审核, 1 已通过
const keyword = ref('')

async function loadList() {
  loading.value = true
  try {
    const params = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (statusFilter.value !== '') params.status = Number(statusFilter.value)
    if (keyword.value) params.keyword = keyword.value
    const res = await adminRequest.get('/album/list-admin', { params })
    if (res.data?.code === 2000 && res.data?.data) {
      const data = res.data.data
      list.value = data.records || []
      total.value = data.total || 0
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNum.value = 1
  loadList()
}

function handlePageChange(p) {
  pageNum.value = p
  loadList()
}

function handleSizeChange(s) {
  pageSize.value = s
  pageNum.value = 1
  loadList()
}

function statusText(s) {
  if (s === 0) return '待审核'
  if (s === 1) return '已通过'
  return '-'
}

// 详情/编辑弹窗
const detailVisible = ref(false)
const detailLoading = ref(false)
const detailData = ref(null) // { album, singerName, songs }
const editForm = ref({ id: null, title: '', coverUrl: '', description: '', status: 0 })
const editSubmitting = ref(false)
const coverFileInput = ref(null)
const coverUploading = ref(false)

async function openDetail(row) {
  detailVisible.value = true
  detailData.value = null
  detailLoading.value = true
  try {
    const res = await adminRequest.get(`/album/admin/${row.id}`)
    if (res.data?.code === 2000 && res.data?.data) {
      detailData.value = res.data.data
      const album = detailData.value.album || {}
      editForm.value = {
        id: album.id,
        title: album.title || '',
        coverUrl: album.coverUrl || '',
        description: album.description || '',
        status: album.status != null ? album.status : 0,
      }
    }
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '加载详情失败')
  } finally {
    detailLoading.value = false
  }
}

function triggerCoverUpload() {
  coverFileInput.value?.click()
}

async function onCoverFileChange(e) {
  const file = e.target?.files?.[0]
  if (!file) return
  coverUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    const res = await adminRequest.post('/user/uploadImage', formData)
    const url = res.data?.data
    if (url) editForm.value.coverUrl = url
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '封面上传失败')
  } finally {
    coverUploading.value = false
    e.target.value = ''
  }
}

async function submitEdit() {
  editSubmitting.value = true
  try {
    await adminRequest.post('/album/admin/update', {
      id: editForm.value.id,
      title: editForm.value.title?.trim() || undefined,
      coverUrl: editForm.value.coverUrl || undefined,
      description: editForm.value.description || undefined,
      status: editForm.value.status,
    })
    ElMessage.success('保存成功')
    await openDetail({ id: editForm.value.id })
    loadList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    editSubmitting.value = false
  }
}

async function approve(row) {
  try {
    await adminRequest.post(`/album/admin/${row.id}/approve`)
    ElMessage.success('已通过')
    loadList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function reject(row) {
  try {
    await ElMessageBox.confirm('确定驳回该专辑？歌手可重新提交。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await adminRequest.post(`/album/admin/${row.id}/reject`)
    ElMessage.success('已驳回')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function removeSongFromAlbum(song) {
  const albumId = detailData.value?.album?.id
  if (!albumId) return
  try {
    await adminRequest.post(`/album/admin/${albumId}/songs/remove`, { songId: song.id })
    ElMessage.success('已从专辑移除')
    detailData.value.songs = (detailData.value.songs || []).filter((s) => s.id !== song.id)
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

onMounted(loadList)
</script>

<template>
  <div class="album-list">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索专辑名"
        clearable
        style="width: 200px; margin-right: 12px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px; margin-right: 12px">
        <el-option label="全部" value="" />
        <el-option label="待审核" :value="0" />
        <el-option label="已通过" :value="1" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>
    <el-table v-loading="loading" :data="list" stripe style="width: 100%; margin-top: 16px" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="专辑名" min-width="160" show-overflow-tooltip />
      <el-table-column prop="singerName" label="所有人" width="120" show-overflow-tooltip />
      <el-table-column prop="songCount" label="歌曲数" width="90" align="center" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">
            {{ statusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openDetail(row)">详情/编辑</el-button>
          <template v-if="row.status === 0">
            <el-button type="success" link size="small" @click="approve(row)">通过</el-button>
            <el-button type="warning" link size="small" @click="reject(row)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 16px"
      @current-change="handlePageChange"
      @size-change="handleSizeChange"
    />

    <!-- 专辑详情/编辑 -->
    <el-dialog
      v-model="detailVisible"
      title="专辑详情"
      width="560px"
      destroy-on-close
      class="admin-dialog"
    >
      <div v-if="detailLoading" class="loading">加载中...</div>
      <template v-else-if="detailData">
        <el-form :model="editForm" label-width="80px" label-position="top">
          <el-form-item label="专辑名">
            <el-input v-model="editForm.title" placeholder="专辑名" />
          </el-form-item>
          <el-form-item label="专辑封面">
            <div class="cover-upload-wrap" @click="triggerCoverUpload">
              <div class="cover-upload-preview">
                <img v-if="editForm.coverUrl" :src="editForm.coverUrl" alt="封面" class="cover-upload-img" />
                <span v-else class="cover-upload-placeholder">点击上传图片</span>
              </div>
              <div v-if="coverUploading" class="cover-upload-mask">上传中...</div>
            </div>
            <input
              ref="coverFileInput"
              type="file"
              accept="image/*"
              class="cover-file-input"
              @change="onCoverFileChange"
            />
          </el-form-item>
          <el-form-item label="简介">
            <el-input v-model="editForm.description" type="textarea" :rows="2" placeholder="专辑简介" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="editForm.status" placeholder="状态" style="width: 120px">
              <el-option label="待审核" :value="0" />
              <el-option label="已通过" :value="1" />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="detail-meta">
          <span>所有人：{{ detailData.singerName || '-' }}</span>
        </div>
        <div class="detail-songs">
          <div class="detail-songs-title">专辑内歌曲</div>
          <div v-if="!detailData.songs?.length" class="empty">暂无歌曲</div>
          <ul v-else class="songs-ul">
            <li v-for="s in detailData.songs" :key="s.id" class="songs-li">
              <span class="song-name">{{ s.title }}</span>
              <el-button type="danger" link size="small" @click="removeSongFromAlbum(s)">移出专辑</el-button>
            </li>
          </ul>
        </div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailData" type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.album-list {
  padding: 0;
}
.toolbar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.loading,
.empty {
  color: #94a3b8;
  padding: 12px 0;
}
.detail-meta {
  margin-bottom: 12px;
  font-size: 13px;
  color: #94a3b8;
}
.detail-songs-title {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
}
.songs-ul {
  list-style: none;
  padding: 0;
  margin: 0;
  max-height: 240px;
  overflow-y: auto;
  border: 1px solid #334155;
  border-radius: 8px;
}
.songs-li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #334155;
}
.songs-li:last-child {
  border-bottom: none;
}
.song-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cover-upload-wrap {
  position: relative;
  width: 120px;
  height: 120px;
  border: 1px dashed #475569;
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
  background: #1e293b;
}

.cover-upload-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-upload-placeholder {
  font-size: 12px;
  color: #94a3b8;
  text-align: center;
  padding: 8px;
}

.cover-upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  color: #e2e8f0;
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
</style>
