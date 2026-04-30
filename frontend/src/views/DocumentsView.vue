<template>
  <div class="documents-page">
    <el-container>
      <el-aside width="220px" class="sidebar">
        <div class="sidebar-header">
          <h3>分类</h3>
          <el-button type="primary" size="small" round @click="showCategoryDialog = true">+ 新建</el-button>
        </div>
        <el-menu :default-active="currentCategoryId ?? 'all'" @select="selectCategory" class="cat-menu">
          <el-menu-item index="all">
            <span style="flex:1;">全部</span>
          </el-menu-item>
          <el-menu-item v-for="cat in categories" :key="cat.id" :index="cat.id.toString()">
            <span style="flex:1;">{{ cat.name }}</span>
            <el-icon v-if="cat.id !== 1" class="cat-delete" @click.stop="deleteCategory(cat.id)"><Close /></el-icon>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-main class="main-content">
        <div class="toolbar">
          <el-input v-model="searchKeyword" placeholder="搜索文档..." style="width: 240px;" clearable @keyup.enter="searchDocs" @clear="loadDocuments">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <div style="flex:1;"></div>
          <el-button type="success" round @click="showImportDialog = true">
            <el-icon><Link /></el-icon>导入链接
          </el-button>
          <el-button type="warning" round @click="showUploadDialog = true">
            <el-icon><UploadFilled /></el-icon>上传文件
          </el-button>
          <el-button type="primary" round @click="showCreateDialog = true">
            <el-icon><Plus /></el-icon>新建文档
          </el-button>
        </div>

        <div v-if="loading" v-loading="true" style="min-height: 200px;"></div>

        <template v-else>
          <el-card v-for="doc in documents" :key="doc.id" shadow="hover" class="doc-card">
            <div class="doc-header">
              <div class="doc-title-row">
                <span class="doc-title">{{ doc.title }}</span>
                <div class="doc-actions">
                  <el-button size="small" text type="primary" @click="viewDocument(doc)">查看</el-button>
                  <el-button size="small" text type="warning" @click="openEditCategory(doc)">改分类</el-button>
                  <el-button size="small" text type="danger" @click="deleteDoc(doc)">删除</el-button>
                </div>
              </div>
              <div class="doc-meta">
                <el-tag size="small">{{ getCategoryName(doc.categoryId) }}</el-tag>
                <el-tag v-for="tag in parseTags(doc.tags)" :key="tag" size="small" type="info">{{ tag }}</el-tag>
                <a v-if="doc.sourceUrl" :href="doc.sourceUrl" target="_blank" class="source-link">来源</a>
                <span class="doc-time">{{ formatDate(doc.createdAt) }}</span>
              </div>
              <p v-if="doc.summary" class="doc-summary">{{ doc.summary }}</p>
            </div>
          </el-card>

          <el-empty v-if="documents.length === 0" description="暂无文档" />
        </template>
      </el-main>
    </el-container>

    <!-- 新建分类 -->
    <el-dialog v-model="showCategoryDialog" title="新建分类" width="400px" rounded>
      <el-input v-model="newCategoryName" placeholder="如: Java 并发" @keyup.enter="createCategory" />
      <template #footer>
        <el-button @click="showCategoryDialog = false">取消</el-button>
        <el-button type="primary" @click="createCategory">创建</el-button>
      </template>
    </el-dialog>

    <!-- 导入链接 -->
    <el-dialog v-model="showImportDialog" title="导入链接" width="500px">
      <el-form label-position="top">
        <el-form-item label="链接地址" required>
          <el-input v-model="importUrlVal" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="importCategoryId" style="width:100%;">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="importTags" placeholder="逗号分隔" />
        </el-form-item>
      </el-form>
      <p style="font-size:12px;color:#999;">支持：技术博客、GitHub README、arXiv 论文</p>
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" :loading="importing" @click="doImportUrl">导入</el-button>
      </template>
    </el-dialog>

    <!-- 上传文件 -->
    <el-dialog v-model="showUploadDialog" title="上传文件" width="500px">
      <el-upload drag :auto-upload="false" :on-change="handleFileChange" :limit="1" accept=".pdf,.docx,.md,.txt">
        <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
        <div class="el-upload__text">拖拽文件或点击上传</div>
        <template #tip><div class="el-upload__tip">支持 PDF、Word、Markdown</div></template>
      </el-upload>
      <el-form label-position="top" style="margin-top:16px;">
        <el-form-item label="分类">
          <el-select v-model="uploadCategoryId" style="width:100%;">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="uploadTags" placeholder="逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUploadDialog = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="doUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- 新建文档 -->
    <el-dialog v-model="showCreateDialog" title="新建文档" width="500px">
      <el-form label-position="top">
        <el-form-item label="标题" required>
          <el-input v-model="createTitle" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createCategoryId" style="width:100%;">
            <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="createContent" type="textarea" :rows="5" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="createTags" placeholder="逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="doCreate">创建</el-button>
      </template>
    </el-dialog>

    <!-- 查看文档 -->
    <el-dialog v-model="showViewDialog" width="700px">
      <template #header>
        <span style="font-size:18px;font-weight:600;">{{ viewDocData.title }}</span>
      </template>
      <div class="doc-meta" style="margin-bottom:12px;">
        <el-tag v-for="tag in parseTags(viewDocData.tags)" :key="tag" size="small">{{ tag }}</el-tag>
        <span class="doc-time">{{ formatDate(viewDocData.createdAt) }}</span>
      </div>
      <div v-if="viewDocData.summary" class="view-summary">{{ viewDocData.summary }}</div>
      <div class="view-content">{{ viewDocData.content }}</div>
    </el-dialog>

    <!-- 修改分类 -->
    <el-dialog v-model="showEditCategoryDialog" title="修改分类" width="400px">
      <el-select v-model="editCategoryId" style="width:100%;">
        <el-option v-for="cat in categories" :key="cat.id" :label="cat.name" :value="cat.id" />
      </el-select>
      <template #footer>
        <el-button @click="showEditCategoryDialog = false">取消</el-button>
        <el-button type="primary" @click="doEditCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Link, UploadFilled, Plus, Close } from '@element-plus/icons-vue'
import api from '../api'

const categories = ref([])
const documents = ref([])
const currentCategoryId = ref(null)
const loading = ref(false)
const searchKeyword = ref('')

const showCategoryDialog = ref(false)
const showImportDialog = ref(false)
const showUploadDialog = ref(false)
const showCreateDialog = ref(false)
const showViewDialog = ref(false)

const newCategoryName = ref('')
const importUrlVal = ref('')
const importCategoryId = ref(null)
const importTags = ref('')
const importing = ref(false)
const selectedFile = ref(null)
const uploadCategoryId = ref(null)
const uploadTags = ref('')
const uploading = ref(false)
const createTitle = ref('')
const createCategoryId = ref(null)
const createContent = ref('')
const createTags = ref('')
const viewDocData = ref({})

const showEditCategoryDialog = ref(false)
const editDocId = ref(null)
const editCategoryId = ref(null)

onMounted(async () => {
  await loadCategories()
  currentCategoryId.value = null
  loadDocuments()
})

async function loadCategories() {
  const res = await api.listCategories()
  categories.value = res.data
}

async function loadDocuments() {
  loading.value = true
  const res = await api.listDocuments(currentCategoryId.value, 1, 20)
  documents.value = res.data.records || []
  loading.value = false
}

function selectCategory(id) {
  currentCategoryId.value = id === 'all' ? null : parseInt(id)
  loadDocuments()
}

function getCategoryName(id) {
  if (id === null) return '全部'
  return categories.value.find(c => c.id === id)?.name || '未知'
}

function parseTags(tags) {
  return tags ? tags.split(',').map(t => t.trim()).filter(t => t) : []
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return dateStr.substring(0, 10)
}

async function searchDocs() {
  if (!searchKeyword.value) return loadDocuments()
  loading.value = true
  const res = await api.searchDocuments(searchKeyword.value)
  documents.value = res.data
  loading.value = false
}

async function createCategory() {
  if (!newCategoryName.value.trim()) return ElMessage.warning('请输入分类名称')
  await api.createCategory(newCategoryName.value.trim())
  ElMessage.success('创建成功')
  showCategoryDialog.value = false
  newCategoryName.value = ''
  loadCategories()
}

async function deleteCategory(id) {
  await ElMessageBox.confirm('删除后关联文档将移至默认分类', '提示')
  await api.deleteCategory(id)
  ElMessage.success('已删除')
  if (currentCategoryId.value === id) currentCategoryId.value = 1
  loadCategories()
  loadDocuments()
}

async function doImportUrl() {
  if (!importUrlVal.value.trim()) return ElMessage.warning('请输入链接')
  importing.value = true
  try {
    await api.importUrl(importUrlVal.value.trim(), importCategoryId.value, importTags.value)
    ElMessage.success('导入成功')
    showImportDialog.value = false
    importUrlVal.value = ''
    loadDocuments()
  } catch (e) { ElMessage.error('导入失败') }
  importing.value = false
}

function handleFileChange(file) {
  selectedFile.value = file.raw
}

async function doUpload() {
  if (!selectedFile.value) return ElMessage.warning('请选择文件')
  uploading.value = true
  try {
    const formData = new FormData()
    formData.append('file', selectedFile.value)
    formData.append('categoryId', uploadCategoryId.value)
    formData.append('tags', uploadTags.value)
    await api.uploadFile(formData)
    ElMessage.success('上传成功')
    showUploadDialog.value = false
    loadDocuments()
  } catch (e) { ElMessage.error('上传失败') }
  uploading.value = false
}

async function doCreate() {
  if (!createTitle.value.trim()) return ElMessage.warning('请输入标题')
  await api.createDocument({
    title: createTitle.value.trim(),
    categoryId: createCategoryId.value,
    content: createContent.value,
    tags: createTags.value
  })
  ElMessage.success('创建成功')
  showCreateDialog.value = false
  createTitle.value = ''
  createContent.value = ''
  createTags.value = ''
  loadDocuments()
}

function viewDocument(doc) {
  viewDocData.value = doc
  showViewDialog.value = true
}

async function deleteDoc(doc) {
  await ElMessageBox.confirm(`确定删除「${doc.title}」？`, '提示')
  await api.deleteDocument(doc.id)
  ElMessage.success('已删除')
  loadDocuments()
}

function openEditCategory(doc) {
  editDocId.value = doc.id
  editCategoryId.value = doc.categoryId
  showEditCategoryDialog.value = true
}

async function doEditCategory() {
  await api.updateDocumentCategory(editDocId.value, editCategoryId.value)
  ElMessage.success('分类已更新')
  showEditCategoryDialog.value = false
  loadDocuments()
}
</script>

<style scoped>
.documents-page {
  display: flex;
  gap: 20px;
}
.sidebar {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  width: 200px;
  position: sticky;
  top: 24px;
  height: fit-content;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}
.sidebar-header h3 {
  font-size: 15px;
  color: #1a1a2e;
}
.cat-menu {
  border: none;
}
.cat-menu .el-menu-item {
  border-radius: 8px;
  margin-bottom: 2px;
  min-height: 38px;
  font-size: 13px;
}
.cat-delete {
  font-size: 12px;
  color: #f56c6c;
  cursor: pointer;
  opacity: 0;
  transition: opacity 0.2s;
}
.cat-menu .el-menu-item:hover .cat-delete {
  opacity: 1;
}

.main-content {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  min-height: calc(100vh - 48px);
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  align-items: center;
}
.doc-card {
  margin-bottom: 12px;
  border-radius: 10px;
  border: none;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
  transition: box-shadow 0.2s;
}
.doc-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.doc-card .el-card__body {
  padding: 16px 20px;
}
.doc-title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.doc-title {
  font-size: 15px;
  font-weight: 500;
  color: #1a1a2e;
}
.doc-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}
.doc-card:hover .doc-actions {
  opacity: 1;
}
.doc-meta {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-top: 8px;
  flex-wrap: wrap;
}
.source-link {
  color: #409eff;
  font-size: 12px;
  text-decoration: none;
}
.source-link:hover {
  text-decoration: underline;
}
.doc-time {
  color: #bbb;
  font-size: 12px;
}
.doc-summary {
  color: #888;
  font-size: 13px;
  margin-top: 8px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.view-summary {
  background: #f5f7fa;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 16px;
  font-size: 13px;
  color: #666;
  line-height: 1.6;
}
.view-content {
  font-size: 14px;
  line-height: 1.8;
  max-height: 400px;
  overflow-y: auto;
  white-space: pre-wrap;
}
</style>