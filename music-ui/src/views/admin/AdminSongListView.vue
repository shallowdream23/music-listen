<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import adminRequest from '../../utils/adminRequest'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const statusFilter = ref('') // '' 全部, 0 待审核, 1 已上架
const keyword = ref('')

async function loadList() {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
    }
    if (statusFilter.value !== '') params.status = Number(statusFilter.value)
    const res = await adminRequest.get('/song/list-admin', { params })
    if (res.data?.code === 2000 && res.data?.data) {
      const page = res.data.data
      list.value = page.records || []
      total.value = page.total || 0
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
  if (s === 1) return '已上架'
  return '-'
}

const editDialogVisible = ref(false)
const editForm = ref({
  id: null,
  title: '',
  coverUrl: '',
  lyricFile: null,
  existingLyric: '',
  status: 0,
})
const lyricFileInput = ref(null)
const coverFileInput = ref(null)
const coverUploading = ref(false)
const editSubmitting = ref(false)

function openEdit(row) {
  editForm.value = {
    id: row.id,
    title: row.title || '',
    coverUrl: row.coverUrl || '',
    lyricFile: null,
    existingLyric: row.lyric != null ? row.lyric : '',
    status: row.status != null ? row.status : 0,
  }
  editDialogVisible.value = true
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

function onLyricFileChange(e) {
  const f = e.target?.files?.[0]
  editForm.value.lyricFile = f || null
  e.target.value = ''
}

function readLyricFileAsText(file) {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(reader.result ?? '')
    reader.onerror = () => reject(new Error('读取歌词文件失败'))
    reader.readAsText(file, 'UTF-8')
  })
}

async function submitEdit() {
  editSubmitting.value = true
  try {
    let lyricText = editForm.value.existingLyric || ''
    if (editForm.value.lyricFile) {
      lyricText = await readLyricFileAsText(editForm.value.lyricFile)
      lyricText = (lyricText || '').trim()
    }
    await adminRequest.put(`/song/${editForm.value.id}`, {
      id: editForm.value.id,
      title: (editForm.value.title || '').trim() || undefined,
      coverUrl: editForm.value.coverUrl?.trim() || undefined,
      lyric: lyricText,
      status: editForm.value.status,
    })
    ElMessage.success('保存成功')
    editDialogVisible.value = false
    loadList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '保存失败')
  } finally {
    editSubmitting.value = false
  }
}

async function approve(row) {
  try {
    await adminRequest.post(`/song/${row.id}/approve`)
    ElMessage.success('已上架')
    loadList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function offline(row) {
  try {
    await ElMessageBox.confirm('确定下架该歌曲吗？下架后用户将无法在首页看到。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await adminRequest.post(`/song/${row.id}/offline`)
    ElMessage.success('已下架')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该歌曲吗？此操作不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await adminRequest.delete(`/song/${row.id}`)
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

onMounted(loadList)
</script>

<template>
  <div class="song-list">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索歌名"
        clearable
        style="width: 200px; margin-right: 12px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px; margin-right: 12px">
        <el-option label="全部" value="" />
        <el-option label="待审核" :value="0" />
        <el-option label="已上架" :value="1" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>
    <el-table v-loading="loading" :data="list" stripe style="width: 100%; margin-top: 16px" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="歌名" min-width="160" />
      <el-table-column prop="coverUrl" label="封面" width="80">
        <template #default="{ row }">
          <img v-if="row.coverUrl" :src="row.coverUrl" alt="" class="cover-img" />
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="singerId" label="歌手ID" width="90" />
      <el-table-column prop="playCount" label="播放量" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span :class="row.status === 0 ? 'status-pending' : 'status-ok'">{{ statusText(row.status) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="260" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="row.status === 0" type="success" link size="small" @click="approve(row)">上架</el-button>
          <el-button v-else type="warning" link size="small" @click="offline(row)">下架</el-button>
          <el-button type="danger" link size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="editDialogVisible" title="编辑歌曲" width="560px" :close-on-click-modal="false">
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="歌名">
          <el-input v-model="editForm.title" placeholder="歌曲名称" />
        </el-form-item>
        <el-form-item label="歌曲封面">
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
        <el-form-item label="歌词文件">
          <div class="upload-file-wrap lyric-file-wrap" @click="lyricFileInput?.click()">
            <span v-if="!editForm.lyricFile" class="upload-placeholder">
              {{ editForm.existingLyric ? '当前已有歌词，点击上传 LRC 覆盖' : '点击选择 LRC 歌词文件' }}
            </span>
            <span v-else class="upload-filename">{{ editForm.lyricFile.name }}</span>
          </div>
          <input
            ref="lyricFileInput"
            type="file"
            accept=".lrc,.txt,text/plain"
            class="upload-file-input"
            @change="onLyricFileChange"
          />
        </el-form-item>
        <el-form-item label="上架状态">
          <el-select v-model="editForm.status" placeholder="状态" style="width: 100%">
            <el-option label="待审核" :value="0" />
            <el-option label="已上架" :value="1" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="editSubmitting" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped>
.song-list {
  max-width: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.cover-img {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: cover;
}

.text-muted {
  color: #94a3b8;
}

.status-ok {
  color: #4ade80;
}

.status-pending {
  color: #fbbf24;
}

.upload-file-wrap {
  padding: 10px 14px;
  border: 1px dashed rgba(148, 163, 184, 0.5);
  border-radius: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #94a3b8;
}

.upload-file-wrap:hover {
  border-color: #64748b;
  color: #e2e8f0;
}

.upload-placeholder {
  display: block;
}

.upload-filename {
  display: block;
  color: #38bdf8;
}

.upload-file-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.cover-upload-wrap {
  position: relative;
  width: 96px;
  height: 96px;
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

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

:deep(.admin-table) {
  --el-table-bg-color: #1e293b;
  --el-table-tr-bg-color: #1e293b;
  --el-table-header-bg-color: #334155;
  --el-table-border-color: #334155;
  --el-table-row-hover-bg-color: #334155;
}
</style>
