package com.assistant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 创建文档的请求 DTO（Data Transfer Object）
 *
 * DTO 用于接收前端传来的数据，与实体类分离的好处：
 * 1. 只暴露必要字段，不暴露 id、createdAt 等服务端生成的字段
 * 2. 可以添加校验注解，在 Controller 层自动校验参数
 *
 * @NotBlank: 不能为空字符串或 null
 * @Size: 限制字符串长度
 */
@Data
public class DocumentDTO {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题最长200字")
    private String title;

    private String content;

    private String summary;

    private Long categoryId;

    private String tags;

    private String sourceUrl;
}