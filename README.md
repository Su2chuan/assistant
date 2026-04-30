# AI Knowledge Assistant

个人知识库管理与 AI 智能问答系统，基于 RAG（检索增强生成）架构。用户可将文档或网页内容导入知识库，系统自动解析、分块、向量化存储，并通过语义检索实现 AI 智能问答。

**GitHub**：https://github.com/Su2chuan/assistant

## 功能概览

- **文档管理**：支持 PDF / Word / Markdown / TXT 文件上传，或直接粘贴内容创建文档
- **链接导入**：输入网址自动抓取正文内容（见下方支持列表）
- **分类标签**：自定义分类与标签，灵活组织知识库
- **AI 问答**：基于知识库的语义检索问答，回答附带引用来源
- **会话管理**：多轮对话，自动生成会话标题，保留完整聊天历史

## 支持导入的链接类型

| 类型 | 示例 | 说明 |
|------|------|------|
| GitHub 仓库 | `https://github.com/owner/repo` | 自动调用 GitHub API 获取 README |
| 掘金 | `https://juejin.cn/post/...` | 提取文章正文 |
| CSDN | `https://blog.csdn.net/...` | 提取文章正文 |
| 博客园 | `https://www.cnblogs.com/...` | 提取文章正文 |
| 微信公众号 | `https://mp.weixin.qq.com/...` | 提取图文正文 |
| arXiv 论文 | `https://arxiv.org/abs/...` | 提取摘要信息 |
| 通用网页 | 其他网站 | 优先提取 `<article>` 标签或 `.content` 等正文区域 |

> 不支持需要登录的页面、纯客户端渲染（SPA）的网站。

## 支持上传的文件类型

| 格式 | 扩展名 | 大小限制 |
|------|--------|---------|
| PDF | `.pdf` | 10 MB |
| Word | `.docx` | 10 MB |
| Markdown | `.md` | 10 MB |
| 纯文本 | `.txt` | 10 MB |

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

## 环境要求

- JDK 21 + Node.js 18+
- Docker Desktop（运行 MySQL、Qdrant、Redis）
- DeepSeek API Key（Chat 模型）— https://platform.deepseek.com
- SiliconFlow API Key（Embedding 模型）— https://siliconflow.cn

## 快速启动

### 1. 启动中间件

```bash
docker compose up -d
```

这会启动三个服务：
- MySQL 8.x（端口 3307，用户名 root）
- Qdrant 向量数据库（端口 6333/6334）
- Redis 7.x（端口 6379）

首次启动需要等待约 30 秒让 MySQL 初始化。

### 2. 初始化数据库

```bash
docker exec -i knowledge-mysql mysql -uroot -pSZc+04.25 --default-character-set=utf8mb4 knowledge_db < init/schema.sql
```

### 3. 配置环境变量

复制模板并填入你的 API Key：

```bash
cp .env.example .env
```

编辑 `.env`，填入以下真实值：

| 变量 | 获取方式 |
|------|---------|
| `DEEPSEEK_API_KEY` | DeepSeek 开放平台 → API Keys |
| `SILICONFLOW_API_KEY` | SiliconFlow → API Keys |
| `MYSQL_ROOT_PASSWORD` | 自定义 MySQL 密码（默认 SZc+04.25） |
| `APP_PASSWORD` | 前端登录密码（默认 admin123） |

### 4. 启动后端

**方式一：IDEA（推荐，支持断点调试）**

1. 打开 IDEA → Run → Edit Configurations → AssistantApplication
2. Modify options → 勾选 Environment variables
3. 粘贴 `.env` 文件中的内容，格式：`KEY1=VAL1;KEY2=VAL2`
4. 或安装 EnvFile 插件，直接勾选 `.env` 文件
5. 点击运行

**方式二：命令行**

```bash
# 加载 .env 并启动
export $(cat .env | xargs) && ./mvnw spring-boot:run
```

后端启动成功后控制台显示 `Started AssistantApplication`，运行在 http://localhost:8080

### 5. 启动前端

```bash
cd frontend
npm install   # 首次需要安装依赖
npm run dev   # 启动开发服务器
```

访问 http://localhost:5173，输入密码（默认 admin123）即可使用。

### 6. 功能使用

- **首页**：查看文档总数、分类数量、对话次数统计
- **文档管理**：左侧分类列表，右侧文档列表。支持上传文件（PDF/Word/Markdown）、导入链接、新建文档
- **AI 问答**：基于知识库的智能问答，自动检索相关文档并生成回答

## 常用命令

```bash
# 查看中间件状态
docker compose ps

# 查看 MySQL 数据
docker exec knowledge-mysql mysql -uroot -pSZc+04.25 --default-character-set=utf8mb4 \
  -e "SELECT id, name FROM knowledge_db.category; SELECT COUNT(*) FROM knowledge_db.document;"

# 重建数据库（清空所有数据）
docker compose down && docker volume rm assistant_mysql_data assistant_qdrant_data && docker compose up -d
# 等待 30 秒后重新执行步骤 2

# 构建前端产物到 static/（用于 IDEA 直接启动后端访问前端）
cd frontend && npm run build
```

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
- [x] Phase 4.5: 前端优化与功能完善
- [x] Phase 4.6: Redis 缓存与接口限流
- [x] Phase 4.7: Docker 规范化（Dockerfile、.env、docker-compose 编排）
- [ ] Phase 5: 服务器部署（待后续开发）
