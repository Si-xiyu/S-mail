# Basic Mail MVP 迭代报告

## 1. 本次目标

以《项目架构与技术选型.md》为实现标准，在独立分支 `feature/basic-mail-mvp` 上完成可启动的前端、后端及核心邮件工作流，并由前端和 Agent 适配后端 API 契约。

## 2. 实际完成内容

- 修复前端构建、登录状态恢复、失效 Token 清理和启动流程。
- 后端默认使用文档允许的 H2 开发配置，可直接启动；MySQL 配置仍保留给持久化部署。
- 完成邮件列表、分页、详情、发送、TO/CC/BCC、附件、回复、转发、Thread、已读、星标、搜索、标签、Junk、删除、Trash 恢复和永久删除。
- 草稿按文档要求采用用户隔离的本地自动保存，并支持恢复和丢弃 Pending Attachment。
- 接入通知轮询、未读/文件夹计数、手动同步、自动同步和基础账户/同步设置。
- 后端通知返回服务器游标，查询窗口采用 `(since, cursor]`，避免漏报和重复窗口。
- 修复 Thread 越权读取、伪造父邮件、无效认证 HTTP 状态、BCC 隐私和重复收件人投递问题。
- 修复 H2 与 MySQL 列名大小写差异造成的搜索结果空字段问题。

## 3. 修改文件统计

截至搜索修复代码（不含本报告及故障日志），相对本迭代起点 `d5d6c1c`：34 个文件，新增 1811 行，删除 1004 行。范围集中在 `backend/src`、`frontend/src` 和 `backend/src/test`。

## 4. Git Commit 历史

- `0c32d1b fix(frontend): restore build and authenticated startup`
- `b2dcc8a feat(backend): secure mail threads and recipient delivery`
- `f5665bc feat(backend): complete mailbox search and label semantics`
- `9d1a518 feat(frontend): complete core mailbox workflows`
- `6de0556 feat(frontend): add mailbox sync and basic settings`
- `f220d70 fix(integration): make mailbox sync and actions reliable`
- `bfcdeb9 test(backend): cover mailbox state and notification contracts`

搜索大小写映射修复及其回归测试已通过全部验证，但因本地 Git 写入审批服务连续断线，生成本报告时仍待提交。

## 5. 功能实现情况

| 模块 | 状态 | 验收结果 |
| --- | --- | --- |
| Frontend | 已完成 | `npm run build`、`vue-tsc --noEmit` 通过，开发服务器可启动 |
| Backend | 已完成 | H2 默认配置启动成功，最终 JAR 构建成功 |
| Auth | 已完成 | 注册、登录、401、Token 恢复链路通过 |
| Mail API | 已完成 | 发送、列表、详情、状态变更及投递通过 |
| Compose | 已完成 | TO/CC/BCC、附件和部分投递反馈已接通 |
| Draft | 已完成 | 本地自动保存、恢复、丢弃通过静态链路验证 |
| Thread | 已完成 | 回复后 root + reply Thread 真实 API 验证通过 |
| Search | 已完成 | 搜索结果字段修复后真实 API 定向复测通过 |
| Label | 已完成 | 多标签、幂等分配、删除回退由单元测试覆盖 |
| Integration | 已完成 | 核心前后端 API 链路真实运行通过 |
| Tests | 部分完成 | 后端、构建、类型、API 全绿；浏览器 UI 点击测试因运行环境无可用应用内 Browser 未执行 |

## 6. 测试结果

- 后端最终回归：103 个生产源码、10 个测试源码编译成功；23 项测试，0 Failure、0 Error、0 Skipped。
- 前端：Vite production build 通过，Vue TypeScript 检查通过，开发服务器启动通过。
- API 集成：无 Token 401、双用户注册、发送、收件列表、详情、通知游标、回复、Thread、搜索、删除进 Trash、恢复至 Inbox 均通过。
- 搜索定向复测：`total=1`，`itemId`、`mailId`、发件人、主题、文件夹、优先级、接收时间和摘要均正确且非空。
- UI 自动化：应用内 Browser 列表为空，无法执行真实 DOM 点击；未将其虚报为通过。
- 非阻塞警告：前端主 bundle 超过 500 kB；第三方包存在 Rollup PURE 注释位置警告。

## 7. 已解决问题

- 前端依赖已存在，无法构建并非缺少依赖；根因是类型/API 契约和组件实现不一致，已修复。
- 认证错误过去返回 HTTP 200，现按实际错误返回 401/4xx。
- Thread 接口越权和 DTO 不兼容已修复。
- BCC 泄露、重复投递和错误 Thread 归属已修复。
- Trash、SENT、Junk 状态迁移和多标签唯一约束已修复。
- 邮箱操作失败后 UI 假成功、星标分页截断、通知游标竞态已修复。
- H2 搜索 Map 使用小写列名导致 DTO 全空的问题已修复。
- `logs/backend_build_acl_failure.md` 中的 Maven ACL 故障已复查并标记为已解决。

## 8. 遗留问题

- 浏览器 UI 点击测试未执行：测试运行环境没有可用应用内 Browser。构建、类型、静态链路与真实 API 已覆盖，但不能等同于完整 UI E2E。
- 主 bundle 约 1.1 MB，后续可用路由和组件动态导入拆包。
- 最终搜索修复和本报告仍需在 Git 审批服务恢复后提交。

## 9. 产品当前状态

Basic Mail MVP 的前端、后端和核心 API 已可运行，主要邮件工作流具备实际数据闭环。当前不包含 Gmail 的高级 AI、Workspace 协作和完整服务器草稿同步；这与本迭代限定的 Basic Mail 范围一致。

严格按 Definition of Done 判断，产品功能已完成，但由于 UI E2E 未执行、最后 Git 提交受外部审批服务阻塞，迭代暂不能标记为完全完成。

## 10. 下一阶段建议

1. 在具备 Browser/Playwright 的环境补充登录、Compose、Drawer、设置和异常反馈 E2E。
2. 将前端页面和 Element Plus 依赖按路由/功能拆包。
3. 增加服务器草稿同步，保留当前本地草稿作为离线恢复层。
4. 在 MySQL 8 环境执行迁移与 API 回归，验证多标签唯一索引和搜索查询计划。
5. Basic Mail 稳定后，再按文档边界逐步启用 Agent Plugin，不允许 Agent 绕过后端业务和权限 API。
