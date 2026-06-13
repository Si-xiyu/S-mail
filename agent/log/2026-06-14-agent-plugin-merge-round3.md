# Agent 插件并行开发合并报告 - Round 3

日期：2026-06-14
主分支：`agent/main`
来源分支：`agent/dev1`、`agent/dev2`

## 本轮目标

在不修改前端/后端的前提下，继续强化 Agent 插件 API 的可集成性：dev1 负责自动分析插件的文档、fixtures 与契约测试；dev2 负责用户确认后的写操作执行 API 契约。

## dev1 结果：分析插件契约文档化

- 更新 `agent/README.md`，补充插件 API 契约入口：
  - `GET /plugin/v1/health`
  - `POST /plugin/v1/analysis/mail`
  - `POST /plugin/v1/agent/chat`
- 明确 Basic Mail Mode：`aiPluginEnabled=false` 时返回 `DISABLED`，不生成 AI category/priority。
- 明确分析响应字段、结构化 category、`Other` / `Junk Mail` fallback、`modelInfo.provider/mode/fallbackUsed`。
- 新增 `agent/tests/fixtures.py`，沉淀稳定请求 fixtures，覆盖普通邮件、垃圾/钓鱼邮件、禁用插件、分类 id/name 保留、DeepSeek 配置但规则降级。
- 扩展分析契约测试，覆盖 provider、fallback、risk/priority 枚举边界。

## dev2 结果：确认写操作执行契约

- 新增 `POST /plugin/v1/agent/actions/execute`。
- 新增执行请求/响应 schema：
  - `ConfirmedActionExecuteRequest`
  - `ConfirmedActionExecuteResponse`
  - `BackendOperation`
  - `ActionExecutionStatus`
  - `ActionExecutionMode`
- 行为边界：
  - 插件关闭返回 `DISABLED`，不产生 backend operation。
  - `confirmed=false` 且 `agentAutoWriteEnabled=false` 返回 `REJECTED`。
  - `confirmed=true` 返回 `DELEGATED` + `BACKEND_REQUIRED`。
  - `agentAutoWriteEnabled=true` 可免确认委派，但 Agent 仍不直接改本地/后端邮件状态。
  - 兼容 legacy `mailId` / `mail_id` / `mail_item_id`，对外归一化为 `mailItemId`。
  - 不支持的 action type 返回 `REJECTED`。

## 合并处理

- `agent/dev1` 先合入主分支，产生合并提交 `merge: agent analysis contract docs`。
- `agent/dev2` 以 `--no-commit` 合入，自动合并成功。
- 共享测试文件 `agent/tests/test_plugin_api.py` 自动保留了 dev1 的分析契约测试与 dev2 的 action execute endpoint 测试。
- 工作区中已有的 `CLAUDE.md` 删除和 `AGENT.md` 未跟踪文件不是本轮变更，未纳入合并提交。

## 验证结果

- `E:\software\Miniconda\python.exe -m compileall app`：通过。
- `E:\software\Miniconda\python.exe -B -m unittest discover -s tests`：通过，25 个用例全部成功。
- 编译时通过 `PYTHONPYCACHEPREFIX=$env:TEMP` 避免 Windows 环境下已有 `__pycache__` 权限问题。
- 单测中存在 FastAPI/Starlette 关于 `httpx` 的弃用警告，不影响当前测试结论。

## 当前技术边界

- Agent 插件层现在已经覆盖：健康检查、自动分析、规则 fallback、结构化分类、插件关闭、当前邮件 chat、全局 mock RAG、写操作 pending action、确认执行后的 backend delegation envelope。
- Agent 仍不做真实 DB 写入、真实后端状态变更、真实 DeepSeek/Ollama/向量检索调用。
- 新邮件通知、IMAP/SMTP 同步、草稿本地存储、附件上传/下载、前端抽屉与确认交互、后端落库/权限校验仍必须由对应开发部门接入。

## 继续推进判断

Agent-only 可做的核心契约已经基本闭环。下一步若继续并行开发，工作会自然落到：

- 后端实现 `/internal/v1/tools/mail-items/{itemId}/context` 与 `/internal/v1/tools/mail-actions/execute`。
- 前端对接 `pendingActions` 与 `/plugin/v1/agent/actions/execute` 的确认 UI。
- 基础邮箱 MVP 的注册、收发、附件、搜索、删除、已读/未读、通知、草稿本地保存。

因此本轮后建议暂停 Agent-only 循环，先进行前后端接口同步，再继续下一轮集成开发。
