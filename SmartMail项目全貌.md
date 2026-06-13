# SmartMail 项目全貌

SmartMail 是一个课程实训项目。基础形态是完整的 Web 邮件系统；启用 AI Plugin 后，产品进入类 Notion Mail 的 AI 邮件工作台形态。

## 一句话理解

用户在同一个 SmartMail 服务器内收发邮件。关闭 AI Plugin 时，它是基础邮箱系统；启用 AI Plugin 时，系统自动为新邮件生成摘要、分类、Junk 判断、风险提示和优先级，用户还可以在邮件详情中呼出当前邮件 Agent。

## 第一版要做什么

- 注册、登录、站内邮箱通信。
- 写信、发信、收信、已发送、垃圾箱、Junk。
- 登录后自动拉取邮件，支持刷新和轻量轮询同步。
- 新邮件到达时显示站内通知、未读数或徽标更新。
- 本地草稿自动保存。
- 附件上传和下载，体验接近现代 Web 邮箱。
- 搜索邮件，支持按主题、发件人、正文和分类检索。
- 邮件列表中展示摘要、分类、附件、状态和优先级。
- 点击邮件后右侧抽屉展示详情。
- 收到邮件后自动生成摘要、分类、Junk 判断、优先级预测和安全风险提示。
- 右侧详情内提供当前邮件 Agent。
- 左侧提供全局 Agent 入口，MVP 中可先使用 mock RAG Tool。
- AI Plugin 可关闭；关闭后仍能使用基础邮件系统。

## 第一版不做什么

- 不做真实外部邮箱投递。
- 不做 SMTP/POP3/IMAP 主链路。
- 不要求 Redis、消息队列、向量库。
- 不要求附件在线预览、病毒扫描、断点续传。
- 不让 AI 分析阻塞邮件投递。
- 不要求为了 AI Plugin 实现 DLL；DLL Adapter 作为扩展方向。

## 用户界面

产品以桌面端工作台为主。

```text
左侧：Workspace Navigation
  - View
  - Mail
  - Categories
  - System / Global Agent
  - User / Settings

中栏：Mail List
  - 主题
  - 发件人
  - AI 摘要首句
  - 分类
  - 附件标记
  - 分析状态

右栏：Mail Detail Drawer
  - AI 摘要卡片
  - 邮件正文
  - 附件列表
  - 当前邮件 Agent
```

## 技术架构

```text
Vue 3 前端
  -> Spring Boot 后端
      -> 主数据库
      -> 本地附件存储
      -> AI 分析任务
      -> Internal Tool API
          -> Python FastAPI Agent
```

前端只调用后端。Agent 不直接访问数据库，而是通过后端提供的内部工具 API 读取邮件上下文、写回 AI 结果或执行用户确认后的操作。

## AI 设计

SmartMail 有两条 AI 通道。

自动分析管道：

- 邮件投递后后台运行。
- 自动写摘要、分类、Junk 和分析状态。
- 不阻塞邮件进入收件箱。
- 失败时邮件仍可正常阅读。

交互式 Tool Router：

- 用户主动打开 Agent。
- 默认只读。
- 写操作需要用户确认。
- 用户可在 Settings 中开启自动写，但不是默认行为。

## 附件和草稿

附件采用“选择即上传，发送时绑定”：

1. 用户选择文件。
2. 文件上传为 Pending Attachment。
3. 发送邮件时绑定到正式邮件。
4. 发件人和有权限的收件人可下载。

草稿第一版优先做本地自动保存，保证同一浏览器中刷新或短暂中断后能恢复。跨设备草稿同步放到后续增强。

## 数据模型重点

最重要的模型边界是“邮件原文”和“用户邮箱条目”分离：

- `mail_message`：一封邮件的正文和发送信息。
- `mailbox_item`：某个用户视角下这封邮件的位置、已读、星标、删除、优先级。

这样一个收件人删除邮件，不会影响发件人的已发送，也不会影响其他收件人。

## 后续增强

MVP 跑通后必须追加：

- 全量邮箱问答。
- BM25 + 向量 + RRF 混合检索。
- 向量失败降级 BM25。
- Ollama 本地 embedding 模型。
- DeepSeek 主问答模型。
- 草稿服务器同步。
- SSE/WebSocket 新邮件通知增强实现。

## 阅读顺序

1. [项目架构与技术选型.md](./项目架构与技术选型.md)
2. [CONTEXT.md](./CONTEXT.md)
3. [docs/design/mvp-architecture.md](./docs/design/mvp-architecture.md)
4. [docs/api/mvp-api.md](./docs/api/mvp-api.md)
5. [docs/README.md](./docs/README.md)
