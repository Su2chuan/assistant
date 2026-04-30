package com.assistant.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文档实体
 *
 * @TableName("document") 指定映射到 MySQL 的 document 表
 * @Data 是 Lombok 注解，自动生成 getter/setter/toString/equals/hashCode
 */
@Data
@TableName("document")
public class Document {

    /**
     * 主键ID，自增
     * @TableId(type = IdType.AUTO) 表示使用数据库自增主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档标题 */
    private String title;

    /** 文档正文内容 */
    private String content;

    /** 内容摘要（用于列表展示和 AI 上下文） */
    private String summary;

    /** 分类ID，关联 category 表 */
    private Long categoryId;

    /** 标签，逗号分隔，如 "AI,Agent,LangChain" */
    private String tags;

    /** 来源链接 */
    private String sourceUrl;

    /** 上传文件的存储路径 */
    private String filePath;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}