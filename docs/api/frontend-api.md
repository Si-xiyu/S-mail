# SmartMail 前端 API 文档

> 本文档记录前端（Vue 3）所需的后端接口规范

## 基础配置

- **Base URL**: 根据环境设置，默认 `/api/v1`
- **认证方式**: JWT Bearer Token
- **超时时间**: 5000ms
- **Token 存储**: localStorage 中的 `smartmail_token`

## 认证模块 (`/auth`)

### 用户登录

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**返回示例**:
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "token": "eyJhbGc...",
    "userId": 1,
    "email": "user@example.com",
    "username": "John Doe"
  }
}
```

### 用户注册

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "newuser@example.com",
  "username": "New User",
  "password": "password123"
}
```

**返回示例**:
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "token": "eyJhbGc...",
    "userId": 2,
    "email": "newuser@example.com",
    "username": "New User"
  }
}
```

## 邮箱模块 (`/mailbox`)

### 获取邮箱列表

```http
GET /api/v1/mailbox?folder=INBOX
Authorization: Bearer {token}
```

**Query 参数**:
- `folder` (string): 文件夹类型，可选值：`INBOX`, `SENT`, `DRAFTS`, `TRASH`, `SPAM`, `STARRED`

**返回示例**:
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "records": [
      {
        "itemId": 1,
        "mailId": 1,
        "senderEmail": "sender@example.com",
        "subject": "邮件主题",
        "preview": "邮件预览内容...",
        "folder": "INBOX",
        "read": false,
        "starred": true,
        "priority": "HIGH",
        "hasAttachment": true,
        "receivedAt": "2026-06-13T10:30:00Z"
      }
    ]
  }
}
```

## 邮件模块 (`/mails`)

### 获取邮件详情

```http
GET /api/v1/mails/{mailId}
Authorization: Bearer {token}
```

**返回示例**:
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "itemId": 1,
    "mailId": 1,
    "messageNo": "MSG-20260613-001",
    "senderEmail": "sender@example.com",
    "subject": "邮件主题",
    "preview": "邮件预览...",
    "contentText": "完整邮件文本内容...",
    "contentHtml": "<p>HTML 格式内容</p>",
    "folder": "INBOX",
    "read": true,
    "starred": false,
    "priority": "NORMAL",
    "recipients": ["recipient@example.com"],
    "hasAttachment": true,
    "receivedAt": "2026-06-13T10:30:00Z",
    "aiResults": []
  }
}
```

### 发送邮件

```http
POST /api/v1/mails
Authorization: Bearer {token}
Content-Type: application/json

{
  "to": ["recipient@example.com"],
  "cc": ["cc@example.com"],
  "subject": "邮件主题",
  "contentText": "邮件正文（纯文本）",
  "contentHtml": "<p>邮件正文（HTML）</p>"
}
```

**返回示例**:
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "mailId": 123,
    "messageNo": "MSG-20260613-123"
  }
}
```

## AI 模块 (`/ai`)

### 运行 AI 任务

```http
POST /api/v1/ai/mails/{mailId}/{task}
Authorization: Bearer {token}
```

**Path 参数**:
- `mailId` (number): 邮件 ID
- `task` (string): 任务类型，可选值：
  - `summary` - 生成邮件摘要
  - `reply-draft` - 生成回复草稿
  - `analyze` - 分析邮件优先级和标签建议

**返回示例**:
```json
{
  "code": 0,
  "message": "Success",
  "data": {
    "task": "summary",
    "status": "COMPLETED",
    "result": {
      "summary": ["要点一", "要点二"],
      "priority": "HIGH",
      "labels": ["标签1", "标签2"]
    },
    "steps": [
      {
        "name": "Parse",
        "detail": "解析邮件内容"
      },
      {
        "name": "Generate",
        "detail": "生成 AI 结果"
      }
    ]
  }
}
```

## 前端数据类型定义

### UserProfile
```typescript
interface UserProfile {
  id: number
  email: string
  username: string
}
```

### MailboxItem
```typescript
interface MailboxItem {
  itemId: number
  mailId: number
  senderEmail: string
  subject: string
  preview: string
  folder: string
  read: boolean
  starred: boolean
  priority: string
  hasAttachment: boolean
  receivedAt: string
}
```

### MailDetail
```typescript
interface MailDetail extends MailboxItem {
  messageNo: string
  contentText: string
  contentHtml?: string
  recipients: string[]
  aiResults: Array<Record<string, unknown>>
}
```

### SendMailPayload
```typescript
interface SendMailPayload {
  to: string[]
  cc: string[]
  subject: string
  contentText: string
  contentHtml?: string
}
```

### AgentTaskResponse
```typescript
interface AgentTaskResponse {
  task: string
  status: string
  result: Record<string, unknown>
  steps?: Array<{ name: string; detail: string }>
}
```

## 错误处理

所有 API 响应遵循统一格式：

```json
{
  "code": 0,
  "message": "Success 或 Error Message",
  "data": {}
}
```

**错误码规约**:
- `0` - 成功
- `400` - 请求参数错误
- `401` - 未授权（无效或过期的 Token）
- `403` - 禁止访问
- `404` - 资源不存在
- `500` - 服务器内部错误

## 前端 Mock 策略

前端 API 客户端（`src/api/client.ts`）配备了降级方案：

- 开发阶段，若接口连接失败会返回 Mock 数据
- Mock 数据存储在客户端，可快速演示功能
- 后端启动后，自动切换到真实接口

## 相关文件

- 前端 API 客户端: `frontend/src/api/client.ts`
- 前端类型定义: `frontend/src/types/mail.ts`
- 前端存储: `frontend/src/stores/mailStore.ts`
