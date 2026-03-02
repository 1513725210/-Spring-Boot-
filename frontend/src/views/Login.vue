<template>
  <div class="login-container">
    <!-- 背景粒子装饰 -->
    <div class="login-particles">
      <div v-for="i in 20" :key="i" class="particle" :style="particleStyle(i)"></div>
    </div>

    <div class="login-card">
      <div class="login-logo">🏔️</div>
      <h1 class="login-title">景区预警系统</h1>
      <p class="login-subtitle">Scenic Spot Over-tourism Early Warning System</p>

      <el-form ref="formRef" :model="loginForm" :rules="rules" @submit.prevent="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            prefix-icon="User"
            placeholder="管理员账号"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            prefix-icon="Lock"
            placeholder="登录密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%; height: 44px; font-size: 15px;"
            @click="handleLogin"
          >
            {{ loading ? '正在登录...' : '登 录' }}
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <el-button type="primary" link size="small" @click="$router.push('/dashboard')">
          → 前往监控大屏（免登录）
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { authApi } from '@/api'
import { ElMessage } from 'element-plus'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const res = await authApi.login(loginForm)
      if (res.code === 200) {
        localStorage.setItem('token', res.data.token || 'mock-token')
        localStorage.setItem('userId', res.data.userId || res.data.id)
        localStorage.setItem('username', loginForm.username)
        ElMessage.success('登录成功')
        router.push('/admin')
      } else {
        ElMessage.error(res.message || '登录失败')
      }
    } catch (error) {
      ElMessage.error('网络错误，请稍后再试')
    } finally {
      loading.value = false
    }
  })
}

const particleStyle = (i) => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  width: `${2 + Math.random() * 4}px`,
  height: `${2 + Math.random() * 4}px`,
  animationDelay: `${Math.random() * 5}s`,
  animationDuration: `${3 + Math.random() * 7}s`
})
</script>

<style scoped>
.login-particles {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  overflow: hidden;
  pointer-events: none;
}
.particle {
  position: absolute;
  background: var(--primary-light);
  border-radius: 50%;
  opacity: 0.3;
  animation: floatUp linear infinite;
}
@keyframes floatUp {
  0% { transform: translateY(0) scale(1); opacity: 0; }
  10% { opacity: 0.4; }
  90% { opacity: 0.4; }
  100% { transform: translateY(-100vh) scale(0.5); opacity: 0; }
}
.login-logo {
  text-align: center;
  font-size: 48px;
  margin-bottom: 12px;
}
.login-footer {
  text-align: center;
  margin-top: 16px;
}
</style>
