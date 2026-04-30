<template>
  <div class="home-page">
    <h2 class="page-title">欢迎回来</h2>
    <p class="page-subtitle">管理你的学习资料，通过 AI 智能问答检索知识</p>

    <el-row :gutter="20" class="stat-row">
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-blue">
          <div class="stat-icon">📄</div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.documents }}</div>
            <div class="stat-label">文档总数</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-green">
          <div class="stat-icon">📁</div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.categories }}</div>
            <div class="stat-label">分类数量</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover" class="stat-card stat-purple">
          <div class="stat-icon">💬</div>
          <div class="stat-info">
            <div class="stat-number">{{ stats.chats }}</div>
            <div class="stat-label">对话次数</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="action-row">
      <el-col :span="12">
        <el-card shadow="hover" class="action-card" @click="$router.push('/documents')">
          <div class="action-icon">📚</div>
          <h3>文档管理</h3>
          <p>上传文件、导入链接、管理分类</p>
          <el-button type="primary" text class="action-btn">
            进入 →
          </el-button>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="action-card" @click="$router.push('/chat')">
          <div class="action-icon">🤖</div>
          <h3>AI 问答</h3>
          <p>基于知识库的智能问答</p>
          <el-button type="primary" text class="action-btn">
            进入 →
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="recent-card">
      <template #header>
        <div class="card-header">
          <span>近七天添加的文档</span>
          <el-button type="primary" link @click="$router.push('/documents')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentDocs" style="width: 100%">
        <el-table-column prop="title" label="标题" min-width="250">
          <template #default="{ row }">
            <span class="doc-title-cell">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ getCategoryName(row.categoryId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="120">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="recentDocs.length === 0" description="近七天内暂无新文档" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import api from '../api'

const stats = ref({ documents: 0, categories: 0, chats: 0 })
const recentDocs = ref([])
const categories = ref([])

onMounted(async () => {
  try {
    const [catRes, docRes, sessionRes] = await Promise.all([
      api.listCategories(),
      api.listDocuments(null, 1, 50),
      api.listSessions()
    ])
    categories.value = catRes.data
    stats.value.categories = catRes.data.length
    stats.value.documents = docRes.data.total || 0
    stats.value.chats = sessionRes.data.length
    // 过滤近七天内的文档，最多显示5条
    const allDocs = docRes.data.records || []
    const sevenDaysAgo = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000)
    recentDocs.value = allDocs
      .filter(doc => new Date(doc.createdAt) >= sevenDaysAgo)
      .slice(0, 5)
  } catch (e) {
    console.error('加载失败', e)
  }
})

function getCategoryName(id) {
  return categories.value.find(c => c.id === id)?.name || '未知'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}
</script>

<style scoped>
.home-page {
  max-width: 960px;
}
.page-title {
  font-size: 26px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #1a1a2e;
}
.page-subtitle {
  color: #888;
  font-size: 14px;
  margin-bottom: 28px;
}

.stat-row {
  margin-bottom: 20px;
}
.stat-card {
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: none;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.stat-card.stat-blue { background: linear-gradient(135deg, #e8f4fd, #dbeafe); }
.stat-card.stat-green { background: linear-gradient(135deg, #e8f8ee, #d1fae5); }
.stat-card.stat-purple { background: linear-gradient(135deg, #f0e8fd, #ede9fe); }
.stat-icon {
  font-size: 36px;
}
.stat-info {
  flex: 1;
}
.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #1a1a2e;
}
.stat-label {
  color: #888;
  font-size: 13px;
  margin-top: 2px;
}

.action-row {
  margin-bottom: 20px;
}
.action-card {
  border-radius: 12px;
  padding: 28px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s;
  border: none;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.action-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 24px rgba(0,0,0,0.1);
}
.action-icon {
  font-size: 40px;
  margin-bottom: 12px;
}
.action-card h3 {
  font-size: 16px;
  margin-bottom: 6px;
  color: #1a1a2e;
}
.action-card p {
  color: #888;
  font-size: 13px;
  margin-bottom: 12px;
}

.recent-card {
  border-radius: 12px;
  border: none;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.doc-title-cell {
  font-weight: 500;
  color: #1a1a2e;
}
</style>