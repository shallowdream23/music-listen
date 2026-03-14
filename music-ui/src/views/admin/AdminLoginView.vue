<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAdminAuthStore } from '../../store/adminAuth'
import adminRequest from '../../utils/adminRequest'

const router = useRouter()
const adminAuth = useAdminAuthStore()

const loading = ref(false)
const formRef = ref()
const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, message: '密码至少 6 位', trigger: 'blur' }],
}

const handleSubmit = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await adminRequest.post('/login', {
        username: form.username,
        password: form.password,
      })
      const headerToken = res.headers['token'] || res.headers['Token']
      const bodyToken = res.data?.data
      const token = headerToken || bodyToken
      if (!token) {
        ElMessage.error('登录成功，但未获取到 token')
        return
      }
      adminAuth.setToken(token)
      const userInfo = await adminAuth.fetchUserInfo()
      if (!userInfo?.isAdmin) {
        adminAuth.logout()
        ElMessage.error('该账号不是管理员，请使用管理员账号登录')
        return
      }
      ElMessage.success('管理员登录成功')
      router.replace('/admin/dashboard')
    } catch (e) {
      const msg = e.response?.data?.message || '登录失败'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  })
}
</script>

<template>
  <div class="admin-login-page">
    <div class="admin-login-card">
      <h1 class="title">管理后台</h1>
      <p class="subtitle">请使用管理员账号登录</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="admin-login-form">
        <el-form-item label="管理员账号" prop="username">
          <el-input v-model="form.username" placeholder="请输入管理员账号" autocomplete="username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="current-password"
            @keyup.enter="handleSubmit"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" style="width: 100%" :loading="loading" @click="handleSubmit">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      <p class="back-link">
        <a href="/">← 返回首页</a>
      </p>
    </div>
  </div>
</template>

<style scoped>
.admin-login-page {
  min-height: 100vh;
  background: #0f172a;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.admin-login-card {
  width: 100%;
  max-width: 380px;
  padding: 32px;
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
}

.title {
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 600;
  color: #f1f5f9;
  text-align: center;
}

.subtitle {
  margin: 0 0 24px;
  font-size: 14px;
  color: #94a3b8;
  text-align: center;
}

.admin-login-form :deep(.el-form-item__label) {
  color: #e2e8f0;
}

.back-link {
  margin: 20px 0 0;
  text-align: center;
}

.back-link a {
  color: #94a3b8;
  font-size: 14px;
  text-decoration: none;
}

.back-link a:hover {
  color: #38bdf8;
}
</style>
