# Agent 插件并行开发合并报告 - Round 2

日期：2026-06-14
主分支：`agent/main`
来源分支：`agent/dev1`、`agent/dev2`

## 本轮目标

继续在不修改前后端代码的前提下，强化 Agent 插件 API 契约，使其更接近课程要求与产品方案中“可关闭 AI 插件、基础邮箱可独立运行、Agent 通过 API 与前后端解耦”的边界。

## dev1 结果：自动分析契约硬化

- 新增 `RulesAnalysisProvider`，将当前规则 fallback 包装为可替换 provider。
- `/plugin/v1/analyze/incoming-mail` 的分类输出改为结构化对象，同时保留字符串兼容输入。
- 用户自定义分类支持 `{id, name}` 结构，字符串输入会被规范化为分类对象。
- 垃圾邮件优先映射到 `Junk Mail`，插件关闭时返回 `category: null`。
- `modelInfo` 增加 provider/降级信息，用于表达规则 fallback、DeepSeek 配置占位与 LLM 开关状态。
- 风险等级收敛为 `LOW | MEDIUM | HIGH`。

## dev2 结果：聊天与工具路由契约硬化

- `pendingActions[]` 明确包含 `actionId`、`label`、`payload.mailItemId`、`status: PENDING`、`execution: BACKEND_REQUIRED`。
- 写操作在默认策略下保持待确认，顶层聊天状态保持 `SUCCEEDED`，由前端展示确认动作。
- 兼容旧字段 `mailId`，但对外 payload 优先输出 `mailItemId`。
- 当前邮件上下文读取优先调用新内部接口 `/internal/v1/tools/mail-items/{itemId}/context`，失败后降级到旧接口。
- 增加工具路由和插件 API 测试，覆盖禁用插件、全局 mock RAG、当前邮件上下文和写操作待确认。

## 合并处理

- `agent/dev1` 先合入主分支，产生合并提交 `merge: agent plugin analysis contract hardening`。
- `agent/dev2` 以 `--no-commit` 合入后执行组合测试，再生成本报告并提交最终合并。
- `agent/tests/test_plugin_api.py` 自动合并成功，保留了两边新增的断言。
- 工作区中已有的 `CLAUDE.md` 删除和 `AGENT.md` 未跟踪文件不是本轮变更，未纳入合并提交。

## 验证结果

- `E:\software\Miniconda\python.exe -m compileall app`：通过。
- `E:\software\Miniconda\python.exe -B -m unittest discover -s tests`：通过，16 个用例全部成功。
- 编译时通过 `PYTHONPYCACHEPREFIX=$env:TEMP` 避免 Windows 环境下已有 `__pycache__` 权限问题。
- 单测中存在 FastAPI/Starlette 关于 `httpx` 的弃用警告，不影响当前测试结论。

## 当前技术边界

- Agent 插件层已经具备可关闭配置、自动分析 API、聊天 API、工具路由、mock RAG、写操作待确认、规则 fallback 的核心契约。
- 当前实现仍是插件服务侧 mock/规则优先，不调用真实 DeepSeek、Ollama、向量库或后端数据库。
- 新邮件通知、附件解析、真实 IMAP/SMTP 同步、前端交互和后端落库仍应由对应开发部门完成，Agent 只提供 API 契约与可替换 provider/router 边界。

## 下一轮建议

仍可继续在 `agent/` 内推进一轮，不必立即阻塞在前后端同步：

- dev1：补齐插件配置与 provider 选择的文档/契约测试，明确关闭插件、规则 fallback、未来 DeepSeek/Ollama 接入点。
- dev2：补齐工具路由的确认执行 API mock、失败响应结构和后端内部工具调用的错误降级测试。

若下一轮之后要验证真实收件、发件、通知、草稿和附件体验，就需要与后端/前端分支同步接口实现。
