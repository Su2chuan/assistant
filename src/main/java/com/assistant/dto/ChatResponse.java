package com.assistant.dto;

import lombok.Data;

/**
 * 聊天响应 DTO
 */
@Data
public class ChatResponse {

    /** 会话ID */
    private Long sessionId;

    /** AI 回答内容 */
    private String answer;

    /** 本次检索到的相关文档数量 */
    private int relevantDocCount;

    public static ChatResponse of(Long sessionId, String answer, int relevantDocCount) {
        ChatResponse response = new ChatResponse();
        response.setSessionId(sessionId);
        response.setAnswer(answer);
        response.setRelevantDocCount(relevantDocCount);
        return response;
    }
}