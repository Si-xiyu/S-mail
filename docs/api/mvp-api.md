# SmartMail MVP API 草案

本文记录目标 MVP 的接口契约。主原则是：前端主体验调用 Workspace View API，不直接拼数据库表。

## 1. 通用约定

### 1.1 响应格式

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

### 1.2 分页格式

```json
{
  "records": [],
  "total": 100,
  "page": 1,
  "pageSize": 20
}
```

### 1.3 认证

登录后前端通过请求头携带：

```text
Authorization: Bearer <token>
```

## 2. Auth API

### POST `/api/v1/auth/register`

```json
{
  "email": "demo@smartmail.local",
  "username": "Demo",
  "password": "123456"
}
```

### POST `/api/v1/auth/login`

```json
{
  "email": "demo@smartmail.local",
  "password": "123456"
}
```

响应：

```json
{
  "token": "jwt-token",
  "userId": 1,
  "email": "demo@smartmail.local",
  "username": "Demo"
}
```

### GET `/api/v1/users/me`

返回当前用户和设置摘要。

## 3. Workspace API

### GET `/api/v1/workspace/views`

返回左侧工作区导航需要的数据。

```json
{
  "views": [
    { "key": "today", "label": "Today", "count": 3 },
    { "key": "important", "label": "Important", "count": 2 },
    { "key": "unread", "label": "Unread", "count": 8 },
    { "key": "junk", "label": "Junk", "count": 1 }
  ],
  "folders": [
    { "key": "inbox", "label": "Inbox", "count": 12 },
    { "key": "sent", "label": "Sent", "count": 5 },
    { "key": "drafts", "label": "Drafts", "count": 1 },
    { "key": "trash", "label": "Trash", "count": 0 }
  ],
  "categories": [
    { "id": 1, "name": "课程", "color": "#4f46e5" },
    { "id": 2, "name": "Other", "color": "#64748b" },
    { "id": 3, "name": "Junk Mail", "color": "#ef4444" }
  ]
}
```

### GET `/api/v1/workspace/mail-items`

查询中栏邮件列表。

参数：

| 参数 | 说明 |
| --- | --- |
| `view` | `inbox`、`sent`、`drafts`、`trash`、`junk`、`today`、`important`、`unread` |
| `categoryId` | 可选，按用户类别过滤 |
| `keyword` | 可选，关键词 |
| `page` | 页码 |
| `pageSize` | 每页数量 |

响应记录：

```json
{
  "itemId": 101,
  "mailId": 88,
  "folder": "INBOX",
  "senderEmail": "teacher@smartmail.local",
  "subject": "项目阶段汇报提醒",
  "summaryPreview": "请在明天下午前提交项目进度并准备演示。",
  "category": { "id": 1, "name": "课程", "color": "#4f46e5" },
  "analysisStatus": "SUCCEEDED",
  "read": false,
  "starred": false,
  "priority": "HIGH",
  "hasAttachment": true,
  "receivedAt": "2026-06-10T10:30:00"
}
```

### GET `/api/v1/workspace/mail-items/{itemId}`

返回右侧 Mail Detail Drawer 数据。

```json
{
  "itemId": 101,
  "mailId": 88,
  "folder": "INBOX",
  "senderEmail": "teacher@smartmail.local",
  "recipients": ["demo@smartmail.local"],
  "subject": "项目阶段汇报提醒",
  "contentText": "请各组在明天下午前提交项目进度...",
  "contentHtml": null,
  "read": false,
  "starred": false,
  "priority": "HIGH",
  "analysis": {
    "status": "SUCCEEDED",
    "summary": ["明天下午前提交项目进度", "准备 5 分钟阶段演示"],
    "category": { "id": 1, "name": "课程", "color": "#4f46e5" },
    "junk": false,
    "riskHints": []
  },
  "attachments": [
    {
      "id": 5001,
      "fileName": "requirements.pdf",
      "mimeType": "application/pdf",
      "fileSize": 204800,
      "downloadUrl": "/api/v1/attachments/5001/download"
    }
  ],
  "agent": {
    "currentMailAgentAvailable": true
  }
}
```

## 4. Compose 与附件 API

### POST `/api/v1/compose/attachments`

选择文件后立即上传为 Pending Attachment。

请求：`multipart/form-data`

响应：

```json
{
  "pendingAttachmentId": "pa_123",
  "fileName": "report.docx",
  "mimeType": "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
  "fileSize": 102400,
  "status": "UPLOADED"
}
```

### DELETE `/api/v1/compose/attachments/{pendingAttachmentId}`

移除未绑定附件。

### POST `/api/v1/mails/send`

发送邮件并绑定 Pending Attachment。

```json
{
  "to": ["alice@smartmail.local"],
  "cc": [],
  "bcc": [],
  "subject": "SmartMail MVP 联调",
  "contentText": "这是一封测试邮件。",
  "contentHtml": null,
  "pendingAttachmentIds": ["pa_123"]
}
```

响应：

```json
{
  "mailId": 88,
  "messageNo": "SM-20260610-0001",
  "delivery": {
    "delivered": ["alice@smartmail.local"],
    "failed": []
  }
}
```

## 5. Mailbox 操作 API

### PATCH `/api/v1/mailbox/items/{itemId}/read`

```json
{
  "read": true
}
```

### PATCH `/api/v1/mailbox/items/{itemId}/star`

```json
{
  "starred": true
}
```

### PATCH `/api/v1/mailbox/items/{itemId}/category`

```json
{
  "categoryId": 1
}
```

### POST `/api/v1/mailbox/items/{itemId}/move`

用于移入 Junk 或恢复到 Inbox。

```json
{
  "folder": "JUNK"
}
```

### DELETE `/api/v1/mailbox/items/{itemId}`

第一次删除移入 Trash；Trash 中再次删除则对当前用户隐藏。

## 6. Category API

### GET `/api/v1/categories`

返回当前用户类别。

### POST `/api/v1/categories`

```json
{
  "name": "课程",
  "color": "#4f46e5"
}
```

### PATCH `/api/v1/categories/{categoryId}`

更新名称或颜色。

### DELETE `/api/v1/categories/{categoryId}`

删除用户类别。被删除类别下的邮件回到 Other。

## 7. Agent API

### POST `/api/v1/agent/sessions`

创建交互式 Agent 会话。

当前邮件 Agent：

```json
{
  "scope": "CURRENT_MAIL",
  "itemId": 101
}
```

全局 Agent：

```json
{
  "scope": "GLOBAL"
}
```

响应：

```json
{
  "sessionId": "as_123",
  "scope": "CURRENT_MAIL"
}
```

### POST `/api/v1/agent/sessions/{sessionId}/messages`

```json
{
  "content": "帮我总结这封邮件需要我做什么"
}
```

响应：

```json
{
  "messageId": "msg_456",
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

写操作默认返回待确认动作：

```json
{
  "answer": "我可以把这封邮件移动到 Junk。",
  "pendingActions": [
    {
      "actionId": "act_789",
      "type": "MOVE_TO_JUNK",
      "label": "移入 Junk"
    }
  ]
}
```

### POST `/api/v1/agent/actions/{actionId}/confirm`

用户确认 Agent 写操作。

## 8. AI Analysis API

前端一般不直接触发自动分析，但需要能查看状态或手动重试。

### POST `/api/v1/analysis/mail-items/{itemId}/retry`

重新排队分析当前邮件。

响应：

```json
{
  "analysisStatus": "PENDING"
}
```

## 9. Internal Tool API

仅 Agent 调用，需要内部 token：

```text
X-Internal-Token: smartmail-internal-dev-token
```

### GET `/internal/v1/tools/mail-items/{itemId}/context`

读取当前邮件上下文。

### GET `/internal/v1/tools/mail-search`

RAG Tool 或 mock RAG Tool 使用。MVP 可返回演示数据。

参数：

| 参数 | 说明 |
| --- | --- |
| `userId` | 用户 ID |
| `query` | 检索问题 |
| `limit` | 数量 |

### POST `/internal/v1/tools/analysis-results`

自动分析管道写回摘要、分类、Junk、状态。

### POST `/internal/v1/tools/mail-actions`

交互式 Agent 的写操作执行入口。只有用户确认后，后端才应调用实际写操作。

## 10. 状态枚举

### `analysisStatus`

- `PENDING`
- `SUCCEEDED`
- `FAILED`
- `DISABLED`

### `folder`

- `INBOX`
- `SENT`
- `DRAFTS`
- `TRASH`
- `JUNK`

### `priority`

- `LOW`
- `NORMAL`
- `HIGH`
- `URGENT`

### `agent scope`

- `CURRENT_MAIL`
- `GLOBAL`
