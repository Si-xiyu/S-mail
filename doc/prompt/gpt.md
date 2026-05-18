# SmartMail 技术选型（Codex Prompt）

你正在协助开发一个“AI Native 智能邮件系统（SmartMail）”。

项目目标：
实现一个类似 Gmail / Notion Mail 的现代化邮件系统，支持用户注册登录、邮件收发、附件管理、邮件搜索，以及基于 Agent 的智能邮件分析功能。

技术架构采用前后端分离 + AI 微服务架构。

---

# 整体架构

```text
Vue Frontend
    ↓ HTTP REST API
Spring Boot Backend
    ├─ MySQL
    ├─ 本地附件目录
    └─ Internal Tool API
            ↓ HTTP
    Python Agent Service
            ↓
    LLM API / Rule Engine / Vector Retrieval
```

---

# 前端技术栈

* Vue 3
* Vite
* TypeScript
* Element Plus
* Axios
* Pinia
* Vue Router

前端职责：

* 登录注册页面
* 收件箱/已发送页面
* 邮件详情页
* 写邮件页面
* AI 助手聊天框
* 邮件搜索
* 标签与优先级展示
* 附件上传下载

UI 风格参考：

* Gmail
* Outlook
* Notion Mail

---

# 后端技术栈（主业务系统）

* Java SE
* Spring Boot 3
* MyBatis-Plus
* MySQL
* JWT Authentication
* Maven

Redis：

* 作为可选增强组件
* 用于验证码、缓存、未读计数、限流等
* 不作为核心依赖

后端职责：

* 用户注册登录
* Token 鉴权
* 邮件发送与接收
* 收件箱与已发送管理
* 邮件搜索
* 已读/未读状态
* 附件上传下载
* AI 结果存储
* Agent Tool API

---

# AI / Agent 技术栈

* Python 3
* FastAPI
* Pydantic
* httpx / requests
* OpenAI / DeepSeek SDK

Agent 采用轻量 Tool Calling Loop 架构：

```python
messages[]
↓
LLM
↓
tool_calls?
↓
execute_tool()
↓
append(tool_result)
↓
LLM
↓
final_answer
```

不要使用重型 Agent Framework（如 LangChain、AutoGen、CrewAI）。

---

# Agent 功能

实现以下 AI 能力：

* 邮件摘要
* 垃圾邮件识别
* 邮件优先级分析
* 自动标签分类
* 回复草稿生成
* 自然语言搜索邮件

---

# Tool Calling 设计

Python Agent 不直接访问数据库。

所有业务操作通过 Spring Boot Internal API 完成：

```text
search_mail(keyword)
get_mail(mail_id)
apply_label(mail_id, label)
draft_reply(mail_id)
mark_as_read(mail_id)
```

调用流程：

```text
Python Agent
    ↓ HTTP
Spring Boot Internal API
    ↓
MySQL
```

---

# 插件化设计

AI 模块采用低耦合插件化架构。

即使 AI 服务关闭：

* 邮件系统仍能正常运行

AI 功能应支持开关控制：

```yaml
ai:
  enable_summary: true
  enable_spam_filter: true
  enable_priority: false
```

---

# 数据存储设计

邮件正文：

* 存储于 MySQL

附件：

* 存储于服务器本地文件系统

数据库只保存：

* 文件路径
* 文件名
* MIME 类型
* 文件大小

---

# 项目目标

项目重点不是“高并发”，而是：

* 完整业务闭环
* 工程化结构
* AI 与业务系统解耦
* 类 Notion Mail 的智能邮件体验
* 类 Claude Code 的 Tool-use Loop

参考产品：

* [Notion Mail](https://www.notion.com/product/mail?utm_source=chatgpt.com)
* [Notion AI Auto Labeling](https://www.notion.com/help/guides/organize-your-inbox-with-notion-ai-auto-labeling?utm_source=chatgpt.com)
* [Notion Mail 技术思路介绍](https://techcrunch.com/2025/04/15/notion-releases-its-ai-driven-email-inbox/?utm_source=chatgpt.com)