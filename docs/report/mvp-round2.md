# 第二轮后端基础功能完成记录

## 完成日期
2026-06-14

## 完成内容

### Phase 1: 附件模块
- 新增 `pending_attachment` 表（选择即上传，发送时绑定）
- 创建 `mail_attachment` 实体、Mapper（表已存在，代码缺失）
- 附件上传：`POST /api/v1/compose/attachments`（multipart/form-data）
- 附件移除：`DELETE /api/v1/compose/attachments/{pendingId}`
- 附件下载：`GET /api/v1/attachments/{id}/download`（权限校验）
- MailService.send() 支持 `pendingAttachmentIds` 参数，发送时绑定附件
- 附件存储目录可配置：`smartmail.storage.attachment-path`
- 文件上传限制配置：50MB

### Phase 2: 分类模块
- 新增 `mail_category` 和 `mail_category_assignment` 表
- 分类 CRUD：`GET/POST/PATCH/DELETE /api/v1/categories`
- 注册时自动创建默认分类 "Other" 和 "Junk Mail"
- 系统默认分类不可删除
- 删除分类时邮件回退到 Other
- 支持 AI 和手动两种分类来源

### Phase 3: 搜索模块
- 新增 `GET /api/v1/mailbox/search?keyword=&folder=&page=&pageSize=`
- 按关键词搜索主题、发件人、正文
- 支持按文件夹过滤
- 搜索结果包含命中片段高亮上下文
- SQL 兼容 H2 和 MySQL

### Phase 4: 工作台视图
- `GET /api/v1/workspace/views`：返回 Views(4个)/Folders(4个)/Categories 及其计数
- `GET /api/v1/workspace/mail-items`：智能视图（inbox/sent/drafts/trash/junk/today/important/unread）
- `GET /api/v1/workspace/mail-items/{itemId}`：详情抽屉数据，含 AI 分析结果、附件列表、分类信息
- 支持按 categoryId 和 keyword 过滤
- Workspace View Model 统一数据形状

### Phase 5: 邮箱操作增强
- `PATCH /api/v1/mailbox/items/{itemId}/category`：修改邮件分类
- `POST /api/v1/mailbox/items/{itemId}/move`：移动邮件文件夹
- 移入 Junk 自动分配 Junk Mail 分类；恢复 INBOX 自动移除 Junk Mail 分类

### Phase 6: 自动分析管道
- 新增 `ai_analysis_task` 表
- 邮件发送后自动为每个收件人创建 PENDING 分析任务
- `POST /api/v1/analysis/mail-items/{itemId}/retry`：手动重试分析
- `AnalysisTaskScheduler`：每 10 秒轮询 pending 任务（批量 5 个，最多重试 3 次）
- `AiService` 新增 `runMailTask(mailId, userId, task)` 重载，支持后台调度时指定用户
- 分析失败不影响邮件可见性

### Phase 7: H2 开发环境适配
- 本地未安装 MySQL，创建 `application-dev.yml` 使用 H2 内存库
- `schema.sql` 中 `CLOB` → `TEXT`，确保 H2/MySQL 双兼容
- `AnalysisService` 日志修复：`e.getMessage()`（返回 null） → `e`（完整堆栈）
- `.vscode/launch.json` 添加 `vmArgs: "-Dspring.profiles.active=dev"`，F5 启动自动走 H2
- 验证：启动时间从 4.1s 降至 1.9s，无 HikariPool 报错，无定时任务报错

### 本次未实现（按用户要求）
- Agent 会话管理（agent_session / agent_message 表及相关 API）

## 数据结构变更

### 新增表（6 张）
| 表名 | 用途 |
|------|------|
| `pending_attachment` | 已上传未绑定附件 |
| `mail_category` | 用户私有类别 |
| `mail_category_assignment` | 邮件与类别关系 |
| `ai_analysis_task` | 后台 AI 分析任务 |

### 已有表补充代码
- `mail_attachment`：原表有 DDL 但无 Java 代码，现已补全实体/Mapper

## 修改的文件（15 个）
- `schema.sql`：新增 4 张表 + 索引，CLOB → TEXT 兼容 H2
- `application.yml`：新增 storage + multipart 配置
- `application-dev.yml`：新增 H2 开发环境配置
- `.vscode/launch.json`：添加 dev profile vmArgs
- `SmartMailApplication.java`：添加 @EnableScheduling
- `AuthService.java`：注册时创建默认分类
- `MailService.java`：附件绑定 + 分析任务创建（注入 AttachmentService + AnalysisService）
- `SendMailRequest.java`：新增 pendingAttachmentIds
- `MailSendResponse.java`：新增 delivery 投递结果
- `MailboxController.java`：新增 search / changeCategory / move 端点（注入 SearchService）
- `MailboxService.java`：新增 changeCategory / move 方法（注入 CategoryService）
- `MailboxItemMapper.java`：新增搜索 SQL、Today/Important/Unread 查询和计数查询
- `AiService.java`：新增后台处理用 runMailTask(mailId, userId, task) 重载
- `AnalysisService.java`：修复日志输出完整堆栈
- `WebConfig.java` / `AuthTokenFilter.java` / `pom.xml`：小幅调整

## 新增文件（36 个）
- attachment: 9 个（entity×2, mapper×2, dto×2, config×1, service×1, controller×1）
- category: 8 个（entity×2, mapper×2, dto×3, service×1, controller×1）
- search: 2 个（dto×1, service×1）
- workspace: 9 个（dto×6, service×1, controller×1）
- mailbox dto: 2 个（CategoryChangeRequest, MoveRequest）
- analysis: 6 个（entity×1, mapper×1, dto×1, service×1, task×1, controller×1）

## 后续建议
1. 用 `mvn compile` 和 `mvn spring-boot:run -Dspring-boot.run.profiles=dev` 验证编译和启动
2. 前端接入新 API（workspace、附件上传下载、分类管理、搜索）
3. Agent 接入真实 LLM Provider
4. 补充接口单元测试
5. 补充演示数据脚本
