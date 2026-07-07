# SmartMail Agent 最终对接文档

本文是前端、后端、Agent 三方本地联调的主文档。接口字段以后端当前实现为准；示例邮箱统一使用 `@smail.com`。

## 1. 总体边界

```text
Frontend
  -> Backend Public API
      -> Agent Plugin API
          -> Backend Internal Tool API
```

三条调用链必须分清：

1. **Frontend -> Backend Public API**：前端只调用 Spring Boot 的 `/api/v1/**`，不直连 Agent。
2. **Backend -> Agent Plugin API**：后端在自动分析或交互 Agent 时调用 Python FastAPI 的 `/plugin/v1/**`。
3. **Agent -> Backend Internal Tool API**：Agent 只通过后端 `/internal/v1/tools/**` 读取邮件上下文、搜索邮件或提交工具结果，不访问数据库。

后端是唯一的鉴权、权限校验和持久化中心。Agent 返回的写操作默认只是 `pendingActions`，必须由后端保存并在用户确认后执行。

## 2. 通用约定

### 2.1 本地地址

| 服务 | 默认地址 | 说明 |
| --- | --- | --- |
| Frontend | `http://127.0.0.1:5173` | Vue/Vite 工作台 |
| Backend | `http://localhost:8080` | Spring Boot public API 和 internal API |
| Agent | `http://127.0.0.1:8000` | Python FastAPI Agent Plugin |

### 2.2 响应 envelope

后端公开 API 和 Internal Tool API 统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

文档中的后端响应示例默认展示 `data` 内部结构。

### 2.3 认证头

前端调用后端公开 API：

```http
Authorization: Bearer <jwt>
```

后端调用 Agent Plugin：

```http
X-Plugin-Token: smartmail-agent-plugin-dev-token
```

Agent 调用后端 Internal Tool API：

```http
X-Internal-Token: smartmail-internal-dev-token
```

### 2.4 字段命名基准

以后端 DTO 和 Controller 为准：

| 场景 | 字段 | 说明 |
| --- | --- | --- |
| 邮箱地址 | `demo@smail.com` | 只接受 `@smail.com` 后缀 |
| Workspace 当前邮件 | `itemId` | 后端公开 API 路径和响应使用 `itemId` |
| Agent 当前邮件上下文 | `context.mailItemId` | 创建 Agent 会话和 Plugin 请求使用 `mailItemId` |
| Agent 用户输入 | `message` | `POST /api/v1/agent/sessions/{sessionId}/messages` 请求字段是 `message` |
| 邮件正文 | `contentText` / `contentHtml` | 发信、详情、Internal Tool 均使用该命名 |
| 待确认写操作 | `pendingActions` | Agent 返回，后端保存，前端再确认 |

## 3. 调用链一：Frontend -> Backend Public API

### 3.1 注册和登录

```http
POST /api/v1/auth/register
Content-Type: application/json
```

```json
{
  "email": "demo@smail.com",
  "username": "Demo",
  "password": "123456"
}
```

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "demo@smail.com",
  "password": "123456"
}
```

登录成功后前端保存 `data.token`，后续请求加 `Authorization: Bearer <jwt>`。

### 3.2 Workspace 主体验

前端主体验使用 Workspace API，不直接拼旧式 mailbox CRUD。

```http
GET /api/v1/workspace/views
GET /api/v1/workspace/mail-items?view=inbox&keyword=项目&page=1&pageSize=20
GET /api/v1/workspace/mail-items/{itemId}
```

邮件列表核心字段：

```json
{
  "itemId": 101,
  "mailId": 88,
  "folder": "INBOX",
  "senderEmail": "teacher@smail.com",
  "subject": "项目阶段汇报提醒",
  "summaryPreview": "请在明天下午前提交项目进度并准备演示。",
  "category": { "id": 1, "name": "课程", "color": "#4f46e5" },
  "analysisStatus": "SUCCEEDED",
  "riskLevel": "LOW",
  "read": false,
  "starred": false,
  "priority": "HIGH",
  "priorityScore": 86,
  "hasAttachment": true,
  "receivedAt": "2026-06-10T10:30:00"
}
```

### 3.3 发信和附件

```http
POST /api/v1/compose/attachments
Content-Type: multipart/form-data
```

返回 `pendingAttachmentId` 后，发送邮件时绑定：

```http
POST /api/v1/mails/send
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "to": ["alice@smail.com"],
  "cc": [],
  "bcc": [],
  "subject": "SmartMail MVP 联调",
  "contentText": "这是一封测试邮件，请确认能收到并看到 AI 分析状态。",
  "contentHtml": null,
  "pendingAttachmentIds": [123],
  "parentMailId": null
}
```

### 3.4 交互式 Agent

创建全局 Agent 会话：

```http
POST /api/v1/agent/sessions
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "scope": "GLOBAL",
  "context": {}
}
```

创建当前邮件 Agent 会话：

```json
{
  "scope": "CURRENT_MAIL",
  "context": {
    "mailItemId": 101
  }
}
```

响应 `data`：

```json
{
  "sessionId": "as_123",
  "scope": "CURRENT_MAIL",
  "context": { "mailItemId": 101 },
  "status": "ACTIVE",
  "messages": [],
  "pendingActions": [],
  "createdAt": "2026-06-10T10:35:00",
  "updatedAt": "2026-06-10T10:35:00"
}
```

发送用户消息。请求字段是 `message`：

```http
POST /api/v1/agent/sessions/{sessionId}/messages
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "message": "这封邮件需要我做什么？"
}
```

响应 `data`：

```json
{
  "sessionId": "as_123",
  "status": "SUCCEEDED",
  "answer": "这封邮件要求你在明天下午前提交项目进度，并准备 5 分钟演示。",
  "assistantMessage": {
    "role": "ASSISTANT",
    "content": "这封邮件要求你在明天下午前提交项目进度，并准备 5 分钟演示。",
    "status": "SUCCEEDED",
    "toolCalls": [
      {
        "tool": "mail_context_tool",
        "status": "SUCCEEDED"
      }
    ],
    "createdAt": "2026-06-10T10:36:00"
  },
  "pendingActions": []
}
```

确认待执行动作：

```http
POST /api/v1/agent/actions/{actionId}/confirm
Authorization: Bearer <jwt>
Content-Type: application/json
```

```json
{
  "confirmed": true
}
```

`confirmed=false` 表示取消动作。

## 4. 调用链二：Backend -> Agent Plugin API

后端调用 Python Agent 的接口只在后端内部使用，前端不要调用。

### 4.1 健康检查

```http
GET /plugin/v1/health
X-Plugin-Token: smartmail-agent-plugin-dev-token
```

```json
{
  "status": "UP",
  "pluginVersion": "0.1.0",
  "capabilities": {
    "rules": true,
    "llm": false,
    "currentMailAgent": true,
    "ragTool": "MOCK"
  }
}
```

### 4.2 自动分析

```http
POST /plugin/v1/analysis/mail
X-Plugin-Token: smartmail-agent-plugin-dev-token
Content-Type: application/json
```

```json
{
  "taskId": 1001,
  "userId": 1,
  "mailItemId": 101,
  "mail": {
    "mailId": 88,
    "senderEmail": "teacher@smail.com",
    "senderDisplayName": "Teacher",
    "recipients": ["demo@smail.com"],
    "subject": "项目阶段汇报提醒",
    "contentText": "请各组在明天下午前提交项目进度，并准备 5 分钟演示。",
    "contentHtml": null,
    "attachments": [
      {
        "fileName": "requirements.pdf",
        "mimeType": "application/pdf",
        "fileSize": 204800
      }
    ],
    "sentAt": "2026-06-10T10:30:00"
  },
  "userCategories": [
    { "id": 1, "name": "课程" },
    { "id": 2, "name": "Other" },
    { "id": 3, "name": "Junk Mail" }
  ],
  "behaviorSignals": {
    "frequentSenders": ["teacher@smail.com"],
    "recentRepliedSenders": ["teacher@smail.com"],
    "recentMarkedJunkSenders": []
  },
  "pluginConfig": {
    "aiPluginEnabled": true,
    "provider": "RULES",
    "llmEnabled": false,
    "ragEnabled": false
  }
}
```

成功响应：

```json
{
  "status": "SUCCEEDED",
  "summary": [
    "明天下午前提交项目进度。",
    "准备 5 分钟阶段演示。"
  ],
  "category": {
    "id": 1,
    "name": "课程"
  },
  "junk": false,
  "priority": "HIGH",
  "priorityScore": 86,
  "riskLevel": "LOW",
  "riskHints": [
    "邮件包含明确截止时间，请优先处理。"
  ],
  "modelInfo": {
    "provider": "RULES",
    "model": "rule-engine-v1"
  }
}
```

后端处理规则：

- `SUCCEEDED`：写入分析结果。
- `PARTIAL`：写入可用字段并记录失败原因。
- `FAILED`：分析任务标记失败，邮件仍正常可见。
- `DISABLED`：不展示 AI 结果，基础邮箱功能正常。

### 4.3 交互聊天

```http
POST /plugin/v1/agent/chat
X-Plugin-Token: smartmail-agent-plugin-dev-token
Content-Type: application/json
```

```json
{
  "sessionId": "as_123",
  "userId": 1,
  "scope": "CURRENT_MAIL",
  "message": "这封邮件需要我做什么？",
  "context": {
    "mailItemId": 101
  },
  "toolPolicy": {
    "readToolsAutoAllowed": true,
    "writeToolsRequireConfirmation": true,
    "agentAutoWriteEnabled": false
  },
  "pluginConfig": {
    "aiPluginEnabled": true,
    "provider": "DEEPSEEK",
    "llmEnabled": true,
    "ragEnabled": false
  }
}
```

Agent 可以返回待确认动作，但不得直接改库：

```json
{
  "status": "SUCCEEDED",
  "answer": "我可以把这封邮件标记为重要，并分类到课程。",
  "toolCalls": [
    {
      "tool": "mail_context_tool",
      "status": "SUCCEEDED"
    }
  ],
  "pendingActions": [
    {
      "type": "SET_PRIORITY",
      "label": "标记为 HIGH",
      "payload": {
        "mailItemId": 101,
        "priority": "HIGH"
      }
    },
    {
      "type": "SET_CATEGORY",
      "label": "分类到课程",
      "payload": {
        "mailItemId": 101,
        "categoryId": 1
      }
    }
  ]
}
```

## 5. 调用链三：Agent -> Backend Internal Tool API

Internal Tool API 只给 Agent 使用，必须携带 `X-Internal-Token`。

### 5.1 读取当前邮件上下文

```http
GET /internal/v1/tools/mail-items/{itemId}/context?userId=1
X-Internal-Token: smartmail-internal-dev-token
```

响应 `data`：

```json
{
  "itemId": 101,
  "mailId": 88,
  "userId": 1,
  "folder": "INBOX",
  "senderEmail": "teacher@smail.com",
  "subject": "项目阶段汇报提醒",
  "contentText": "请各组在明天下午前提交项目进度...",
  "contentHtml": null,
  "priority": "HIGH",
  "recipients": ["demo@smail.com"]
}
```

### 5.2 搜索用户邮件集合

```http
GET /internal/v1/tools/mail-search?userId=1&keyword=项目&limit=5
X-Internal-Token: smartmail-internal-dev-token
```

可选参数：

| 参数 | 说明 |
| --- | --- |
| `userId` | 必填，当前用户 ID |
| `keyword` | 可选，检索词 |
| `folder` | 可选，限定 `INBOX`、`SENT`、`TRASH`、`JUNK` 等 |
| `limit` | 可选，默认 10 |

### 5.3 写回分析结果

后端当前保存接口使用 `SaveAiResultRequest`：

```http
POST /internal/v1/tools/analysis-results
X-Internal-Token: smartmail-internal-dev-token
Content-Type: application/json
```

```json
{
  "mailId": 88,
  "userId": 1,
  "resultType": "ANALYSIS",
  "resultJson": "{\"summary\":[\"明天下午前提交项目进度\"],\"priority\":\"HIGH\",\"junk\":false}",
  "status": "SUCCEEDED"
}
```

`/internal/v1/tools/ai-results` 是兼容旧路径；新联调优先使用 `/analysis-results`。

### 5.4 执行已确认写操作

```http
POST /internal/v1/tools/mail-actions/execute
X-Internal-Token: smartmail-internal-dev-token
Content-Type: application/json
```

```json
{
  "userId": 1,
  "mailItemId": 101,
  "action": "MOVE",
  "folder": "TRASH"
}
```

后端兼容 `itemId` 和 `mailItemId`，但 Agent 侧统一发 `mailItemId`。

写操作白名单：

- `MARK_READ`
- `MARK_UNREAD`
- `SET_READ`
- `STAR`
- `UNSTAR`
- `MOVE_TO_JUNK`
- `MOVE`，`folder` 只能是 `INBOX`、`JUNK`、`TRASH`
- `SET_PRIORITY`，值只能是 `LOW`、`NORMAL`、`HIGH`、`URGENT`
- `SET_CATEGORY`

## 6. 本地 Demo 启动步骤

建议开三个终端，按 Backend -> Agent -> Frontend 顺序启动。

### 6.1 启动 Backend

```powershell
cd backend
$env:SMARTMAIL_AI_ENABLED = "true"
$env:SMARTMAIL_AGENT_BASE_URL = "http://127.0.0.1:8000"
$env:SMARTMAIL_AGENT_PLUGIN_TOKEN = "smartmail-agent-plugin-dev-token"
mvn spring-boot:run
```

默认地址：`http://localhost:8080`。

### 6.2 启动 Agent

```powershell
cd agent
pip install -r requirements.txt
$env:SMARTMAIL_BACKEND_BASE_URL = "http://localhost:8080"
$env:SMARTMAIL_INTERNAL_TOKEN = "smartmail-internal-dev-token"
$env:SMARTMAIL_PLUGIN_TOKEN = "smartmail-agent-plugin-dev-token"
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

可选 DeepSeek 配置：

```powershell
Copy-Item config/providers.example.toml config/providers.toml
$env:SMARTMAIL_AGENT_CONFIG = "./config/providers.toml"
$env:DEEPSEEK_API_KEY = "sk-..."
```

不配置 API Key 时，Agent 应使用规则或 mock fallback，基础联调仍可继续。

### 6.3 启动 Frontend

```powershell
cd frontend
npm install
npm run dev
```

默认地址：`http://127.0.0.1:5173`。

### 6.4 联调顺序

1. 注册两个用户：`demo@smail.com`、`alice@smail.com`，密码可用 `123456`。
2. 使用 `demo@smail.com` 登录并发送邮件给 `alice@smail.com`。
3. 登录 `alice@smail.com`，打开 Inbox，确认新邮件出现。
4. 打开邮件详情，确认 `analysisStatus` 能从 `PENDING` 变为 `SUCCEEDED` 或可展示 fallback 结果。
5. 从 Mail Detail Drawer 创建 `CURRENT_MAIL` Agent 会话，发送测试提示词。
6. 从左侧 Global Agent 创建 `GLOBAL` 会话，测试 mock RAG / 搜索能力。
7. 如果返回 `pendingActions`，在前端确认后检查邮件列表状态变化。

## 7. 测试提示词

当前邮件 Agent：

- `这封邮件需要我做什么？`
- `帮我总结这封邮件的三条要点。`
- `这封邮件有没有风险？需要注意哪些链接或附件？`
- `把这封邮件标记为重要。`
- `把这封邮件移动到 Junk。`
- `把这封邮件分类到课程。`

全局 Agent：

- `帮我搜索项目阶段汇报相关邮件。`
- `最近有哪些高优先级邮件？`
- `帮我找老师发来的邮件。`
- `帮我把广告邮件移入回收站。`

用于触发规则分析的邮件正文示例：

```text
请在明天下午 5 点前提交 SmartMail 项目阶段汇报，并准备 5 分钟演示。附件是 requirements.pdf。
```

用于触发风险提示的邮件正文示例：

```text
你的账号需要立即验证，请点击 http://example-risk.test/login 输入验证码，否则邮箱将被停用。
```

## 8. AI 关闭时的预期

当 `SMARTMAIL_AI_ENABLED=false` 或用户设置 `aiEnabled=false`：

- 后端不调用 Python Agent。
- 自动分析任务应标记为 `DISABLED` 或不创建。
- 公开 Agent message API 返回 `DISABLED`。
- 注册、登录、收发、附件、搜索、同步、通知等基础邮件能力保持可用。

## 9. 联调检查清单

- 前端 Network 中只有 `/api/v1/**`，没有直接请求 `:8000/plugin/v1/**`。
- 后端日志能看到调用 `/plugin/v1/health`、`/plugin/v1/analysis/mail` 或 `/plugin/v1/agent/chat`。
- Agent 日志能看到调用 `/internal/v1/tools/mail-items/{itemId}/context` 或 `/internal/v1/tools/mail-search`。
- 所有示例邮箱均为 `@smail.com`。
- Agent 消息请求字段为 `message`。
- 当前邮件上下文为 `context.mailItemId`。
- 写操作先返回 `pendingActions`，用户确认后才改变邮件状态。
