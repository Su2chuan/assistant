<template>
  <div v-if="isLogin" class="login-page">
    <div class="login-card">
      <div class="login-header">
        <div class="login-icon">🔒</div>
        <h2>AI 知识库</h2>
        <p>请输入密码访问</p>
      </div>
      <el-form @submit.prevent="handleLogin">
        <el-form-item>
          <el-input
            v-model="password"
            type="password"
            placeholder="请输入密码"
            size="large"
            show-password
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%;" :loading="loading" @click="handleLogin">
          进入
        </el-button>
      </el-form>
    </div>
  </div>

  <el-container v-else class="app-container">
    <el-aside width="200px" class="app-aside">
      <div class="aside-logo">
        <span class="logo-icon">📚</span>
        <span class="logo-text">AI 知识库</span>
      </div>
      <el-menu
        :default-active="$route.path"
        router
        class="aside-menu"
        background-color="transparent"
        text-color="#a0a0b0"
        active-text-color="#fff"
      >
        <el-menu-item index="/">
          <el-icon><HomeFilled /></el-icon>
          <span>首页</span>
        </el-menu-item>
        <el-menu-item index="/documents">
          <el-icon><Document /></el-icon>
          <span>文档管理</span>
        </el-menu-item>
        <el-menu-item index="/chat">
          <el-icon><ChatDotRound /></el-icon>
          <span>AI 问答</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-footer">
        <el-button text @click="logout">
          <el-icon><SwitchButton /></el-icon>
          退出
        </el-button>
      </div>
    </el-aside>
    <el-main class="app-main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { HomeFilled, Document, ChatDotRound, SwitchButton } from '@element-plus/icons-vue'
import api from './api'

const router = useRouter()
const route = useRoute()
const password = ref('')
const loading = ref(false)

const isLogin = computed(() => route.path === '/login')

onMounted(() => {
  const token = localStorage.getItem('token')
  if (!token && route.path !== '/login') {
    router.push('/login')
  }
})

async function handleLogin() {
  if (!password.value) return
  loading.value = true
  try {
    const res = await api.login(password.value)
    localStorage.setItem('token', res.data.token)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (e) {
    ElMessage.error('密码错误')
  }
  loading.value = false
}

function logout() {
  localStorage.removeItem('token')
  router.push('/login')
}
</script>

<style>
.login-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.login-card {
  background: #fff;
  border-radius: 16px;
  padding: 40px 32px;
  width: 360px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.25);
}
.login-header {
  text-align: center;
  margin-bottom: 28px;
}
.login-icon {
  font-size: 40px;
  margin-bottom: 12px;
}
.login-header h2 {
  margin-bottom: 6px;
  color: #1a1a2e;
  font-size: 20px;
}
.login-header p {
  color: #999;
  font-size: 13px;
}

.app-container {
  min-height: 100vh;
  background: #f0f2f5;
}
.app-aside {
  background: #1a1a2e;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 10;
  width: 200px !important;
}
.aside-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.logo-icon {
  font-size: 22px;
}
.logo-text {
  color: #e8e8e8;
  font-size: 16px;
  font-weight: 600;
}
.aside-menu {
  border: none;
  padding: 8px;
  flex: 1;
}
.aside-menu .el-menu-item {
  border-radius: 8px;
  margin-bottom: 4px;
  height: 42px;
  font-size: 14px;
}
.aside-menu .el-menu-item:hover {
  background: rgba(255,255,255,0.06) !important;
}
.aside-menu .el-menu-item.is-active {
  background: rgba(64,158,255,0.15) !important;
}
.aside-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255,255,255,0.06);
}
.aside-footer .el-button {
  color: #888;
  font-size: 13px;
  width: 100%;
  justify-content: flex-start;
}
.app-main {
  margin-left: 200px;
  padding: 24px;
  min-height: 100vh;
}
</style>