package com.assistant.service;

import com.assistant.dto.ChatRequest;
import com.assistant.dto.ChatResponse;
import com.assistant.entity.ChatMessage;
import com.assistant.entity.ChatSession;

import java.util.List;

/**
 * 聊天服务接口
 *
 * 提供基于 RAG（检索增强生成）的 AI 问答功能
 */
public interface ChatService {

    /**
     * 发起 AI 问答（RAG）
     *
     * @param request 包含 sessionId（可选）和 question
     * @return 包含 sessionId、answer、relevantDocCount
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 获取所有聊天会话列表
     */
    List<ChatSession> listSessions();

    /**
     * 获取指定会话的消息历史
     *
     * @param sessionId 会话 ID
     * @return 消息列表，按时间正序排列
     */
    List<ChatMessage> getSessionMessages(Long sessionId);

    /**
     * 删除指定会话及其消息
     *
     * @param sessionId 会话 ID
     */
    void deleteSession(Long sessionId);
}