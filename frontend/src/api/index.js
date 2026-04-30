import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

export default {
  // 认证
  login: (password) => api.post('/auth/login', { password }),
  checkAuth: (token) => api.get('/auth/check', { params: { token } }),

  // 分类
  listCategories: () => api.get('/categories'),
  createCategory: (name) => api.post('/categories', { name }),
  deleteCategory: (id) => api.delete(`/categories/${id}`),

  // 文档
  listDocuments: (categoryId, page = 1, size = 20) =>
    api.get('/documents', { params: { ...(categoryId != null && { categoryId }), page, size } }),
  getDocument: (id) => api.get(`/documents/${id}`),
  updateDocumentCategory: (id, categoryId) => api.put(`/documents/${id}/category`, { categoryId }),
  createDocument: (data) => api.post('/documents', data),
  deleteDocument: (id) => api.delete(`/documents/${id}`),
  searchDocuments: (keyword) => api.get('/documents/search', { params: { keyword } }),
  importUrl: (url, categoryId, tags) =>
    api.post('/documents/import-url', { url, categoryId, tags }),
  uploadFile: (formData) => api.post('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  }),

  // 聊天
  chat: (sessionId, question) => api.post('/chat', { sessionId, question }),
  listSessions: () => api.get('/chat/sessions'),
  getSessionMessages: (sessionId) => api.get(`/chat/sessions/${sessionId}/messages`),
  deleteSession: (sessionId) => api.delete(`/chat/sessions/${sessionId}`)
}