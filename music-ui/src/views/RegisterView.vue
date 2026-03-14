<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()

const formRef = ref()
const sending = ref(false)
const loading = ref(false)
const countdown = ref(0)
let timer = null

const form = reactive({
  username: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
})

const rules = {
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
        } else if (value !== form.password) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: ['blur', 'change'],
    },
  ],
}

const startCountdown = () => {
  countdown.value = 120
  timer = window.setInterval(() => {
    if (countdown.value <= 1) {
      window.clearInterval(timer)
      countdown.value = 0
    } else {
      countdown.value -= 1
    }
  }, 1000)
}

const handleSendCode = async () => {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  if (countdown.value > 0 || sending.value) return

  sending.value = true
  try {
    await request.get('/sendEmailCode', {
      params: { email: form.email },
    })
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown()
  } catch (e) {
    const msg = e.response?.data?.message || '发送验证码失败'
    ElMessage.error(msg)
  } finally {
    sending.value = false
  }
}

const handleSubmit = () => {
  if (!formRef.value) return
  formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      await request.post('/register', {
        username: form.username,
        password: form.password,
        email: form.email,
        code: form.code,
      })
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    } catch (e) {
      const msg = e.response?.data?.message || '注册失败'
      ElMessage.error(msg)
    } finally {
      loading.value = false
    }
  })
}

const goLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <h1 class="title">在线音乐 · 注册</h1>
      <p class="subtitle">创建一个账号，开始你的音乐之旅</p>

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

        <el-form-item label="邮箱" prop="email">
          <el-input
            v-model="form.email"
            placeholder="请输入邮箱"
            autocomplete="email"
          />
        </el-form-item>

        <el-form-item label="邮箱验证码" prop="code">
          <div class="code-line">
            <el-input
              v-model="form.code"
              placeholder="请输入验证码"
              maxlength="6"
            />
            <el-button
              class="code-btn"
              :loading="sending"
              :disabled="!!countdown"
              @click="handleSendCode"
            >
              <span v-if="!countdown">获取验证码</span>
              <span v-else>{{ countdown }}s 后重发</span>
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入密码"
            autocomplete="new-password"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            class="submit-btn"
            :loading="loading"
            @click="handleSubmit"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="switch-line">
        已有账号？
        <el-button link type="primary" @click="goLogin">
          去登录
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
  padding: 24px 0;
  background: radial-gradient(ellipse 80% 60% at 50% 0%, rgba(79, 70, 229, 0.25) 0%, transparent 50%),
    radial-gradient(circle at 20% 80%, rgba(14, 165, 233, 0.15) 0%, transparent 40%),
    #0f172a;
}

.auth-card {
  width: 420px;
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

.code-line {
  display: flex;
  gap: 10px;
}

.code-line .el-input {
  flex: 1;
  min-width: 0;
}

.code-btn {
  white-space: nowrap;
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

