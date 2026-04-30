package com.assistant.service.impl;

import com.assistant.dto.ChatRequest;
import com.assistant.dto.ChatResponse;
import com.assistant.entity.ChatMessage;
import com.assistant.entity.ChatSession;
import com.assistant.mapper.ChatMessageMapper;
import com.assistant.mapper.ChatSessionMapper;
import com.assistant.service.ChatService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天服务实现类 —— RAG 核心逻辑
 *
 * RAG（Retrieval-Augmented Generation）流程：
 * 1. 用户提问 → 保存到数据库
 * 2. 用 VectorStore 在 Qdrant 中做相似度搜索，找到相关文档片段
 * 3. 把检索到的内容作为上下文，和用户问题一起发给 DeepSeek API
 * 4. AI 回答 → 保存到数据库 → 返回给前端
 *
 * 为什么用 RAG？
 * - 直接问 LLM：回答可能不准确，因为它不知道你的个人知识库内容
 * - 用 RAG：先从你的知识库中检索相关内容，让 LLM 基于这些内容回答，更准确
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatSessionMapper sessionMapper;
    private final ChatMessageMapper messageMapper;
    private final ChatModel chatModel;
    private final VectorStore vectorStore;

    /**
     * 系统提示词 —— 定义 AI 的角色和行为
     *
     * 这段 prompt 告诉 AI：
     * 1. 你是一个知识库助手
     * 2. 优先基于检索到的知识内容回答
     * 3. 如果没有相关内容，可以自行回答但要说明
     */
    private static final String SYSTEM_PROMPT = """
            你是一个个人知识库助手。请根据以下检索到的知识内容回答用户问题。
            如果检索到的内容与问题不相关，可以结合你的知识回答，但要说明哪些是你的理解。
            请用中文回答，回答要简洁清晰。
            """;

    /**
     * 当没有检索到相关文档时的补充提示
     */
    private static final String NO_CONTEXT_PROMPT = """
            当前知识库中没有检索到与用户问题直接相关的内容。
            请基于你的知识回答，并在开头说明：以下回答并非基于知识库内容。
            """;

    @Override
    @Transactional
    public ChatResponse chat(ChatRequest request) {
        // 1. 获取或创建会话
        ChatSession session = getOrCreateSession(request.getSessionId(), request.getQuestion());

        // 2. 保存用户消息
        saveMessage(session.getId(), "user", request.getQuestion());

        // 3. RAG 检索：在 Qdrant 中搜索相关文档片段
        // 如果向量化失败（如 Embedding API 不可用），降级为空结果，AI 仍能回答
        List<Document> relevantDocs = List.of();
        try {
            relevantDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(request.getQuestion())
                            .topK(5)
                            .similarityThreshold(0.5)
                            .build()
            );
        } catch (Exception e) {
            log.warn("向量检索失败，降级为无上下文模式: {}", e.getMessage());
        }
        log.debug("检索到 {} 条相关文档", relevantDocs.size());

        // 4. 拼接上下文 + 构建 prompt
        String context = relevantDocs.stream()
                .map(doc -> doc.getText())
                .collect(Collectors.joining("\n\n"));

        String systemText = SYSTEM_PROMPT;
        if (context.isEmpty()) {
            systemText += NO_CONTEXT_PROMPT;
        } else {
            systemText += "\n\n检索到的知识内容：\n" + context;
        }

        // 5. 构建消息列表：系统提示 + 最近对话历史 + 当前问题
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(systemText));

        // 加载最近 10 条历史消息，保持对话连贯性
        List<ChatMessage> history = getRecentMessages(session.getId(), 10);
        for (ChatMessage msg : history) {
            if ("user".equals(msg.getRole())) {
                messages.add(new UserMessage(msg.getContent()));
            } else {
                messages.add(new AssistantMessage(msg.getContent()));
            }
        }

        // 6. 调用 DeepSeek API 生成回答
        Prompt prompt = new Prompt(messages);
        String answer = chatModel.call(prompt)
                .getResult()
                .getOutput()
                .getText();

        // 7. 保存 AI 回答
        saveMessage(session.getId(), "assistant", answer);

        // 8. 返回结果
        return ChatResponse.of(session.getId(), answer, relevantDocs.size());
    }

    @Override
    public List<ChatSession> listSessions() {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(ChatSession::getCreatedAt);
        return sessionMapper.selectList(wrapper);
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .orderByAsc(ChatMessage::getCreatedAt);
        return messageMapper.selectList(wrapper);
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId) {
        // 先删除会话下的所有消息
        LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessage::getSessionId, sessionId);
        messageMapper.delete(msgWrapper);

        // 再删除会话
        sessionMapper.deleteById(sessionId);
    }

    /**
     * 获取或创建聊天会话
     *
     * 如果 sessionId 为空，创建新会话，标题取问题前 20 个字符
     */
    private ChatSession getOrCreateSession(Long sessionId, String question) {
        if (sessionId != null) {
            return sessionMapper.selectById(sessionId);
        }

        ChatSession session = new ChatSession();
        session.setTitle(question.length() > 20 ? question.substring(0, 20) + "..." : question);
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);
        return session;
    }

    /**
     * 保存一条聊天消息
     */
    private void saveMessage(Long sessionId, String role, String content) {
        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(message);
    }

    /**
     * 获取最近 N 条消息（用于保持对话上下文连贯）
     */
    private List<ChatMessage> getRecentMessages(Long sessionId, int limit) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
               .orderByDesc(ChatMessage::getCreatedAt)
               .last("LIMIT " + limit);
        List<ChatMessage> messages = messageMapper.selectList(wrapper);

        // 反转列表，让时间正序（最早的在前）
        java.util.Collections.reverse(messages);
        return messages;
    }
}