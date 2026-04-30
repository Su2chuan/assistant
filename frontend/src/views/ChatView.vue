<template>
  <div class="chat-page">
    <div class="chat-box">
      <div class="chat-messages" ref="messagesContainer">
        <div v-if="messages.length === 0" class="chat-placeholder">
          <div class="placeholder-icon">🤖</div>
          <h3>AI 知识库问答</h3>
          <p>向 AI 提问关于你知识库中的内容</p>
        </div>
        <div v-for="(msg, idx) in messages" :key="idx" :class="['message', msg.role]">
          <div v-if="msg.role === 'assistant'" class="avatar ai-avatar">AI</div>
          <div class="message-bubble">
            <div class="message-content">{{ msg.content }}</div>
          </div>
          <div v-if="msg.role === 'user'" class="avatar user-avatar">我</div>
        </div>
        <div v-if="thinking" class="message assistant">
          <div class="avatar ai-avatar">AI</div>
          <div class="message-bubble thinking-bubble">
            <el-icon class="is-loading"><Loading /></el-icon>
            思考中...
          </div>
        </div>
      </div>
      <div class="chat-input-area">
        <el-input
          v-model="question"
          placeholder="输入你的问题..."
          size="large"
          @keyup.enter="sendQuestion"
          :disabled="thinking"
        >
          <template #append>
            <el-button type="primary" :loading="thinking" @click="sendQuestion">
              发送
            </el-button>
          </template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import api from '../api'

const question = ref('')
const messages = ref([])
const thinking = ref(false)
const sessionId = ref(null)
const messagesContainer = ref(null)

async function sendQuestion() {
  if (!question.value.trim() || thinking.value) return

  const q = question.value.trim()
  question.value = ''
  messages.value.push({ role: 'user', content: q })
  thinking.value = true
  scrollToBottom()

  try {
    const res = await api.chat(sessionId.value, q)
    sessionId.value = res.data.sessionId
    messages.value.push({
      role: 'assistant',
      content: res.data.answer
    })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '请求失败，请检查服务是否正常运行' })
  }

  thinking.value = false
  scrollToBottom()
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}
</script>

<style scoped>
.chat-page {
  display: flex;
  justify-content: center;
}
.chat-box {
  width: 100%;
  max-width: 860px;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 48px);
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.06);
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.chat-placeholder {
  text-align: center;
  padding: 80px 0;
  color: #ccc;
}
.placeholder-icon {
  font-size: 56px;
  margin-bottom: 16px;
}
.chat-placeholder h3 {
  color: #1a1a2e;
  font-size: 18px;
  margin-bottom: 8px;
}
.chat-placeholder p {
  font-size: 13px;
  color: #999;
}

.message {
  margin-bottom: 20px;
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.message.user {
  justify-content: flex-end;
}
.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  flex-shrink: 0;
}
.ai-avatar {
  background: #f0f2f5;
  color: #409eff;
}
.user-avatar {
  background: #409eff;
  color: #fff;
}
.message-bubble {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.7;
}
.message.user .message-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}
.message.assistant .message-bubble {
  background: #f5f7fa;
  color: #333;
  border-bottom-left-radius: 4px;
}
.thinking-bubble {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #999;
}
.message-content {
  white-space: pre-wrap;
  word-break: break-word;
}

.chat-input-area {
  padding: 16px 24px;
  border-top: 1px solid #f0f0f0;
}
.chat-input-area .el-input-group__append {
  padding: 0;
}
.chat-input-area .el-input-group__append .el-button {
  border-radius: 0 var(--el-border-radius-base) var(--el-border-radius-base) 0;
}
</style>