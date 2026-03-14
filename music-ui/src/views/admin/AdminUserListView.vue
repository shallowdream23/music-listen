<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import adminRequest from '../../utils/adminRequest'

const loading = ref(false)
const list = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const keyword = ref('')

const editDialogVisible = ref(false)
const editFormRef = ref()
const editForm = ref({
  id: null,
  username: '',
  avatarUrl: '',
  profile: '',
  status: 0,
})
const editRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }, { min: 2, max: 50, message: '长度 2～50', trigger: 'blur' }],
}
const editSubmitting = ref(false)
const avatarFileInput = ref(null)
const avatarUploading = ref(false)

function openEdit(row) {
  editForm.value = {
    id: row.id,
    username: row.username || '',
    avatarUrl: row.avatarUrl || '',
    profile: row.profile || '',
    status: row.status != null ? row.status : 0,
  }
  editDialogVisible.value = true
}

async function onAvatarFileChange(e) {
  const file = e.target?.files?.[0]
  e.target.value = ''
  if (!file || !editForm.value.id) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  avatarUploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', file)
    formData.append('userId', String(editForm.value.id))
    const res = await adminRequest.post('/user/admin/uploadAvatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    const url = res?.data?.data
    if (url) {
      editForm.value.avatarUrl = url
      ElMessage.success('头像已上传')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '上传失败')
  } finally {
    avatarUploading.value = false
  }
}

async function submitEdit() {
  if (!editFormRef.value) return
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      await adminRequest.post('/user/admin/update', editForm.value)
      ElMessage.success('保存成功')
      editDialogVisible.value = false
      loadList()
    } catch (e) {
      ElMessage.error(e.response?.data?.message || '保存失败')
    } finally {
      editSubmitting.value = false
    }
  })
}

async function loadList() {
  loading.value = true
  try {
    const res = await adminRequest.get('/user/list-admin', {
      params: { pageNum: pageNum.value, pageSize: pageSize.value, keyword: keyword.value || undefined },
    })
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

async function freeze(row) {
  try {
    await ElMessageBox.confirm('确定冻结该用户吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await adminRequest.post('/user/freeze', null, { params: { userId: row.id } })
    ElMessage.success('已冻结')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function unFreeze(row) {
  try {
    await adminRequest.post('/user/unFreeze', null, { params: { userId: row.id } })
    ElMessage.success('已解冻')
    loadList()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

async function remove(row) {
  try {
    await ElMessageBox.confirm('确定删除该用户吗？此操作不可恢复。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    await adminRequest.post('/user/delete', null, { params: { id: row.id } })
    ElMessage.success('已删除')
    loadList()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e.response?.data?.message || '操作失败')
  }
}

onMounted(loadList)
</script>

<template>
  <div class="user-list">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索用户名"
        clearable
        style="width: 220px; margin-right: 12px"
        @keyup.enter="handleSearch"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>
    <el-table v-loading="loading" :data="list" stripe style="width: 100%; margin-top: 16px" class="admin-table">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="avatarUrl" label="头像" width="80">
        <template #default="{ row }">
          <img v-if="row.avatarUrl" :src="row.avatarUrl" alt="" class="avatar-img" />
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <span v-if="row.status === 1" class="status-frozen">已冻结</span>
          <span v-else class="status-ok">正常</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="openEdit(row)">编辑</el-button>
          <template v-if="row.status === 1">
            <el-button type="primary" link size="small" @click="unFreeze(row)">解冻</el-button>
          </template>
          <template v-else>
            <el-button type="warning" link size="small" @click="freeze(row)">冻结</el-button>
          </template>
          <el-button type="danger" link size="small" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="editDialogVisible" title="编辑用户" width="480px" :close-on-click-modal="false">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editForm.username" placeholder="用户名" />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-upload-wrap" @click="avatarFileInput?.click()">
            <div class="avatar-preview">
              <img v-if="editForm.avatarUrl" :src="editForm.avatarUrl" alt="" class="avatar-circle" />
              <span v-else class="avatar-placeholder">暂无头像</span>
            </div>
            <div v-if="avatarUploading" class="avatar-upload-mask">
              <span>上传中...</span>
            </div>
            <div v-else class="avatar-upload-hint">点击上传图片</div>
          </div>
          <input
            ref="avatarFileInput"
            type="file"
            accept="image/*"
            class="avatar-file-input"
            @change="onAvatarFileChange"
          />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="editForm.profile" type="textarea" :rows="3" placeholder="个人简介" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="editForm.status" placeholder="状态" style="width: 100%">
            <el-option label="正常" :value="0" />
            <el-option label="冻结" :value="1" />
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
.user-list {
  max-width: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
}

.avatar-img {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  object-fit: cover;
}

.text-muted {
  color: #94a3b8;
}

.status-ok {
  color: #4ade80;
}

.status-frozen {
  color: #f87171;
}

.avatar-upload-wrap {
  position: relative;
  width: 96px;
  height: 96px;
  border: 1px dashed rgba(148, 163, 184, 0.5);
  border-radius: 50%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
}

.avatar-upload-wrap:hover {
  border-color: #64748b;
}

.avatar-preview {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-circle {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-placeholder {
  font-size: 12px;
  color: #64748b;
}

.avatar-upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #e2e8f0;
}

.avatar-upload-hint {
  position: absolute;
  bottom: 4px;
  left: 0;
  right: 0;
  font-size: 11px;
  color: #64748b;
  text-align: center;
}

.avatar-file-input {
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
