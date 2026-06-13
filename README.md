# SmartMail

SmartMail 是一个 Web 邮件系统实训项目。关闭 AI Plugin 时，它是完整的基础邮件系统；启用 AI Plugin 后，它进入类 Notion Mail 的 AI 邮件工作台形态。

## 快速入口

- [项目架构与技术选型.md](./项目架构与技术选型.md)：项目主入口，介绍产品定位、边界、技术栈、模块、API 原则和文档导航。
- [SmartMail项目全貌.md](./SmartMail项目全貌.md)：面向人类快速理解的项目总览。
- [CONTEXT.md](./CONTEXT.md)：项目领域术语和已确认边界。
- [docs/README.md](./docs/README.md)：正式文档目录说明。

## 当前定位

第一版必须稳定跑通：

- 注册、登录、站内邮件收发。
- 本地草稿自动保存。
- 附件选择即上传、发送时绑定、邮件详情下载。
- 收件箱、已发送、草稿、垃圾箱、Junk、用户分类。
- Mail Sync 和新邮件到达通知。
- 邮件搜索。
- AI Plugin 可关闭；开启后自动摘要、分类、Junk 判断、优先级预测和风险提示。
- 当前邮件 Agent。
- 全局 Agent 入口和 mock RAG Tool。
- 类 Notion Mail 的三栏工作台体验。

第一版不强求：

- 外部邮箱真实通信。
- SMTP/POP3/IMAP 主链路。
- Redis、消息队列、向量库作为必需依赖。
- 全量邮箱真实 RAG 问答。

## 技术路线

| 模块 | 技术 |
| --- | --- |
| 前端 | Vue 3、Vite、TypeScript、Element Plus、Pinia、Axios |
| 后端 | Spring Boot 3、Java 17、Spring Security、MyBatis-Plus、MySQL/H2、JWT |
| Agent | Python、FastAPI、Pydantic、httpx、DeepSeek/Ollama 后续接入 |
| 存储 | 主数据库、本地磁盘附件存储 |
| 通信 | HTTP REST、Workspace View API、Internal Tool API |

## 仓库结构

```text
SmartMail/
├─ frontend/                 # Vue 3 前端工作台
├─ backend/                  # Spring Boot 主业务后端
├─ agent/                    # Python FastAPI Agent 服务
├─ docs/                     # 项目正式文档
├─ doc/                      # 课程原始资料、计划书和早期方案
├─ storage/                  # 本地附件存储目录
├─ CONTEXT.md                # 领域术语
├─ SmartMail项目全貌.md       # 人类总览
├─ 项目架构与技术选型.md       # 主入口文档
└─ README.md
```

## 协作约定

- 修改产品边界或核心术语时，先更新 [CONTEXT.md](./CONTEXT.md)。
- 新增或修改接口时，同步更新 [docs/api/](./docs/api/)。
- 修改架构、数据模型或服务边界时，同步更新 [docs/design/](./docs/design/)。
- 前端只调用 Spring Boot API，不直接访问数据库或 Agent。
- Agent 不直连数据库，只通过后端 Internal Tool API 访问业务能力。
- AI 不能阻塞邮件投递，自动分析结果异步补齐。
