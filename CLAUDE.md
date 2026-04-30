# AI 知识库助手 - 项目文档

## 项目概述

个人成长知识库 + AI 智能问答系统，基于 RAG（检索增强生成）架构。

**目标**：Java + AI Infra 简历亮点项目

## 技术栈

- Java 21 + Spring Boot 3.3.6
- Spring AI 1.0.5（OpenAI 兼容模式接入 DeepSeek Chat + SiliconFlow Embedding）
- MyBatis-Plus 3.5.9
- MySQL 8.x（结构化数据）
- Redis 7.x（缓存、接口限流）
- Qdrant（向量存储，Spring AI 内置集成，开箱即用）
- Vue 3 + Vite + Element Plus（现代化前端 SPA）
- Docker Compose（部署）
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
│   ├── AssistantApplication.java
│   ├── config/                    # MyBatisPlus, Redis
│   ├── controller/                # REST API + SPA 路由转发
│   ├── service/                   # 服务接口与实现
│   ├── mapper/                    # MyBatis-Plus Mapper
│   ├── entity/                    # 实体类
│   ├── dto/                       # 数据传输对象
│   └── util/                      # 工具类（文件解析、文本分块、限流、URL 抓取）
│
├── src/main/resources/
│   └── application.yml            # 应用配置（环境变量引用）
│
└── frontend/                      # Vue 3 前端
    ├── package.json
    ├── vite.config.js
    └── src/
        ├── main.js
        ├── App.vue
        ├── router/index.js
        ├── api/index.js
        ├── styles/main.css
        └── views/                 # 首页、文档管理、AI 问答、登录
```

## 知识分类

分类由用户自定义管理（category 表），默认提供：默认、技术博客、AI 研究、开源项目。

## 核心架构

```
用户提问 → 向量检索(Qdrant) → 拼接上下文 → DeepSeek API → 回答
文档上传 → 文本分块 → Embedding 向量化 → 存入 Qdrant
URL 导入 → Jsoup/GitHub API 抓取 → 文本分块 → 向量化 → 存入 Qdrant
```

## 数据库表

| 表名 | 说明 |
|------|------|
| category | 用户自定义分类 |
| document | 文档（标题、内容、分类ID、标签、来源URL） |
| chat_session | 聊天会话 |
| chat_message | 聊天消息（user/assistant） |

## API 接口

### 认证
- `POST /api/auth/login` - 登录（密码验证）
- `GET /api/auth/check?token=` - 验证 token

### 分类管理
- `POST /api/categories` - 创建分类
- `GET /api/categories` - 分类列表
- `DELETE /api/categories/{id}` - 删除分类

### 文档管理
- `POST /api/documents` - 创建文档
- `POST /api/documents/upload` - 上传文件
- `POST /api/documents/import-url` - 导入链接（自动抓取内容）
- `GET /api/documents?categoryId=&page=&size=` - 分页查询
- `GET /api/documents/{id}` - 文档详情
- `GET /api/documents/search?keyword=` - 搜索文档
- `PUT /api/documents/{id}/category` - 修改文档分类
- `DELETE /api/documents/{id}` - 删除文档

### AI 问答
- `POST /api/chat` - 发起问答（RAG）
- `GET /api/chat/sessions` - 会话列表
- `GET /api/chat/sessions/{id}/messages` - 消息历史
- `DELETE /api/chat/sessions/{id}` - 删除会话

## 开发环境要求

- JDK 21 + Node.js 18+
- Docker（MySQL 8.x + Qdrant）
- DeepSeek API Key（Chat 模型）
- SiliconFlow API Key（Embedding 模型，BAAI/bge-large-zh-v1.5）

## 本地启动步骤

1. `docker-compose up -d` 启动 MySQL、Qdrant、Redis
2. 执行 `init/schema.sql` 创建数据库表（已有数据库则执行 `init/migration_category.sql`）
3. 复制 `.env.example` 为 `.env`，填入真实 API Key
4. IDEA 启动 `AssistantApplication`（可通过 EnvFile 插件加载 .env，或在 Run Configuration 设置环境变量）
5. `cd frontend && npm run dev` 启动前端开发服务器
6. 访问 http://localhost:5173

**生产部署**：
```bash
docker-compose up -d --build   # 构建镜像并启动全部服务
```

## 配置项

所有敏感配置通过 `.env` 文件管理（参考 `.env.example`），application.yml 通过 `${ENV_VAR}` 引用。

| 环境变量 | 说明 |
|----------|------|
| `MYSQL_ROOT_PASSWORD` | MySQL 密码 |
| `DEEPSEEK_API_KEY` | DeepSeek Chat API Key |
| `DEEPSEEK_BASE_URL` | DeepSeek API 地址（默认 https://api.deepseek.com） |
| `SILICONFLOW_API_KEY` | SiliconFlow Embedding API Key |
| `SILICONFLOW_BASE_URL` | SiliconFlow API 地址（默认 https://api.siliconflow.cn） |
| `APP_PASSWORD` | 前端访问密码（默认 admin123） |

本地开发时，IDEA 可通过 EnvFile 插件或 Run Configuration 加载 `.env`。

## 服务器部署

- 2核2G Linux 云服务器
- Docker Compose 一键部署：`docker-compose up -d --build`
- 完整编排：MySQL + Qdrant + Redis + Spring Boot App
- 预计内存占用 ~1300MB / 2048MB

## 开发进度

- [x] Phase 1: 项目骨架搭建（实体、Mapper、配置、基础页面）
- [x] Phase 1.5: 向量存储从 ChromaDB 切换为 Qdrant（Spring AI 开箱即用）
- [x] Phase 2: 核心功能开发（AI 问答 RAG、文件上传解析、向量检索）
- [x] Phase 3: 知识库重构
  - 合并学习路线模块到文档管理，删除状态追踪
  - 分类改为用户自定义（category 表）
  - 新增 URL 导入功能（Jsoup + GitHub API）
  - 前端左右布局：左侧分类列表，右侧文档列表
- [x] Phase 4: Vue 3 + Element Plus 前端重构
  - Thymeleaf → Vue SPA（Vite 构建）
  - 侧边栏导航布局
  - 密码保护登录页
  - 首页统计仪表盘
- [x] Phase 4.5: 前端优化与功能完善
  - 文档列表新增"全部"分类选项
  - 文档分类修改功能（后端 PUT 接口 + 前端弹窗）
  - 前端布局优化（卡片阴影、hover 效果、间距统一）
  - 时间戳格式化修复、文档数统计修复（MyBatis-Plus 分页插件）
- [x] Phase 4.6: Redis 缓存与接口限流
  - Redis 7.x 缓存（Spring Cache + RedisTemplate）
  - 分类列表缓存（@Cacheable / @CacheEvict）
  - 滑动窗口限流（Lua 脚本 + Redis ZSET）
  - AI 问答接口限流（10次/60秒）、URL 导入限流（5次/60秒）
- [ ] Phase 5: Docker 规范化部署
  - 多阶段 Dockerfile（Node.js 构建 Vue + Maven 构建 Java）
  - .env 敏感信息管理
  - docker-compose 完整编排（MySQL + Qdrant + Redis + App）
