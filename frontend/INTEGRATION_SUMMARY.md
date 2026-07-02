# 前端与真实后端接口对接 - 修改总结

## 修改概述

本次修改完全移除了前端的 Mock 数据和降级机制，确保前端与真实后端 API 完全对接。所有修改都严格按照 `/docs/api/frontend-api.md` 和 `/docs/api/mvp-api.md` 的接口规范进行。

## 修改的文件

### 1. `src/types/mail.ts`
**目的**：更新 TypeScript 类型定义，与后端 DTO 完全对齐

**关键修改**：
- 新增 `Attachment` 接口，对应后端 `AttachmentResponse`
- 重新定义 `MailboxItem`，完全对应后端 `MailboxItemResponse`
- 重新定义 `MailDetail`，完全对应后端 `MailDetailResponse`
- 新增 `MailSendResponse` 接口，对应邮件发送响应
- 新增 `SendMailPayload`，包含 `pendingAttachmentIds` 和 `bcc` 字段
- 新增 `WorkspaceView`、`Category`、`WorkspaceViewResponse` 接口，为未来 Workspace API 准备

**字段映射**：
| 字段 | 后端类型 | 前端类型 | 说明 |
|------|--------|--------|------|
| `receivedAt` | `LocalDateTime` | `string` | ISO 8601 格式的时间戳 |
| `sentAt` | `LocalDateTime` | `string` (可选) | 邮件发送时间 |
| `attachments` | `List<AttachmentResponse>` | `Attachment[]` | 已发送邮件的附件列表 |

### 2. `src/types/index.ts`
**目的**：维持现有 Pinia Store 的向后兼容性

**关键修改**：
- 保留 `Mail`、`MailItem`、`User`、`Label`、`Attachment` 等旧接口
- 添加注释说明这些是为了维持兼容性
- 新代码优先使用 `mail.ts` 中的类型

### 3. `src/api/client.ts`
**目的**：移除所有 Mock 降级，正确调用真实后端 API

**关键修改**：
- **移除 Mock 数据**：删除了 `mockItems` 数组
- **移除 try-catch 降级**：
  - ❌ `listMailbox()` 不再返回 Mock 邮件
  - ❌ `getMailDetail()` 不再生成虚假的邮件详情
  - ❌ `sendMail()` 不再模拟延迟
  - ❌ `runAiTask()` 不再返回假数据
- **正确的错误处理**：所有函数都检查 `code !== 0`，并抛出错误
- **新增方法**：
  - `markMailRead()` - 标记已读/未读
  - `starMail()` - 标记星标
  - `deleteMail()` - 删除邮件
  - `moveMail()` - 移动到文件夹

**API 端点对应关系**：
| 函数名 | HTTP 方法 | 端点 | 说明 |
|-------|----------|------|------|
| `login()` | POST | `/auth/login` | 用户登录 |
| `register()` | POST | `/auth/register` | 用户注册 |
| `listMailbox()` | GET | `/mailbox` | 获取邮箱列表 |
| `getMailDetail()` | GET | `/mails/{mailId}` | 获取邮件详情 |
| `sendMail()` | POST | `/mails` | 发送邮件 |
| `markMailRead()` | PATCH | `/mailbox/items/{itemId}/read` | 标记已读 |
| `starMail()` | PATCH | `/mailbox/items/{itemId}/star` | 标记星标 |
| `deleteMail()` | DELETE | `/mailbox/items/{itemId}` | 删除邮件 |
| `moveMail()` | POST | `/mailbox/items/{itemId}/move` | 移动文件夹 |
| `runAiTask()` | POST | `/ai/mails/{mailId}/{task}` | 运行 AI 任务 |

### 4. `src/stores/mailStore.ts`
**目的**：完全改造为使用真实后端数据

**关键修改**：
- **核心状态改造**：
  - 移除 `mockMails` 和 `mails`（本地 Mock 数据）
  - 新增 `mailboxItems` - 存储从后端获取的真实 `MailboxItem[]`
  - 新增 `mailDetailCache` - 缓存已加载的邮件详情
  - 新增 `isLoading` 和 `error` - 加载状态和错误信息

- **新增核心方法**：
  - `login()` - 登录并自动加载邮箱
  - `register()` - 注册并自动加载邮箱
  - `loadMailbox()` - 从后端加载邮件列表
  - `getMailDetail()` - 从后端加载邮件详情
  - `sendMail()` - 发送邮件到后端
  - `markMailRead()` - 调用后端 API 标记已读
  - `starMail()` - 调用后端 API 标记星标
  - `deleteMail()` - 调用后端 API 删除邮件
  - `moveMail()` - 调用后端 API 移动邮件

- **向后兼容方法**：
  - 保留 `markAsRead()`、`toggleStar()` 等旧方法
  - 这些方法内部调用新的 API 方法
  - 确保现有组件继续工作

- **计算属性更新**：
  - `currentMailItems` - 从 `mailboxItems` 中筛选
  - `mailItems` - 转换为旧格式用于向后兼容

## API 接口对接规范

### 请求格式
```typescript
// Authorization Header
Authorization: Bearer {token}

// Base URL
/api/v1

// 示例：获取 INBOX 邮件列表
GET /api/v1/mailbox?folder=INBOX
```

### 响应格式（统一）
```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

**错误码**：
- `0` - 成功
- `400` - 请求参数错误
- `401` - 未授权（无效或过期的 Token）
- `403` - 禁止访问
- `404` - 资源不存在
- `500` - 服务器内部错误

### 时间格式
后端返回 ISO 8601 格式的时间戳，示例：`2026-06-13T10:30:00`

前端在 TypeScript 中定义为 `string` 类型，在需要时使用 `new Date(receivedAt)` 转换。

## 开发环境配置

### Vite 代理（已配置）
```javascript
// vite.config.ts
server: {
  proxy: {
    '/api': 'http://localhost:8080'
  }
}
```

### 前后端联调
1. 启动后端服务（Spring Boot）：`http://localhost:8080`
2. 启动前端开发服务器（Vite）：`npm run dev`
3. 前端自动代理 `/api` 请求到后端

### Token 存储
- 登录后，Token 自动存储在 `localStorage.smartmail_token`
- 所有后续请求自动在 `Authorization` header 中携带 Token

## 测试清单

### 认证相关
- [ ] 用户注册
- [ ] 用户登录
- [ ] Token 正确存储
- [ ] 无效 Token 自动清除

### 邮件列表
- [ ] 加载 INBOX
- [ ] 加载 SENT
- [ ] 加载 DRAFTS
- [ ] 加载 TRASH
- [ ] 分页查询

### 邮件操作
- [ ] 查看邮件详情
- [ ] 标记已读/未读
- [ ] 标记/取消星标
- [ ] 删除邮件（到 TRASH）
- [ ] 移动邮件到其他文件夹

### 邮件发送
- [ ] 发送邮件（不含附件）
- [ ] 验证已发送邮件出现在 SENT 文件夹

### 错误处理
- [ ] 网络错误时显示错误消息
- [ ] 后端返回错误时正确处理
- [ ] 加载失败时显示 loading 状态

## 常见问题

### Q: 为什么移除了 Mock 数据？
A: 题目要求所有开发和调试都基于真实后端，不使用 Mock 数据。这样能确保前后端接口完全兼容，避免因 Mock 数据导致的隐藏问题。

### Q: 现有组件能继续工作吗？
A: 是的。通过在 Pinia Store 中保留向后兼容的方法和计算属性，现有组件无需修改。但建议逐步升级到新的 API。

### Q: 如何处理邮件详情中的空字段？
A: 使用可选字段标记（`?`），如 `contentHtml?: string`。在模板中使用条件渲染处理。

### Q: 时间格式如何处理？
A: 后端返回 ISO 8601 格式字符串，前端在需要时手动转换：
```typescript
const date = new Date(item.receivedAt)
const formatted = date.toLocaleString('zh-CN')
```

### Q: 如何处理加载状态？
A: Pinia Store 提供了 `isLoading` 和 `error` 状态，组件可以通过它们显示加载动画和错误提示。

## 后续计划

- [ ] 实现 Workspace API（`/workspace/views` 等）
- [ ] 实现附件上传/下载
- [ ] 实现邮件搜索
- [ ] 实现 AI 任务前端
- [ ] 实现分类管理
- [ ] 实现邮件同步和通知

