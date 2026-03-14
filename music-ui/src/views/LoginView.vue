<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { useAuthStore } from '../store/auth'

const router = useRouter()
const authStore = useAuthStore()

const loading = ref(false)

const formRef = ref()
const form = reactive({
  username: '',
  password: '',
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' },
  ],
}

const handleSubmit = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await request.post('/login', {
        username: form.username,
        password: form.password,
      })

      // 先判断业务是否成功（如密码错误时后端返回 code !== 2000）
      if (res.data?.code !== 2000) {
        ElMessage.error(res.data?.message || '登录失败')
        return
      }

      // token 可能在响应头或响应体中
      const headerToken = res.headers['token'] || res.headers['Token']
      const bodyToken = res.data?.data
      const token = headerToken || bodyToken

      if (!token) {
        ElMessage.error('登录成功，但未获取到 token')
        return
      }

      authStore.setToken(token)
      await authStore.fetchUserInfo()
      ElMessage.success('登录成功')
      router.push('/')
    } catch (e) {
      const msg = e.response?.data?.message || '登录失败'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  })
}

const goRegister = () => {
  router.push('/register')
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="title">在线音乐 · 登录</h1>
      <p class="subtitle">欢迎回来，登录后即可畅听音乐</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="auth-form"
      >
        <el-form-item label="用户名" prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入用户名"
            autocomplete="username"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="current-password"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleSubmit"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="switch-line">
        还没有账号？
        <el-button link type="primary" @click="goRegister">
          去注册
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(ellipse 80% 60% at 50% 0%, rgba(79, 70, 229, 0.25) 0%, transparent 50%),
    radial-gradient(circle at 80% 80%, rgba(14, 165, 233, 0.15) 0%, transparent 40%),
    #0f172a;
}

.auth-card {
  width: 400px;
  padding: 40px 36px 32px;
  border-radius: 20px;
  background: linear-gradient(180deg, rgba(30, 41, 59, 0.95) 0%, rgba(15, 23, 42, 0.98) 100%);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(148, 163, 184, 0.2);
  box-shadow: 0 24px 48px rgba(0, 0, 0, 0.4);
  box-sizing: border-box;
}

.title {
  margin: 0 0 6px;
  font-size: 26px;
  font-weight: 700;
  color: #f1f5f9;
  letter-spacing: -0.02em;
}

.subtitle {
  margin: 0 0 28px;
  font-size: 14px;
  color: #94a3b8;
}

.auth-form {
  margin-top: 4px;
}

.auth-form :deep(.el-form-item__label) {
  color: #94a3b8;
}

.auth-form :deep(.el-input__wrapper) {
  background: rgba(15, 23, 42, 0.8);
  border: 1px solid rgba(148, 163, 184, 0.25);
  box-shadow: none;
}

.auth-form :deep(.el-input__wrapper:hover),
.auth-form :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(79, 70, 229, 0.5);
}

.auth-form :deep(.el-input__inner) {
  color: #e5e7eb;
}

.submit-btn {
  width: 100%;
  height: 44px;
  font-weight: 600;
  background: linear-gradient(135deg, #4f46e5 0%, #0ea5e9 100%);
  border: none;
}

.submit-btn:hover {
  background: linear-gradient(135deg, #6366f1 0%, #38bdf8 100%);
}

.switch-line {
  margin-top: 20px;
  text-align: center;
  font-size: 14px;
  color: #94a3b8;
}

.switch-line .el-button {
  font-weight: 500;
}
</style>

