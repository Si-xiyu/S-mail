# main-test 测试审查报告

## 测试范围

- 分支：`main-test`
- 范围：`backend` 与 `agent`
- 约束：未修改业务代码，仅新增/更新测试与本报告。
- 依据：`项目架构与技术选型.md`、`docs/design/mvp-architecture.md`、`docs/api/mvp-api.md`、`docs/api/agent-plugin-api.md`

## 新增/更新的测试

### Backend

- `AttachmentServiceTest`
  - 跨用户不能绑定他人的 Pending Attachment。
  - 同一个 Pending Attachment 绑定后不能再次绑定到另一封邮件。
- `WorkspaceServiceBehaviorTest`
  - 普通文件夹分页不应二次分页导致第二页为空。
  - 已软删除的 Mailbox Item 不应能通过工作台详情接口读取。
- `InternalToolServiceSecurityTest`
  - Internal Tool 写入 AI 结果前应校验用户是否拥有对应 Mailbox Item。
  - `SET_CATEGORY` 缺少 `categoryId` 时应拒绝执行。

### Agent

- `test_plugin_tool_router.py`
  - 写操作缺少 `mailItemId` 上下文时，不应生成可提交给后端的 pending action。
  - `SET_CATEGORY` pending action 应使用后端契约要求的 `categoryId`。
- `test_plugin_api.py`
  - 执行 `SET_CATEGORY` 时缺少 `categoryId` 应在 Agent 层拒绝，而不是委派给后端。

## 运行结果

### Backend

命令：

```powershell
mvn clean test
```

最新结果：通过。

通过：

- `AttachmentServiceTest`: 4/4 passed
- `InternalToolServiceSecurityTest`: 2/2 passed
- `SearchServiceBehaviorTest`: 1/1 passed
- `WorkspaceServiceBehaviorTest`: 2/2 passed

失败：无

### Agent

命令：

```powershell
E:\software\Miniconda\python.exe -m pytest
```

最新结果：通过。

通过：28

失败：无

说明：直接运行 `python -m pytest` 时，本机 `python.exe` 指向 WindowsApps 占位程序，会报“指定的登录会话不存在”。使用 `E:\software\Miniconda\python.exe` 可以稳定运行。

## 发现的问题

说明：问题 1-5 已由 `aebec8d 修复 main-test 契约测试发现` 修复，并在本轮回归中通过。问题 6 已由 `0f71f97 修复搜索参数边界` 修复，并在本轮回归中通过。

### 1. Workspace 普通文件夹列表存在二次分页

风险等级：高

现象：

- `WorkspaceService.listMailItems("inbox", null, null, 2, 2)` 调用 Mapper 时已经带了 `LIMIT 2 OFFSET 2`。
- Service 随后又对返回的 2 条记录按同样 offset 再次 `subList`。
- 第二页会变成空列表。

影响：

- Inbox/Sent/Trash/Junk 等普通文件夹第二页及后续分页可能不可用。
- 与 `docs/api/mvp-api.md` 中分页格式和 Workspace Mail List API 预期不符。

建议：

- 对已经由 Mapper 分页的普通 folder 查询，不要在 Service 层再次分页。
- 如果存在 category/keyword 内存过滤，应先明确查询策略：要么数据库层统一过滤分页，要么内存过滤后重新分页，但不能混用已分页子集再全局分页。

### 2. Workspace 详情接口没有过滤已软删除条目

风险等级：高

现象：

- `WorkspaceService.getMailItemDetail(itemId)` 只校验 `item.userId == currentUserId`。
- 没有校验 `deletedFlag`。

影响：

- 用户在 Trash 中二次删除后，理论上该条目应对当前用户隐藏。
- 当前实现可能仍允许通过已知 `itemId` 读取详情，违背“用户视角软删除”的边界。

建议：

- 详情读取使用和其他路径一致的 visible item 校验。
- 至少在 `item == null || !userId.equals(item.getUserId()) || deletedFlag` 时返回 404。

### 3. Internal Tool `saveAiResult` 未校验 mail/user 所有权

风险等级：高

现象：

- `InternalToolService.saveAiResult` 直接按请求中的 `mailId`、`userId` 插入 `mail_ai_result`。
- 没有确认该用户是否拥有该邮件对应的可见 Mailbox Item。

影响：

- Agent Plugin 或内部调用方一旦传入错误 user/mail 组合，可能给无关用户写入 AI 结果。
- 这破坏了“后端是唯一业务权限中心，Agent 只通过 API 写回允许结果”的架构边界。

建议：

- 写入前调用 `mailboxMapper.findVisibleByUserAndMail(userId, mailId)`。
- 不存在可见条目时拒绝写入。

### 4. Agent 可生成缺少 `mailItemId` 的写操作

风险等级：中高

现象：

- Current-Mail Agent 收到 “Set priority to high” 这类写意图时，即使 `context` 为空，也会返回 `SUCCEEDED` 和 pending action。
- action id 中 mail 部分变成 `unknown`，payload 只有 `userId` 和 action 字段。

影响：

- 前端可能展示一个看似可确认的写操作，但后端执行时会因为缺少 `mailItemId` 拒绝。
- 用户体验和契约都不稳定。

建议：

- 写操作没有可解析的 `mailItemId` 时，Agent 应返回 `FAILED` 或明确的拒绝消息，不生成 pending action。

### 5. Agent `SET_CATEGORY` 契约与后端不一致

风险等级：中高

现象：

- Agent 生成 `SET_CATEGORY` pending action 时 payload 为 `{"category": "FOLLOW_UP"}`。
- 后端 `InternalToolService.executeAction` 要求 `categoryId`，缺失时抛错。
- Agent `/actions/execute` 对缺少 `categoryId` 的 `SET_CATEGORY` 仍返回 `DELEGATED`。

影响：

- 用户确认后端必然失败。
- Agent Plugin API 与后端 Internal Tool API 不一致。

建议：

- Tool Router 从上下文或前端选择中携带 `categoryId`。
- `/actions/execute` 对 `SET_CATEGORY` 缺少 `categoryId` 的请求直接 `REJECTED`。

### 6. SearchService 没有规范 folder 和分页参数

风险等级：中

状态：已由 `0f71f97` 修复。

现象：

- `SearchService.search("project", "inbox", 0, 500)` 会把 `folder="inbox"` 原样传给 Mapper。
- `page=0` 会计算出 `offset=-500`。
- `pageSize=500` 会原样传到底层 SQL。

影响：

- 用户传小写 `folder=inbox` 时可能查不到数据，因为数据库中 folder 使用 `INBOX`。
- 负 offset 或超大 limit 会导致 SQL 行为不稳定，和 `MailboxService.list` 已有的 `page/pageSize` 防御不一致。

建议：

- `folder` 统一 `trim().toUpperCase()`，空值表示不按文件夹过滤或按产品约定默认 `INBOX`。
- `page` 至少为 1，`pageSize` 限制在 1 到 50。
- 对空白 keyword 给出 400，或明确实现“列出全部”的产品语义。

## 已确认通过的边界

- Pending Attachment 不能被其他用户绑定。
- Pending Attachment 绑定后状态变为 `BOUND`，不能再次绑定到另一封邮件。
- 既有 Agent health、分析 fallback、当前邮件上下文、Global mock RAG、确认写操作等原有测试大部分仍通过。

## 测试环境问题记录

- `backend/target/classes/application-dev.yml` 未被 Git 跟踪，`.gitignore` 已包含 `target/`。
- 普通沙箱命令曾无法创建 `backend/target/classes`；使用升级权限运行 `mvn clean test` 后可正常编译并进入测试阶段。
- Agent 侧应使用 `E:\software\Miniconda\python.exe -m pytest`，不要使用 WindowsApps 的 `python.exe` 占位程序。

## 建议优先级

1. 后续补充 Controller 层集成测试，覆盖 token 鉴权、跨用户访问、附件下载权限、Workspace API 全链路。
2. 补充搜索接口集成测试，验证 `folder=inbox`、空 keyword、page/pageSize 边界和搜索结果分页。
3. 补充邮件发送集成测试，覆盖无效收件人、抄送、附件绑定、投递后分析任务创建。
