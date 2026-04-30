package com.assistant.controller;

import com.assistant.dto.ChatRequest;
import com.assistant.dto.ChatResponse;
import com.assistant.entity.ChatMessage;
import com.assistant.entity.ChatSession;
import com.assistant.service.ChatService;
import com.assistant.util.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天 REST API 控制器
 *
 * 提供以下接口：
 * - POST /api/chat        → 发起 RAG 问答
 * - GET  /api/chat/sessions → 获取会话列表
 * - GET  /api/chat/sessions/{id}/messages → 获取会话消息
 * - DELETE /api/chat/sessions/{id} → 删除会话
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final RateLimiter rateLimiter;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        String ip = httpRequest.getRemoteAddr();
        if (!rateLimiter.tryAcquire("rate:chat:" + ip, 10, 60)) {
            return ResponseEntity.status(429).build();
        }
        return ResponseEntity.ok(chatService.chat(request));
    }

    /**
     * 获取所有聊天会话列表
     */
    @GetMapping("/sessions")
    public List<ChatSession> listSessions() {
        return chatService.listSessions();
    }

    /**
     * 获取指定会话的消息历史
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public List<ChatMessage> getSessionMessages(@PathVariable Long sessionId) {
        return chatService.getSessionMessages(sessionId);
    }

    /**
     * 删除指定会话及其所有消息
     */
    @DeleteMapping("/sessions/{sessionId}")
    public void deleteSession(@PathVariable Long sessionId) {
        chatService.deleteSession(sessionId);
    }
}