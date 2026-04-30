# AI 知识库助手 - 项目文档

## 项目概述

个人成长知识库 + AI 智能问答系统，基于 RAG（检索增强生成）架构。

**目标**：Java + AI Infra 简历亮点项目

**GitHub**：https://github.com/Su2chuan/assistant

## 技术栈

- Java 21 + Spring Boot 3.3.6
- Spring AI 1.0.5（OpenAI 兼容模式接入 DeepSeek Chat + SiliconFlow Embedding）
- MyBatis-Plus 3.5.9（分页插件 mybatis-plus-jsqlparser）
- MySQL 8.x（结构化数据）
- Redis 7.x（Spring Cache 缓存 + 滑动窗口限流）
- Qdrant（向量存储，Spring AI 内置集成）
- Vue 3 + Vite + Element Plus（前端 SPA）
- Docker Compose（MySQL + Qdrant + Redis 中间件编排）
- Hutool 5.8.32 / Apache POI 5.2.5 / PDFBox 3.0.2（文件解析）
- Jsoup 1.18.1（URL 内容抓取）

## 项目结构

```
├── Dockerfile                     # 多阶段构建（Node + Maven + JRE）
├── docker-compose.yml             # 完整编排（MySQL + Qdrant + Redis + App）
├── .env / .env.example            # 敏感配置 / 模板
├── .dockerignore
├── pom.xml
├── init/
│   └── schema.sql                 # 数据库建表脚本
│
├── src/main/java/com/assistant/
│   ├── AssistantApplication.java  # 启动类
│   ├── config/                    # MyBatisPlusConfig, RedisConfig
│   ├── controller/                # REST API + SPA 路由转发
│   │   ├── AuthController         # 密码认证
│   │   ├── CategoryController     # 分类 CRUD
│   │   ├── ChatController         # AI 问答（含限流）
│   │   ├── DocumentController     # 文档管理（含限流）
│   │   └── PageController         # SPA 路由转发
│   ├── service/impl/              # 业务逻辑
│   │   ├── ChatServiceImpl        # RAG 核心流程
│   │   ├── DocumentServiceImpl    # 文档 CRUD + 向量化
│   │   └── CategoryServiceImpl    # 分类管理（含缓存）
│   ├── mapper/                    # MyBatis-Plus Mapper
│   ├── entity/                    # Category, Document, ChatSession, ChatMessage
│   ├── dto/                       # ChatRequest, ChatResponse, DocumentDTO
│   └── util/                      # FileParserUtil, TextChunker, UrlFetcher, RateLimiter
│
├── src/main/resources/
│   └── application.yml            # 环境变量引用，无硬编码密钥
│
└── frontend/                      # Vue 3 前端
    ├── package.json
    ├── vite.config.js             # 构建输出到 src/main/resources/static
    └── src/
        ├── main.js
        ├── App.vue                # 侧边栏导航 + 登录页
        ├── router/index.js        # 路由 + 认证守卫
        ├── api/index.js           # Axios API 封装
        ├── styles/main.css        # 全局样式
        └── views/
            ├── HomeView.vue       # 统计仪表盘
            ├── DocumentsView.vue  # 文档管理（分类侧边栏 + 文档列表）
            ├── ChatView.vue       # AI 问答聊天
            └── LoginView.vue      # 登录页
```

## 核心架构

```
用户提问 → 向量检索(Qdrant) → 拼接上下文 → DeepSeek API → 回答
文档上传 → 文本分块 → Embedding(SiliconFlow) → 向量化 → 存入 Qdrant
URL 导入 → Jsoup/GitHub API 抓取 → 文本分块 → 向量化 → 存入 Qdrant
分类查询 → Redis 缓存 → 命中返回 / 未命中查库并缓存
接口限流 → Redis ZSET 滑动窗口(Lua 脚本) → 超限返回 429
```

## 数据库表

| 表名 | 说明 |
|------|------|
| category | 用户自定义分类（id, name, created_at） |
| document | 文档（标题、内容、摘要、分类ID、标签、来源URL） |
| chat_session | 聊天会话（标题、创建/更新时间） |
| chat_message | 聊天消息（session_id, role, content, created_at） |

## API 接口

### 认证
- `POST /api/auth/login` - 登录（密码验证，返回 token）
- `GET /api/auth/check?token=` - 验证 token

### 分类管理
- `POST /api/categories` - 创建分类
- `GET /api/categories` - 分类列表（Redis 缓存）
- `DELETE /api/categories/{id}` - 删除分类（关联文档移至默认分类，清除缓存）

### 文档管理
- `POST /api/documents` - 创建文档
- `POST /api/documents/upload` - 上传文件（PDF/Word/Markdown）
- `POST /api/documents/import-url` - 导入链接（限流 5次/60秒）
- `GET /api/documents?categoryId=&page=&size=` - 分页查询（支持全部/按分类）
- `GET /api/documents/{id}` - 文档详情
- `GET /api/documents/search?keyword=` - 搜索文档
- `PUT /api/documents/{id}/category` - 修改文档分类
- `DELETE /api/documents/{id}` - 删除文档

### AI 问答
- `POST /api/chat` - 发起问答（RAG，限流 10次/60秒）
- `GET /api/chat/sessions` - 会话列表
- `GET /api/chat/sessions/{id}/messages` - 消息历史
- `DELETE /api/chat/sessions/{id}` - 删除会话

## 开发环境要求

- JDK 21 + Node.js 18+
- Docker（MySQL 8.x + Qdrant + Redis）
- DeepSeek API Key（Chat 模型）
- SiliconFlow API Key（Embedding 模型，BAAI/bge-large-zh-v1.5）

## 本地启动步骤

1. `docker compose up -d` 启动 MySQL、Qdrant、Redis
2. 执行 `init/schema.sql` 创建数据库表
3. 复制 `.env.example` 为 `.env`，填入真实 API Key
4. IDEA 配置 Run Configuration 加载 `.env`（EnvFile 插件或手动设置环境变量）
5. IDEA 启动 `AssistantApplication`
6. `cd frontend && npm run dev` 启动前端开发服务器
7. 访问 http://localhost:5173

## 配置项

所有敏感配置通过 `.env` 文件管理（参考 `.env.example`），application.yml 通过 `${ENV_VAR}` 引用，无硬编码密钥。

| 环境变量 | 说明 |
|----------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL 密码 |
| `DEEPSEEK_API_KEY` | DeepSeek Chat API Key |
| `DEEPSEEK_BASE_URL` | DeepSeek API 地址（默认 https://api.deepseek.com） |
| `SILICONFLOW_API_KEY` | SiliconFlow Embedding API Key |
| `SILICONFLOW_BASE_URL` | SiliconFlow API 地址（默认 https://api.siliconflow.cn） |
| `APP_PASSWORD` | 前端访问密码 |

## 开发进度

- [x] Phase 1: 项目骨架搭建（实体、Mapper、配置）
- [x] Phase 1.5: 向量存储从 ChromaDB 切换为 Qdrant
- [x] Phase 2: 核心功能开发（AI 问答 RAG、文件上传解析、向量检索）
- [x] Phase 3: 知识库重构（分类改为用户自定义、URL 导入、左右布局）
- [x] Phase 4: Vue 3 + Element Plus 前端重构
  - Thymeleaf → Vue SPA（Vite 构建）
  - 侧边栏导航布局、密码保护登录页、首页统计仪表盘
- [x] Phase 4.5: 前端优化与功能完善
  - 文档列表"全部"分类选项、文档分类修改功能
  - 前端布局优化（卡片阴影、hover 效果、间距统一）
  - 时间戳格式化修复、文档数统计修复（MyBatis-Plus 分页插件）
- [x] Phase 4.6: Redis 缓存与接口限流
  - Redis 7.x 缓存（Spring Cache + RedisTemplate）
  - 分类列表缓存（@Cacheable / @CacheEvict）
  - 滑动窗口限流（Lua 脚本 + Redis ZSET）
  - AI 问答限流 10次/60秒、URL 导入限流 5次/60秒
- [x] Phase 4.7: Docker 规范化
  - 多阶段 Dockerfile（Node.js 构建 Vue + Maven 构建 Java）
  - .env 敏感信息管理、.env.example 模板
  - docker-compose 完整编排 + bridge 网络
  - application.yml 环境变量引用（无硬编码密钥）
- [ ] Phase 5: 服务器部署
  - 宝塔面板 + Docker 环境
  - 待解决：服务器 DNS 导致构建失败，App 容器网络连通性问题
