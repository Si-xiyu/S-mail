# SmartMail Agent Plugin API

本文定义 SmartMail 后端与 Agent Plugin 之间的接口契约。该契约用于保持前端、后端、Agent 三方解耦：前端只调用后端公开 API，后端负责业务与权限，Agent Plugin 只通过 API 提供智能能力。

## 1. 架构边界

```text
Frontend
  -> Backend Public API
      -> Database / File Storage
      -> Agent Plugin API
          -> Backend Internal Tool API
```

边界规则：

- 前端不直接调用 Agent Plugin。
- Agent Plugin 不直接访问数据库或附件文件。
- 后端是唯一的业务权限中心。
- AI Plugin 关闭时，后端不调用 Agent Plugin，基础邮件系统保持可用。
- Agent Plugin 失败时，邮件收发、同步、附件、搜索不受影响。

## 2. 服务角色

### Backend

负责：

- 用户鉴权。
- 邮件权限校验。
- 邮件、附件、分类、行为事件、AI 结果持久化。
- 创建自动分析任务。
- 调用 Agent Plugin。
- 提供 Internal Tool API。

### Agent Plugin

负责：

- 自动摘要。
- Junk 判断。
- 优先级预测。
- 安全风险提示。
- 当前邮件 Agent 问答。
- 全局 Agent 的 RAG Tool 边界，MVP 可 mock。

Agent Plugin 可以用规则引擎、DeepSeek Provider、Ollama、本地模型或未来 DLL Adapter 实现，但不得改变对外 API 契约。

## 3. 通用约定

### 3.1 Base URL

开发默认：

```text
http://127.0.0.1:8000
```

### 3.2 鉴权

后端调用 Agent Plugin 时携带内部 token：

```text
X-Plugin-Token: smartmail-agent-plugin-dev-token
```

Agent Plugin 调用后端 Internal Tool API 时携带：

```text
X-Internal-Token: smartmail-internal-dev-token
```

### 3.3 响应状态

Agent Plugin 返回的 `status`：

- `SUCCEEDED`
- `FAILED`
- `DISABLED`
- `PARTIAL`

`DISABLED` 表示 AI Plugin 关闭或该能力未启用。`PARTIAL` 表示部分工具失败，但仍有可展示结果。

## 4. Health API

### GET `/plugin/v1/health`

用于后端检查 Agent Plugin 是否可用。

响应：

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

## 5. 自动分析 API

### POST `/plugin/v1/analysis/mail`

后端在邮件投递后调用，用于生成摘要、分类、Junk 判断、优先级和安全风险提示。

请求：

```json
{
  "taskId": 1001,
  "userId": 1,
  "mailItemId": 88,
  "mail": {
    "mailId": 77,
    "senderEmail": "teacher@smartmail.local",
    "senderDisplayName": "Teacher",
    "recipients": ["demo@smartmail.local"],
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
    { "id": 2, "name": "项目" },
    { "id": 3, "name": "Other" },
    { "id": 4, "name": "Junk Mail" }
  ],
  "behaviorSignals": {
    "frequentSenders": ["teacher@smartmail.local"],
    "recentRepliedSenders": ["teacher@smartmail.local"],
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

响应：

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

失败响应：

```json
{
  "status": "FAILED",
  "errorCode": "MODEL_TIMEOUT",
  "message": "LLM provider timeout",
  "fallbackAvailable": true
}
```

后端处理规则：

- `SUCCEEDED`：写入分析结果。
- `PARTIAL`：写入可用字段，并记录失败原因。
- `FAILED`：分析任务标记失败，邮件仍正常可见。
- `DISABLED`：不展示 AI 结果，基础邮箱功能正常。

## 6. 交互式 Agent API

### POST `/plugin/v1/agent/chat`

后端转发用户消息给 Agent Plugin。该接口可用于 Current-Mail Agent 和 Global Mail Agent。

请求：

```json
{
  "sessionId": "as_123",
  "userId": 1,
  "scope": "CURRENT_MAIL",
  "message": "这封邮件需要我做什么？",
  "context": {
    "mailItemId": 88
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

响应：

```json
{
  "status": "SUCCEEDED",
  "answer": "这封邮件要求你在明天下午前提交项目进度，并准备 5 分钟演示。",
  "toolCalls": [
    {
      "tool": "mail_context_tool",
      "status": "SUCCEEDED"
    }
  ],
  "pendingActions": []
}
```

带待确认写操作的响应：

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
        "mailItemId": 88,
        "priority": "HIGH"
      }
    },
    {
      "type": "SET_CATEGORY",
      "label": "分类到课程",
      "payload": {
        "mailItemId": 88,
        "categoryId": 1
      }
    }
  ]
}
```

后端处理规则：

- `pendingActions` 不由 Agent Plugin 直接执行。
- 后端保存 pending action。
- 用户确认后，后端调用自己的业务服务执行。
- 如果用户开启 `agentAutoWriteEnabled`，后端仍应按白名单执行，不允许任意操作。

## 7. RAG Tool API 边界

MVP 中 RAG Tool 可以 mock，但工具边界需要保留。

Agent Plugin 内部调用后端：

### GET `/internal/v1/tools/mail-search`

参数：

| 参数 | 说明 |
| --- | --- |
| `userId` | 用户 ID |
| `query` | 用户问题或检索词 |
| `limit` | 返回数量 |

MVP mock 响应：

```json
{
  "records": [
    {
      "mailItemId": 88,
      "subject": "项目阶段汇报提醒",
      "snippet": "请各组在明天下午前提交项目进度...",
      "score": 1.0,
      "source": "MOCK"
    }
  ]
}
```

MVP 后真实实现：

- BM25 召回。
- 向量召回。
- RRF 融合排序。
- 向量失败时降级 BM25。

## 8. Backend Internal Tool API

Agent Plugin 只能通过这些工具访问后端能力。

### GET `/internal/v1/tools/mail-items/{itemId}/context`

获取当前邮件上下文。后端必须校验 `userId` 是否有权限查看。

### GET `/internal/v1/tools/mail-search`

检索用户邮件集合。MVP 可 mock。

### POST `/internal/v1/tools/analysis-results`

自动分析管道写回结构化结果。

### POST `/internal/v1/tools/mail-actions`

执行用户已确认的写操作。Agent Plugin 不应绕过后端业务服务直接改数据。

## 9. Plugin Disabled 行为

当 AI Plugin 关闭：

- 后端不应创建自动分析任务，或创建后直接标记 `DISABLED`。
- 后端不调用 `/plugin/v1/analysis/mail`。
- 前端隐藏或弱化 AI 摘要、分类建议、Agent 入口。
- 邮件注册、登录、收发、附件、搜索、同步、通知都正常工作。

如果前端仍尝试打开 Agent，后端返回：

```json
{
  "status": "DISABLED",
  "message": "AI Plugin is disabled"
}
```

## 10. Mock 优先策略

MVP 允许 Agent Plugin 先用 mock 或规则版实现：

- 自动摘要：取正文前几句。
- 分类：关键词匹配用户类别，无法匹配则 Other。
- Junk：垃圾关键词、异常链接、可疑发件人。
- 优先级：截止时间、老师/项目关键词、用户行为信号。
- RAG Tool：返回固定演示结果。

该策略用于保证前后端先完成集成，再逐步替换为 DeepSeek、Ollama 和真实检索。
