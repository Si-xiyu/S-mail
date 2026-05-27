# SmartMail MVP API

本文件记录第一轮 MVP 的前后端与 Agent 接口契约。

## 通用响应

后端统一返回：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

## Auth

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

响应中的 `token` 后续通过 `Authorization: Bearer <token>` 携带。

## Mail

### POST `/api/v1/mails`

发送邮件。

```json
{
  "to": ["alice@smartmail.local"],
  "cc": [],
  "subject": "SmartMail MVP 联调",
  "contentText": "测试邮件正文",
  "contentHtml": null
}
```

### GET `/api/v1/mailbox?folder=INBOX&page=1&pageSize=20`

拉取当前用户邮箱列表。

### GET `/api/v1/mails/{mailId}`

获取邮件详情。

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

### DELETE `/api/v1/mailbox/items/{itemId}`

首次删除移入垃圾箱；垃圾箱中再次删除会软删除。

## Backend AI Facade

前端只调用 Spring Boot，不直接调用 Agent。

### POST `/api/v1/ai/mails/{mailId}/summary`

生成摘要。

### POST `/api/v1/ai/mails/{mailId}/reply-draft`

生成回复草稿。

### POST `/api/v1/ai/mails/{mailId}/analyze`

分析优先级、标签和垃圾邮件风险。

## Agent API

后端调用 Agent：

### POST `http://localhost:8000/api/v1/agent/tasks`

```json
{
  "mailId": 1,
  "userId": 1,
  "task": "summary"
}
```

`task` 可选：

- `summary`
- `reply_draft`
- `analyze`

## Internal Tool API

仅 Agent 调用，需要请求头：

```text
X-Internal-Token: smartmail-internal-dev-token
```

### GET `/internal/v1/tools/mails/{mailId}?userId=1`

获取邮件上下文。

### POST `/internal/v1/tools/ai-results`

保存 AI 结果。

```json
{
  "mailId": 1,
  "userId": 1,
  "resultType": "SUMMARY",
  "resultJson": "{\"summary\":[\"要点\"]}",
  "status": "SUCCESS"
}
```

### POST `/internal/v1/tools/mails/{mailId}/priority`

更新当前用户视角下的邮件优先级。

```json
{
  "userId": 1,
  "priority": "HIGH"
}
```
