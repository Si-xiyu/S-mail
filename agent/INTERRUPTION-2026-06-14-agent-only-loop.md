# Agent-only 并行循环暂停日志

日期：2026-06-14
分支：`agent/main`
状态：暂停，等待前端/后端同步

## 暂停原因

按用户要求，Agent 并行开发循环应持续到出现以下情况之一：

- 必须与其他开发部门代码同步；
- 涉及需要用户确认的边界问题；
- 异常中断。

Round 3 后，Agent 插件层可独立完成的核心 API 契约已经基本闭环：

- 健康检查：`GET /plugin/v1/health`
- 自动分析：`POST /plugin/v1/analysis/mail`
- 当前邮件/全局聊天：`POST /plugin/v1/agent/chat`
- 写操作待确认：`pendingActions[]`
- 确认后的写操作委派：`POST /plugin/v1/agent/actions/execute`
- 规则 fallback、Basic Mail Mode、结构化分类、mock RAG、backend delegation envelope

继续在 `agent/` 内推进会主要变成重复 mock，而不是交付真实产品能力。下一步必须同步前端/后端接口与基础邮箱 MVP。

## 需要同步的接口

后端需要确认或实现：

- `GET /internal/v1/tools/mail-items/{itemId}/context`
- `POST /internal/v1/tools/mail-actions/execute`
- 自动分析结果落库字段：summary、category、junk、priority、priorityScore、riskLevel、riskHints、modelInfo
- 新邮件到达后调用 Agent 分析 API 的异步管道
- 基础邮箱 MVP：注册、登录、SMTP/IMAP 或等价收发、附件、已读/未读、删除、搜索、通知、草稿本地保存

前端需要确认或实现：

- 邮件右栏展示自动 summary/category/priority/risk
- 当前邮件上下文 Agent 入口
- 全局邮箱 Agent 入口
- `pendingActions[]` 的确认 UI
- 调用 `POST /plugin/v1/agent/actions/execute` 后展示 delegated/rejected/disabled 状态
- AI 插件开关与 Basic Mail Mode 回退体验

## 当前验证状态

最近一次合并后验证：

- `E:\software\Miniconda\python.exe -m compileall app`：通过
- `E:\software\Miniconda\python.exe -B -m unittest discover -s tests`：通过，25 个用例成功

## 恢复条件

满足任一条件后可以恢复下一轮并行开发：

- 后端提供内部工具接口实现或明确最终接口路径/字段；
- 前端开始对接插件 API，需要 Agent 调整契约；
- 用户确认要继续在 Agent 内实现真实 DeepSeek/Ollama/vector RAG/provider，而不是保持 mock；
- 需要将 Agent 服务接入真实新邮件事件、通知或异步任务队列。
