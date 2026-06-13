# 前端开发指南

## 项目简介

基于 Vue 3 + TypeScript + Element Plus 的 SmartMail 邮件系统前端。采用 Notion Mail 的设计风格，包含邮件列表、邮件详情、撰写邮件等基础功能。

## 技术栈

- **框架**: Vue 3 (SFC)
- **类型**: TypeScript
- **样式**: Scoped CSS + Element Plus
- **状态管理**: Pinia
- **网络请求**: Axios
- **构建工具**: Vite

## 项目结构

```
frontend/
├── src/
│   ├── api/              # API 客户端
│   │   └── client.ts     # Axios 实例和接口函数
│   ├── components/       # 可复用组件
│   │   ├── TopBar.vue    # 顶部导航栏
│   │   ├── Sidebar.vue   # 左侧边栏
│   │   ├── MailList.vue  # 邮件列表
│   │   ├── MailDetail.vue # 邮件详情
│   │   └── ComposeDialog.vue # 撰写邮件对话框
│   ├── stores/           # Pinia 状态管理
│   │   └── mailStore.ts
│   ├── types/            # TypeScript 类型定义
│   │   ├── index.ts      # 通用类型
│   │   └── mail.ts       # 邮件相关类型
│   ├── App.vue           # 根组件
│   ├── main.ts           # 入口文件
│   ├── styles.css        # 全局样式
│   └── env.d.ts          # Vue 模块声明
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

## 核心功能

### 1. 邮件列表 (MailList.vue)

- 展示指定文件夹的邮件列表
- 支持邮件选中、已读状态、标星、附件图标显示
- 自动格式化时间显示（相对时间）
- 快速刷新按钮

**关键状态**:
```typescript
selectedMailId: string | null  // 当前选中邮件ID
```

**关键方法**:
- `handleSelectMail(mailId)` - 选中邮件
- `formatTime(timestamp)` - 格式化时间显示

### 2. 邮件详情 (MailDetail.vue)

- 展示完整邮件内容
- 支持回复、转发、删除操作
- 显示附件列表
- AI 功能占位符（摘要、优先级、标签）

### 3. 撰写邮件 (ComposeDialog.vue)

- 浮窗式撰写界面
- 支持最小化/还原
- 完整的 To/Cc/Bcc 字段
- 发送前验证收件人和主题

### 4. 左侧边栏 (Sidebar.vue)

- 快速撰写按钮
- 文件夹导航（收件箱、标星、已发送、草稿等）
- 文件夹邮件计数显示
- 设置和帮助按钮

### 5. 顶部导航栏 (TopBar.vue)

- 搜索框
- 用户菜单（头像 + 下拉菜单）
- 全局操作按钮

## 状态管理 (Pinia Store)

### useMailStore

```typescript
// 当前用户信息
user: User

// 当前选中文件夹
currentLabel: string

// 所有标签
labels: Label[]

// 邮件列表
mails: Mail[]

// 计算属性
currentMails: Mail[]      // 当前文件夹邮件
mailItems: MailItem[]     // 列表显示格式的邮件

// 方法
markAsRead(id: string)    // 标记为已读
toggleStar(id: string)    // 切换标星
deleteMail(id: string)    // 删除邮件
moveToLabel(id, label)    // 移动邮件到标签
sendMail(to, subject, content, cc?) // 发送邮件
selectLabel(label)        // 选择文件夹
getMailById(id: string)   // 获取邮件详情
```

## 样式设计

采用 **Notion Mail** 风格设计：

- **配色**: 浅色系 + 紫蓝色强调色 (#667eea)
- **布局**: 三栏式布局（边栏 + 列表 + 详情）
- **圆角**: 统一使用 8px 或 24px 圆角
- **阴影**: 轻量级阴影，仅在悬停/焦点时显示
- **字体**: 系统字体栈，包含 PingFang SC 和 Microsoft YaHei

### 主要颜色

- 背景: #f9fafb, #ffffff
- 边框: #e5e7eb
- 文字: #1f2937 (深), #6b7280 (浅)
- 主题色: #667eea (蓝紫色)
- 成功: #10b981 (绿色)
- 警告: #f59e0b (黄色)
- 错误: #ef4444 (红色)

## API 集成

前端通过 `src/api/client.ts` 与后端通信：

```typescript
// 认证
login(email, password)
register(email, username, password)

// 邮箱操作
listMailbox(folder)
getMailDetail(mailId)
sendMail(payload)

// AI 功能
runAiTask(mailId, task)
```

详见 [`docs/api/frontend-api.md`](./frontend-api.md)

## 开发流程

### 本地开发

```bash
cd frontend
npm install      # 安装依赖
npm run dev      # 启动开发服务器 (http://127.0.0.1:5173)
```

### 构建

```bash
npm run build    # 编译 TypeScript + Vite 打包
npm run preview  # 预览生产构建
```

### 代码规范

- **命名**: PascalCase 组件名，camelCase 函数名
- **类型**: 所有公共接口需要 TypeScript 类型标注
- **注释**: 仅在逻辑复杂处添加注释
- **样式**: 使用 Scoped CSS，避免全局样式污染

## 常见任务

### 添加新组件

1. 在 `src/components/` 下创建 `.vue` 文件
2. 定义 Props、Emits 和内部状态
3. 在 `App.vue` 或父组件中导入并使用

### 添加新页面/视图

1. 在 `src/views/` 下创建 `.vue` 文件（暂未使用 Vue Router）
2. 可在 App.vue 中切换显示

### 调整样式

- 组件内使用 `<style scoped>`
- 全局样式在 `src/styles.css`
- 主题色在 Sidebar.vue 的 `.compose-btn` 中（#667eea）

### 添加状态

在 `src/stores/mailStore.ts` 中：

```typescript
const newState = ref<Type>(initialValue)

// 添加方法
const actionName = (params) => {
  // ...
}

// 导出
return {
  newState,
  actionName
}
```

## 已知限制

1. **Mock 数据**: 现使用内存 Mock 数据，刷新页面数据丢失
2. **离线功能**: 不支持离线操作
3. **文件上传**: 附件功能未实现（UI 已就位）
4. **实时更新**: 不支持多标签页实时同步
5. **搜索**: 搜索框 UI 就位，后端接口待实现

## 后续优化方向

- [ ] 集成 Vue Router 实现 SPA 路由
- [ ] 添加邮件搜索/筛选功能
- [ ] 实现附件上传下载
- [ ] 添加草稿自动保存
- [ ] 集成 AI 摘要、回复草稿等功能
- [ ] 添加响应式设计支持移动端
- [ ] 性能优化（虚拟滚动、缓存等）
- [ ] 国际化（i18n）支持

## 相关文档

- [API 文档](./frontend-api.md)
- [CLAUDE.md](../../CLAUDE.md) - 项目总体规范
- [项目架构与技术选型](../../项目架构与技术选型.md)
