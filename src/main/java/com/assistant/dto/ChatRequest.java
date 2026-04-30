package com.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 聊天请求 DTO
 */
@Data
public class ChatRequest {

    /** 会话ID，为空则创建新会话 */
    private Long sessionId;

    /** 用户提问内容 */
    @NotBlank(message = "问题不能为空")
    private String question;
}