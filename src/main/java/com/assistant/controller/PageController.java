package com.assistant.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA 路由转发控制器
 *
 * 所有页面路由转发到 index.html，由 Vue Router 处理前端路由
 * /api/* 路径由 RestController 处理，不经过这里
 */
@Controller
public class PageController {

    @GetMapping(value = {"/", "/documents", "/chat"})
    public String forward() {
        return "forward:/index.html";
    }
}