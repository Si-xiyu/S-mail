# SmartMail 前端 API 文档

本文记录前端工作台应调用的后端公开 API。Agent 三方联调以 [agent-integration.md](./agent-integration.md) 为主；本文只覆盖 Frontend -> Backend Public API。

## 基础约定

- Base URL：开发环境通过 Vite 代理访问 `/api/v1`。
- 认证方式：`Authorization: Bearer <token>`。
- Token 存储：`localStorage` 中的 `smartmail_token`。
- 邮箱后缀：所有注册、登录、发信示例统一使用 `@smail.com`。
- 前端不得直接调用 Agent 的 `http://127.0.0.1:8000/plugin/v1/**`。

后端统一响应：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## Auth API

### POST `/api/v1/auth/register`

```json
{
  "email": "demo@smail.com",
  "username": "Demo",
  "password": "123456"
}
```

### POST `/api/v1/auth/login`

```json
{
  "email": "demo@smail.com",
  "password": "123456"
}
```

响应 `data`：

```json
{
  "token": "jwt-token",
  "userId": 1,
  "email": "demo@smail.com",
  "username": "Demo"
}
```

## Workspace API

前端主体验应优先使用 Workspace API。

```http
GET /api/v1/workspace/views
GET /api/v1/workspace/mail-items?view=inbox&keyword=项目&page=1&pageSize=20
GET /api/v1/workspace/mail-items/{itemId}
```

列表项核心字段：

```typescript
interface WorkspaceMailItem {
  itemId: number
  mailId: number
  folder: 'INBOX' | 'SENT' | 'DRAFTS' | 'TRASH' | 'JUNK'
  senderEmail: string
  subject: string
  summaryPreview?: string
  category?: { id: number; name: string; color?: string }
  analysisStatus?: 'PENDING' | 'SUCCEEDED' | 'FAILED' | 'DISABLED'
  riskLevel?: 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH'
  read: boolean
  starred: boolean
  priority: 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
  priorityScore?: number
  hasAttachment: boolean
  receivedAt: string
}
```

详情接口使用 `itemId`，不是 `mailId`：

```http
GET /api/v1/workspace/mail-items/101
```

## Compose 与邮件发送

### POST `/api/v1/compose/attachments`

请求：`multipart/form-data`。响应 `data.pendingAttachmentId`。

### POST `/api/v1/mails/send`

```json
{
  "to": ["alice@smail.com"],
  "cc": [],
  "bcc": [],
  "subject": "SmartMail MVP 联调",
  "contentText": "这是一封测试邮件。",
  "contentHtml": null,
  "pendingAttachmentIds": [],
  "parentMailId": null
}
```

## Mailbox 操作

```http
PATCH  /api/v1/mailbox/items/{itemId}/read
PATCH  /api/v1/mailbox/items/{itemId}/star
PATCH  /api/v1/mailbox/items/{itemId}/category
POST   /api/v1/mailbox/items/{itemId}/move
DELETE /api/v1/mailbox/items/{itemId}
```

示例：

```json
{ "read": true }
```

```json
{ "folder": "JUNK" }
```

## Agent Public API

前端只调用后端公开 Agent API。

创建当前邮件会话：

```json
{
  "scope": "CURRENT_MAIL",
  "context": { "mailItemId": 101 }
}
```

创建全局会话：

```json
{
  "scope": "GLOBAL",
  "context": {}
}
```

发送消息时字段名是 `message`：

```json
{
  "message": "这封邮件需要我做什么？"
}
```

确认写操作：

```http
POST /api/v1/agent/actions/{actionId}/confirm
```

```json
{ "confirmed": true }
```

## Settings 与 AI 状态

```http
GET   /api/v1/users/me
PATCH /api/v1/users/me/settings
POST  /api/v1/analysis/mail-items/{itemId}/retry
```

AI 关闭时，基础邮件能力仍应可用；前端应隐藏或弱化摘要、分类建议和 Agent 入口。

## 相关文档

- [agent-integration.md](./agent-integration.md)：三方联调主文档。
- [mvp-api.md](./mvp-api.md)：完整 API 草案。
- [agent-plugin-api.md](./agent-plugin-api.md)：后端与 Agent Plugin 契约。
