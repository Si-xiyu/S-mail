# 前端快速开始

## 分支信息

- **分支名**: `feature/frontend-ui`
- **提交信息**: 完成前端基础 UI 框架搭建

## 快速启动

### 1. 安装依赖

```bash
cd frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 `http://127.0.0.1:5173` 即可查看前端应用。

### 3. 构建生产版本

```bash
npm run build      # 编译 TypeScript + Vite 打包
npm run preview    # 预览生产构建
```

## 项目文件结构

```
frontend/
├── src/
│   ├── components/
│   │   ├── TopBar.vue          ✅ 顶部导航栏（搜索、用户菜单）
│   │   ├── Sidebar.vue         ✅ 左侧边栏（文件夹导航、撰写按钮）
│   │   ├── MailList.vue        ✅ 邮件列表（可选中、标星、刷新）
│   │   ├── MailDetail.vue      ✅ 邮件详情（完整内容、回复、转发）
│   │   └── ComposeDialog.vue   ✅ 撰写邮件（浮窗式、可最小化）
│   ├── stores/
│   │   └── mailStore.ts        ✅ Pinia 状态管理（Mock 数据）
│   ├── api/
│   │   └── client.ts           ✅ HTTP 客户端（带降级 Mock）
│   ├── types/
│   │   ├── index.ts            ✅ 核心类型定义
│   │   └── mail.ts             ✅ API 响应类型
│   ├── App.vue                 ✅ 根组件（三栏布局）
│   ├── main.ts                 ✅ 入口文件
│   └── styles.css              ✅ 全局样式
└── index.html                  ✅ HTML 入口
```

## 核心功能一览

| 功能 | 组件 | 状态 | 说明 |
|------|------|------|------|
| 邮件列表 | MailList | ✅ | 支持选中、已读、标星、刷新 |
| 邮件详情 | MailDetail | ✅ | 完整内容、收件人、时间戳、附件 |
| 撰写邮件 | ComposeDialog | ✅ | To/Cc/Bcc/Subject/Content 完整表单 |
| 边栏导航 | Sidebar | ✅ | 文件夹切换、计数显示 |
| 顶部导航 | TopBar | ✅ | 搜索框、用户菜单 |
| 状态管理 | mailStore | ✅ | 邮件列表、用户信息、文件夹 |
| API 客户端 | client.ts | ✅ | 完整接口定义 + Mock 降级 |

## 当前数据流

```
TopBar
  └── 搜索、用户信息（来自 mailStore.user）

Sidebar
  └── 文件夹列表 ──> 点击切换 ──> mailStore.selectLabel()

App (主容器)
  ├── MailList (展示当前文件夹邮件)
  │   └── 选中邮件 ──> App.selectedMailId ──> MailDetail 显示
  └── MailDetail (展示邮件详情)
      └── 操作按钮（删除、标星等）──> 更新 mailStore

ComposeDialog
  └── 发送 ──> mailStore.sendMail() ──> 新邮件加入 SENT 文件夹
```

## 样式设计规范

采用 **Notion Mail** 风格：

- **主题色**: #667eea (紫蓝色)
- **背景**: #f9fafb (浅灰)、#ffffff (白)
- **边框**: #e5e7eb (浅灰)
- **文字**: #1f2937 (深灰)、#6b7280 (中灰)、#9ca3af (浅灰)
- **圆角**: 8px 或 24px
- **间距**: 8px/12px/16px/24px

## 开发建议

### 1. 添加新功能时

```typescript
// 1. 更新状态 (stores/mailStore.ts)
const newState = ref<Type>(initialValue)

const newAction = (params) => {
  // 更新逻辑
}

return { newState, newAction }

// 2. 在组件中使用
const mailStore = useMailStore()
// mailStore.newState
// mailStore.newAction()
```

### 3. 修改样式时

- 组件内样式：`<style scoped>`（不会污染其他组件）
- 全局样式：`src/styles.css`（需谨慎使用）

### 4. 调试时

```bash
# 终端查看网络请求和控制台输出
npm run dev

# 浏览器 DevTools
# 1. Vue DevTools (vue-devtools Chrome 插件)
# 2. Network 标签查看 HTTP 请求
# 3. Console 查看日志
```

## API 接口对接

前端已准备好连接后端，无需修改代码。当后端启动时，自动切换为真实接口：

```typescript
// src/api/client.ts 已包含以下接口
login(email, password)           // POST /api/v1/auth/login
register(email, username, password) // POST /api/v1/auth/register
listMailbox(folder)              // GET /api/v1/mailbox?folder=INBOX
getMailDetail(mailId)            // GET /api/v1/mails/{mailId}
sendMail(payload)                // POST /api/v1/mails
runAiTask(mailId, task)          // POST /api/v1/ai/mails/{mailId}/{task}
```

**详见**: [`docs/api/frontend-api.md`](docs/api/frontend-api.md)

## Mock 数据说明

当前前端使用的是 **客户端 Mock 数据**（位于 `mailStore.ts` 中）：

```typescript
mockMails: Mail[]  // 7 条示例邮件

labels: Label[]    // 6 个文件夹
  - INBOX (收件箱)
  - STARRED (标星)
  - SENT (已发送)
  - DRAFTS (草稿)
  - TRASH (垃圾箱)
  - SPAM (垃圾邮件)
```

### Mock 数据的生命周期

- **现在**: 每次刷新页面重置
- **后续**: 改为后端 API 后，数据持久化

## 常见问题

### Q1: 前端如何连接后端？

**A**: 前端已内置 API 客户端 (`src/api/client.ts`)。后端启动后：

1. 修改 `vite.config.ts` 中的 proxy 配置（或直接改 baseURL）
2. HTTP 请求会自动转发到后端
3. 无需修改组件代码

```typescript
// src/api/client.ts
const http = axios.create({
  baseURL: '',  // 改为 'http://backend-url' 或配置 Vite proxy
  timeout: 5000
})
```

### Q2: 如何修改邮件数据？

**A**: 编辑 `src/stores/mailStore.ts` 中的 `mockMails` 数组。

### Q3: 样式如何调整？

**A**:
- 整体主题色: Sidebar.vue `.compose-btn` (#667eea)
- 全局颜色: src/styles.css
- 组件样式: 各 .vue 文件的 `<style scoped>` 块

### Q4: 如何添加新组件？

**A**:
1. 在 `src/components/` 创建 `.vue` 文件
2. 编写 `<script setup>`、`<template>`、`<style scoped>`
3. 在 `App.vue` 或其他组件中导入使用

### Q5: 邮件列表太慢了？

**A**: 现使用简单数组渲染。优化方案：
- 虚拟滚动 (vue-virtual-scroller)
- 分页加载
- 缓存策略

---

## 下一步任务

- [ ] 后端对接（改 API baseURL）
- [ ] 添加登录/注册页面
- [ ] 实现搜索功能
- [ ] 添加附件上传下载
- [ ] 集成 AI 功能（摘要、回复草稿等）
- [ ] 响应式设计（移动端适配）
- [ ] 性能优化（虚拟滚动、代码分割等）

---

## 相关文档

- 📚 [前端开发指南](docs/FRONTEND_GUIDE.md) - 详细的组件和功能说明
- 🔗 [API 文档](docs/api/frontend-api.md) - 前后端接口规范
- 🏗️ [项目总体规范](CLAUDE.md)
- 🎨 [设计规范](项目架构与技术选型.md)

---

**现在可以开始开发了！** 🚀

```bash
cd frontend && npm run dev
```

访问 http://127.0.0.1:5173 开始体验。
