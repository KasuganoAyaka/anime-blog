# Anime Blog

[English](./README.md)

Anime Blog 是一个面向动漫风个人站点场景的全栈博客项目，包含 Vue 3 前端、Spring Boot 后端，以及 MySQL 数据库脚本与迁移文件。项目将内容发布、社区互动和站点管理整合在同一个仓库中，适合作为个人博客、练手项目或继续扩展的全栈基础工程。

## 项目亮点

- 提供首页、文章详情、标签页、分类页、关于页和搜索索引
- 支持 Markdown 文章编辑与文章配图上传
- 支持注册、邮箱验证码、登录、刷新令牌、忘记密码、会话管理
- 提供站长、管理员、普通用户三类角色权限控制
- 支持普通用户投稿与管理员审核流程
- 支持评论发布、评论审核、评论举报与游客/匿名评论保护
- 内置音乐模块，支持音乐列表管理与媒体文件上传
- 内置实时聊天，支持 WebSocket、好友申请、拉黑、消息搜索
- 提供联系表单、友链申请和后台回复处理
- 前端支持 `zh-CN`、`zh-TW`、`en` 多语言
- 可选接入 Cloudflare Turnstile 与 IP 归属地解析

## 技术栈

- 前端：Vue 3、Vite 5、Vue Router、Pinia、Vue I18n、Element Plus、md-editor-v3
- 后端：Spring Boot 3.5、Spring Security、MyBatis-Plus、JWT、WebSocket、Java Mail
- 数据库：MySQL 8、初始化 SQL、种子数据、手动迁移脚本
- 构建工具：npm、Maven

## 目录结构

```text
anime-blog/
├─ frontend/      # Vue 3 前端应用
├─ backend/       # Spring Boot API 与 WebSocket 服务
├─ database/      # 建表脚本、初始化数据、迁移脚本
└─ .gitignore
```

## 核心功能

### 前台站点

- 文章列表、文章详情、分类与标签浏览
- 前端搜索索引接口
- 音乐播放器与友情链接展示
- 联系我表单与友链申请表单

### 账号与安全

- 用户名或邮箱登录
- 邮箱验证码注册与找回密码
- Access Token + Refresh Token 鉴权流程
- 会话列表与远程下线
- 可选 Turnstile 人机验证，支持登录、注册发码、游客评论场景

### 内容与审核

- 后台文章管理
- 普通用户投稿工作台与审核发布流程
- 评论审核、驳回、删除与批量处理
- 评论举报查看与处理

### 社交能力

- 私聊与全局聊天
- 好友申请与好友管理
- 用户拉黑、消息已读状态同步
- 消息搜索与联系人搜索

## 快速开始

### 1. 环境要求

- Node.js 18+
- npm 9+
- JDK 17
- Maven 3.9+
- MySQL 8.0+

### 2. 初始化数据库

按下面顺序执行 SQL 文件：

```text
database/init.sql
database/init_data.sql
database/migration_refresh_token.sql
database/migrations/*.sql
```

初始化数据会创建一个默认站长账号：

- 用户名：`admin`
- 密码：`admin123`

首次登录后请立即修改密码。

### 3. 配置后端环境变量

先复制模板文件：

```bash
cd backend
cp .env.template .env
```

后端至少需要补齐这些配置：

- `DB_HOST`
- `DB_PORT`
- `DB_USERNAME`
- `DB_PASSWORD`
- `MAIL_HOST`
- `MAIL_PORT`
- `MAIL_USERNAME`
- `MAIL_PASSWORD`
- `MAIL_FROM_ADDRESS`
- `ANIME_BLOG_JWT_SECRET`

说明：

- 仓库里的 `start.sh` 会自动加载 `.env`。
- 如果你通过 IDE 或 `mvn spring-boot:run` 启动后端，需要先把这些环境变量注入到当前运行进程。

### 4. 启动后端

本地开发：

```bash
cd backend
mvn spring-boot:run
```

默认后端地址：

```text
http://localhost:8080
```

如果是 Linux 服务器并且已经打包成 JAR，也可以使用：

```bash
cd backend
./start.sh start
```

### 5. 启动前端

```bash
cd frontend
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

前端开发服务器会把下面这些路径代理到后端：

- `/api`
- `/uploads`
- `/ws`

### 6. 生产构建

前端构建：

```bash
cd frontend
npm run build
```

后端构建：

```bash
cd backend
mvn clean package
```

## 配置说明

- 后端默认连接的数据库名为 `anime_blog`
- 上传文件默认存储在 `APP_UPLOAD_DIR`，默认值为 `./storage/uploads`
- `frontend/dist`、`backend/target` 等构建产物已经在 `.gitignore` 中排除
- `backend/storage` 被视为运行期数据目录，不会提交到仓库
- 本地开发时默认允许 `http://localhost:5173` 访问后端接口

## 常用文件

- `frontend/package.json`：前端脚本命令
- `frontend/vite.config.js`：开发端口与代理配置
- `backend/.env.template`：环境变量模板
- `backend/start.sh`：后端打包部署脚本
- `database/init.sql`：数据库初始化结构
- `database/init_data.sql`：数据库种子数据

## 后续可扩展方向

这个项目已经具备继续扩展的基础，可以继续往这些方向演进：

- 更完整的内容审核工作流
- 更丰富的模块页与站点页面
- 更强的聊天与社交功能
- 更完善的生产环境部署方案
- 自动化数据库迁移管理

## License

当前仓库还没有附带开源许可证。如果你准备公开开源，建议补充合适的 License 文件。
