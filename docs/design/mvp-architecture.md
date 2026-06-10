# SmartMail 目标 MVP 架构说明

本文展开根目录 [项目架构与技术选型.md](../../项目架构与技术选型.md) 中的目标 MVP 架构。第一轮原型已经证明三服务骨架可行，下一阶段目标是把原型升级为类 Notion Mail 的 AI 原生邮件工作台。

## 1. 架构目标

MVP 要跑通三条主链路：

1. 邮件主链路：注册登录、站内收发、附件、草稿、本地自动保存、删除、Junk。
2. 自动分析链路：邮件投递后异步生成摘要、分类、Junk 判断和分析状态。
3. 交互 Agent 链路：右侧邮件详情抽屉内当前邮件问答；左侧全局 Agent 入口保留 RAG Tool 边界，MVP 可 mock。

## 2. 服务边界

```text
Vue 3 前端工作台
  -> Spring Boot Workspace API
      -> 主数据库
      -> 本地附件存储
      -> AI 分析任务
      -> Internal Tool API
          -> FastAPI Agent
              -> Automatic Analysis Pipeline
              -> Interactive Tool Router
              -> RAG Tool，MVP mock
```

边界规则：

- 前端只调用 Spring Boot。
- Agent 不直接访问数据库。
- Agent 通过 Internal Tool API 读取邮件上下文和写回允许的结果。
- Redis、消息队列、向量库不是 MVP 必需依赖。

## 3. 前端工作台

MVP 前端体验是评分重点，应按工作台组织，而不是传统 CRUD 页面。

### 3.1 布局

- 左栏 Workspace Navigation：View、Mail、Categories、System、头像、Settings、Global Agent。
- 中栏 Mail List：展示邮件标题、发件人、AI 摘要首句、分类、状态、附件标记、优先级。
- 右栏 Mail Detail Drawer：点击邮件后展开详情，顶部显示 AI 摘要卡片，再显示正文、附件和 Current-Mail Agent。
- Compose Panel：写信、附件上传进度、本地草稿自动保存。

### 3.2 智能视图

左侧 View 可先实现：

- Today
- Important
- Unread
- Junk

Mail 系统视图：

- Inbox
- Sent
- Drafts
- Trash

Categories 展示用户私有类别，包括 Other 和 Junk Mail。

## 4. 后端主业务

后端负责所有业务规则、权限和持久化。

### 4.1 推荐模块

| 模块 | 职责 |
| --- | --- |
| `auth` | 注册、登录、JWT |
| `user` | 用户资料、设置、Agent 写权限开关 |
| `workspace` | 工作台视图 API |
| `mail` | 邮件原文、发送、详情 |
| `mailbox` | Mailbox Item、文件夹、软删除、已读 |
| `attachment` | Pending Attachment、正式附件、下载权限 |
| `category` | 用户类别、AI/手动分类 |
| `analysis` | 自动分析任务和结果 |
| `agent` | 交互 Agent 会话 |
| `internal` | Agent 工具 API |
| `common` | 响应、异常、安全上下文 |

### 4.2 Workspace View Model

前端主体验不直接拼表。中栏列表 API 应返回：

- `itemId`
- `mailId`
- `folder`
- `senderEmail`
- `subject`
- `summaryPreview`
- `category`
- `analysisStatus`
- `read`
- `starred`
- `priority`
- `hasAttachment`
- `receivedAt`

右栏详情 API 应返回：

- 邮件正文和收件人。
- AI 摘要、分类、Junk 判断、分析状态。
- 附件列表和下载入口。
- 当前邮件 Agent 可用状态。

## 5. 数据模型

### 5.1 保留核心

- `sys_user`
- `mail_message`
- `mail_recipient`
- `mailbox_item`
- `mail_attachment`

### 5.2 新增或演进

- `pending_attachment`：已上传未绑定附件。
- `mail_category`：用户私有类别。
- `mail_category_assignment`：邮件类别关系。
- `ai_analysis_task`：后台分析任务。
- `ai_analysis_result` 或演进后的 `mail_ai_result`：摘要、分类、Junk、状态。
- `agent_session`：Agent 会话。
- `agent_message`：对话消息、工具调用、待确认操作。
- `user_setting`：AI 开关、Agent 自动写权限。

### 5.3 草稿

MVP 草稿优先用浏览器本地存储实现 Local Autosave。服务端草稿同步是 MVP 后增强。

如果附件采用选择即上传，后端需要保存 Pending Attachment，并在发送时绑定到正式 `mail_message`。本地草稿恢复时可以恢复正文和收件人；Pending Attachment 恢复可作为增强，不阻塞 MVP。

## 6. 关键流程

### 6.1 发送邮件

```text
用户填写 Compose Panel
  -> 附件选择即上传为 Pending Attachment
  -> 点击发送
  -> 后端创建 mail_message
  -> 创建 mail_recipient
  -> 为发件人创建 SENT Mailbox Item
  -> 为有效收件人创建 INBOX Mailbox Item
  -> 为收件人创建 ai_analysis_task
  -> 返回发送成功
```

无效或外部地址不做真实外部投递，可记录为 failed recipient。

### 6.2 自动分析

```text
ai_analysis_task PENDING
  -> Agent 读取邮件上下文
  -> 生成摘要
  -> 分类到用户已有类别或 Other
  -> 判断是否 Junk
  -> 写回 analysis result
  -> 更新 analysis status
```

AI 失败不影响邮件可见性。

### 6.3 当前邮件 Agent

```text
用户从 Mail Detail Drawer 打开 Agent
  -> 创建 agent_session，scope=current_mail
  -> 用户提问
  -> Interactive Tool Router 读取当前邮件上下文
  -> 返回回答
  -> 如涉及写操作，返回待确认动作
  -> 用户确认后后端执行写操作
```

### 6.4 全局 Agent

MVP 中：

```text
用户从 Workspace Navigation 打开 Global Agent
  -> 创建 agent_session，scope=global
  -> RAG Tool 返回 mock 结果或有限搜索结果
  -> Agent 返回演示级回答
```

MVP 后替换为真实 Mail Collection Q&A。

## 7. AI Runtime

### 7.1 Automatic Analysis Pipeline

- 后台系统任务。
- 权限窄：只写摘要、分类、Junk、分析状态。
- 不需要用户确认。
- 可以用规则 fallback。

### 7.2 Interactive Tool Router

工具分三类：

- `mail_context_tool`：读取当前邮件上下文，MVP 真实实现。
- `rag_tool`：检索用户邮件集合，MVP mock，后续真实实现。
- `mail_action_tool`：分类、标记已读、移入 Junk、生成回复草稿等，写操作默认需确认。

用户可以在 Settings 中开启自动写操作，但 MVP 默认关闭。

## 8. 部署与依赖

MVP 最小部署：

- Spring Boot 后端。
- Vue 前端。
- FastAPI Agent。
- MySQL 或 H2 dev profile。
- 本地附件目录。

增强依赖：

- Redis：通知、缓存、限流。
- 消息队列：AI 任务量增大后替代数据库轮询。
- 向量库：全量邮箱问答。
- Ollama：本地 embedding。

## 9. 实现优先级

1. 重构前端工作台布局和 Workspace API DTO。
2. 补附件选择即上传、发送绑定、下载权限。
3. 补类别和 Junk 视图。
4. 实现 ai_analysis_task 和后台自动分析。
5. 实现当前邮件 Agent。
6. 做 Global Agent + mock RAG Tool。
7. MVP 后接入真实混合检索。
