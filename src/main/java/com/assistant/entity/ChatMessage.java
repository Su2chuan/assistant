package com.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 聊天消息实体
 *
 * @TableField("session_id") 显式指定数据库列名
 * 因为 Java 字段是 sessionId（驼峰），MyBatis-Plus 默认会转为 session_id
 * 这里显式写出是为了清晰说明映射关系
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 所属会话ID */
    private Long sessionId;

    /** 角色: user（用户） / assistant（AI） */
    private String role;

    /** 消息内容 */
    private String content;

    private LocalDateTime createdAt;
}