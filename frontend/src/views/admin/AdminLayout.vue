<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <div class="admin-sidebar">
      <div style="padding: 20px 16px; text-align: center; border-bottom: 1px solid var(--border-color);">
        <div style="font-size: 22px; margin-bottom: 4px;">🏔️</div>
        <div style="font-size: 14px; font-weight: 600; color: var(--accent);">预警系统后台</div>
      </div>

      <el-menu
        :default-active="activeMenu"
        background-color="transparent"
        text-color="var(--text-secondary)"
        active-text-color="var(--accent)"
        router
        style="border-right: none; margin-top: 8px;"
      >
        <el-menu-item index="/admin/scenic">
          <template #title>
            <span>🏞️ 景区管理</span>
          </template>
        </el-menu-item>
        <el-menu-item index="/admin/threshold">
          <template #title>
            <span>⚙️ 阈值配置</span>
          </template>
        </el-menu-item>
        <el-menu-item index="/admin/warning">
          <template #title>
            <span>🔔 预警日志</span>
          </template>
        </el-menu-item>
        <el-divider />
        <el-menu-item index="/dashboard">
          <template #title>
            <span>📺 监控大屏</span>
          </template>
        </el-menu-item>
      </el-menu>

      <div style="position: absolute; bottom: 20px; left: 16px; right: 16px; text-align: center;">
        <div style="font-size: 12px; color: var(--text-muted); margin-bottom: 8px;">
          {{ username || '管理员' }}
        </div>
        <el-button size="small" type="danger" plain @click="handleLogout" style="width: 100%;">退出登录</el-button>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="admin-content">
      <div style="margin-bottom: 20px; display: flex; align-items: center; justify-content: space-between;">
        <h2 style="font-size: 20px; font-weight: 600; color: var(--text-primary);">{{ pageTitle }}</h2>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/admin' }">后台管理</el-breadcrumb-item>
          <el-breadcrumb-item>{{ pageTitle }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const username = localStorage.getItem('username') || '管理员'

const activeMenu = computed(() => route.path)
const pageTitle = computed(() => {
  const map = {
    '/admin/scenic': '景区管理',
    '/admin/threshold': '阈值配置',
    '/admin/warning': '预警日志'
  }
  return map[route.path] || '管理'
})

const handleLogout = () => {
  localStorage.removeItem('token')
  localStorage.removeItem('userId')
  localStorage.removeItem('username')
  ElMessage.success('已退出登录')
  router.push('/login')
}
</script>

<style scoped>
.admin-sidebar {
  position: relative;
}
:deep(.el-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 2px 8px;
  border-radius: 8px;
}
:deep(.el-menu-item:hover) {
  background: rgba(45, 90, 240, 0.1) !important;
}
:deep(.el-menu-item.is-active) {
  background: rgba(0, 212, 255, 0.1) !important;
}
:deep(.el-divider) {
  border-color: var(--border-color);
  margin: 8px 16px;
}
:deep(.el-breadcrumb__inner) {
  color: var(--text-muted) !important;
}
</style>
