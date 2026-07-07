# SmartMail Demo 快速启动与人工测试指南

当前 Demo 是三服务架构：

```text
Frontend http://127.0.0.1:5173
  -> Spring Boot Backend http://localhost:8080
      -> Python Agent Plugin http://127.0.0.1:8000
          -> Backend Internal Tool API
```

关键规则：

- 前端只调用后端 `/api/v1/**`，不直连 Agent。
- 后端负责鉴权、权限、业务写入和持久化。
- Agent 只提供智能能力，不直接访问数据库。
- 邮箱地址必须使用 `@smail.com` 后缀。
- AI 可关闭；关闭后传统邮箱功能仍应可用。

---

## 1. 首次准备

### 1.1 检查环境

```powershell
java -version
mvn -v
node -v
npm -v
python --version
```

建议：Java 17+、Maven 3.6+、Node.js 18+、Python 3.11+。

### 1.2 安装依赖

```powershell
cd agent
python -m pip install -r requirements.txt

cd ..\frontend
npm install
```

如果你的 Python 固定在 Miniconda：

```powershell
E:\software\Miniconda\python.exe -m pip install -r agent\requirements.txt
```

---

## 2. 只需要填一次的 Agent 配置

复制模板：

```powershell
cd agent
Copy-Item .env.example .env
Copy-Item config\providers.example.toml config\providers.toml
```

然后编辑 `agent/.env`，只需要重点填这一项：

```env
DEEPSEEK_API_KEY=sk-你的DeepSeek-Key
```

`agent/.env` 推荐内容：

```env
SMARTMAIL_BACKEND_BASE_URL=http://localhost:8080
SMARTMAIL_INTERNAL_TOKEN=smartmail-internal-dev-token
SMARTMAIL_PLUGIN_TOKEN=smartmail-agent-plugin-dev-token
SMARTMAIL_AGENT_CONFIG=./config/providers.toml
DEEPSEEK_API_KEY=sk-你的DeepSeek-Key
SMARTMAIL_AGENT_RAG_MODE=BACKEND
SMARTMAIL_AGENT_MOCK_ON_TOOL_ERROR=false
SMARTMAIL_LOG_LEVEL=INFO
```

`agent/config/providers.toml` 保持模板即可：

```toml
[providers.deepseek]
base_url = "https://api.deepseek.com"
model = "deepseek-chat"
api_key_env = "DEEPSEEK_API_KEY"

[features.analysis]
provider = "deepseek"
model = "deepseek-chat"
timeout_seconds = 15

[features.chat]
provider = "deepseek"
model = "deepseek-chat"
timeout_seconds = 30
```

说明：

- API Key 只写在 `agent/.env`。
- `providers.toml` 只写 DeepSeek 的地址、模型和“从哪个环境变量取 key”。
- `.env` 和 `providers.toml` 都不要提交到 Git。

---

## 3. 最简启动方式（三个终端）

### 终端 1：启动 Agent

从项目根目录执行：

```powershell
.\scripts\start-agent.ps1
```

如果要指定 Python：

```powershell
.\scripts\start-agent.ps1 -Python "E:\software\Miniconda\python.exe"
```

脚本会自动：

- 检查 `agent/.env`，没有则从 `.env.example` 创建；
- 检查 `agent/config/providers.toml`，没有则从 `providers.example.toml` 创建；
- 使用 `uvicorn --env-file .env` 启动 Agent；
- 默认端口：`127.0.0.1:8000`。

验证：

```powershell
Invoke-RestMethod http://127.0.0.1:8000/plugin/v1/health
```

预期包含：

```json
{ "status": "UP" }
```

### 终端 2：启动后端并启用 AI

从项目根目录执行：

```powershell
.\scripts\start-backend-ai.ps1
```

脚本会自动设置：

```env
SMARTMAIL_AI_ENABLED=true
SMARTMAIL_AGENT_BASE_URL=http://127.0.0.1:8000
SMARTMAIL_AGENT_PLUGIN_TOKEN=smartmail-agent-plugin-dev-token
SMARTMAIL_INTERNAL_TOKEN=smartmail-internal-dev-token
```

验证：

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

预期：

```json
{ "status": "UP" }
```

如果要测试 AI 关闭模式：

```powershell
.\scripts\start-backend-ai.ps1 -DisableAi
```

### 终端 3：启动前端

```powershell
cd frontend
npm run dev
```

打开：

```text
http://127.0.0.1:5173
```

---

## 4. 为什么以前要重复填这么多 API 配置

因为有三个独立进程，调用方向不同：

| 配置 | 谁用 | 用途 |
| --- | --- | --- |
| `SMARTMAIL_BACKEND_BASE_URL` | Agent | Agent 调后端 Internal Tool API |
| `SMARTMAIL_INTERNAL_TOKEN` | Agent + 后端 | 保护 `/internal/v1/tools/**` |
| `SMARTMAIL_PLUGIN_TOKEN` | 后端 + Agent | 后端调 Agent `/plugin/v1/**` |
| `SMARTMAIL_AGENT_BASE_URL` | 后端 | 后端知道 Agent 在哪里 |
| `DEEPSEEK_API_KEY` | Agent | Agent 调 DeepSeek |

现在通过脚本后：

- Agent 相关配置集中在 `agent/.env`。
- 后端联调配置由 `scripts/start-backend-ai.ps1` 自动注入。
- 你日常只需要改 `agent/.env` 里的 DeepSeek Key。

---

## 5. 人工测试主流程

### 5.1 注册/登录

使用任意 `@smail.com` 邮箱，例如：

```text
demo1@smail.com
```

密码建议：

```text
Password123!
```

验证：登录后进入邮箱工作台，Console 中有 token：

```javascript
localStorage.getItem('smartmail_token')
```

### 5.2 传统邮箱能力

至少覆盖：

- 发送邮件，收件人必须是 `@smail.com`。
- 收件箱能看到收到的邮件。
- 点击邮件后右侧详情正常展示。
- 标记已读后邮件不能从 Inbox 消失。
- 删除邮件后第一次应进入 Trash。
- Trash 中能看到被删除邮件。
- 搜索邮件关键词能返回结果。

推荐测试邮件：

```text
subject: Google Ads Demo
body: Google Ads promotion. This is an advertisement email for demo deletion.
```

### 5.3 当前邮件 Agent

1. 打开 Inbox。
2. 点击一封邮件。
3. 右侧应显示 Agent 对话区。
4. 输入：

```text
这封邮件需要我做什么？
```

预期：

- Agent 返回摘要或说明。
- Network 中前端只请求 `/api/v1/agent/sessions/**`。
- 后端转发到 Agent `/plugin/v1/agent/chat`。

### 5.4 Agent 写操作确认

打开广告测试邮件后输入：

```text
帮我把这封广告邮件删掉
```

预期：

- Agent 不直接删除。
- 前端显示待确认动作。
- 点击确认后，后端执行写操作。
- 邮件移动到 Trash。

### 5.5 全局 Agent 搜索

点击侧边栏 SmartMail 助手入口，输入：

```text
帮我搜索 Google 的邮件
```

预期：Agent 返回相关邮件结果或说明；如果有写操作，必须先确认。

### 5.6 AI 关闭模式

启动后端时使用：

```powershell
.\scripts\start-backend-ai.ps1 -DisableAi
```

预期：

- 注册、登录、收发、搜索、删除、Trash 仍可用。
- Agent 对话返回 disabled/fallback 提示。
- 前端不崩溃。

---

## 6. 可选 API Smoke

Agent 和后端启动后，可用这个脚本验证“发邮件 -> Agent 删除 -> 确认 -> 进入 Trash”：

```powershell
$base = 'http://127.0.0.1:8080/api/v1'
$email = 'demo-smoke-' + [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds() + '@smail.com'

$register = Invoke-RestMethod -Uri "$base/auth/register" -Method Post -ContentType 'application/json' -Body (@{
  email = $email
  username = 'Demo User'
  password = 'Password123!'
} | ConvertTo-Json) -TimeoutSec 10

$headers = @{ Authorization = "Bearer $($register.data.token)" }

Invoke-RestMethod -Uri "$base/users/me/settings" -Method Patch -Headers $headers -ContentType 'application/json' -Body (@{
  aiEnabled = $true
  agentAutoWriteEnabled = $false
} | ConvertTo-Json) -TimeoutSec 10 | Out-Null

$subject = 'Google Ads Demo'
Invoke-RestMethod -Uri "$base/mails/send" -Method Post -Headers $headers -ContentType 'application/json' -Body (@{
  to = @($email)
  cc = @()
  bcc = @()
  subject = $subject
  contentText = 'Google Ads promotion. This is an advertisement email for demo deletion.'
  contentHtml = $null
  pendingAttachmentIds = @()
} | ConvertTo-Json -Depth 5) -TimeoutSec 10 | Out-Null

Start-Sleep -Seconds 1
$items = Invoke-RestMethod -Uri "$base/workspace/mail-items?view=inbox&page=1&pageSize=10" -Method Get -Headers $headers -TimeoutSec 10
$item = @($items.data.records | Where-Object { $_.subject -eq $subject })[0]
if ($null -eq $item) { throw 'mail not found in inbox' }

$session = Invoke-RestMethod -Uri "$base/agent/sessions" -Method Post -Headers $headers -ContentType 'application/json' -Body (@{
  scope = 'CURRENT_MAIL'
  context = @{ mailItemId = $item.itemId }
} | ConvertTo-Json -Depth 5) -TimeoutSec 10

$delete = Invoke-RestMethod -Uri "$base/agent/sessions/$($session.data.sessionId)/messages" -Method Post -Headers $headers -ContentType 'application/json' -Body (@{
  message = 'delete this advertisement email'
} | ConvertTo-Json) -TimeoutSec 20

$pending = @($delete.data.pendingActions)[0]
if ($null -eq $pending) { throw 'delete request did not return pending action' }

$confirm = Invoke-RestMethod -Uri "$base/agent/actions/$($pending.actionId)/confirm" -Method Post -Headers $headers -ContentType 'application/json' -Body (@{
  confirmed = $true
} | ConvertTo-Json) -TimeoutSec 10

$trash = Invoke-RestMethod -Uri "$base/workspace/mail-items?view=trash&page=1&pageSize=10" -Method Get -Headers $headers -TimeoutSec 10
$trashHit = @($trash.data.records | Where-Object { $_.itemId -eq $item.itemId }).Count

[PSCustomObject]@{
  email = $email
  itemId = $item.itemId
  pendingActionType = $pending.type
  confirmStatus = $confirm.data.status
  trashContainsItem = ($trashHit -gt 0)
} | ConvertTo-Json -Depth 5
```

成功时应看到：

```json
{
  "pendingActionType": "MOVE",
  "confirmStatus": "EXECUTED",
  "trashContainsItem": true
}
```

---

## 7. 故障排查

| 现象 | 处理 |
| --- | --- |
| PowerShell 禁止运行脚本 | 当前窗口执行：`Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` |
| 8000 被占用 | `netstat -ano | findstr :8000`，然后 `taskkill /PID <PID> /F` |
| 8080 被占用 | `netstat -ano | findstr :8080`，然后 `taskkill /PID <PID> /F` |
| Agent 提示 DeepSeek Key 为空 | 编辑 `agent/.env`，填 `DEEPSEEK_API_KEY=sk-...` |
| Agent 返回 disabled | 确认后端用 `scripts/start-backend-ai.ps1` 启动，或前端设置中开启 AI |
| 前端 API 失败 | 确认后端 8080 已启动，且 `frontend/vite.config.ts` 代理 `/api -> http://localhost:8080` |
| 注册失败 | 邮箱必须是 `@smail.com` |
| 删除后 Trash 看不到 | 这是阻塞 bug；记录 Network 和后端日志 |

---

## 8. 最终检查清单

```text
☐ agent/.env 已填写 DEEPSEEK_API_KEY
☐ .\scripts\start-agent.ps1 已启动，/plugin/v1/health 返回 UP
☐ .\scripts\start-backend-ai.ps1 已启动，/actuator/health 返回 UP
☐ frontend npm run dev 已启动
☐ 浏览器打开 http://127.0.0.1:5173
☐ Network 中前端只请求 /api/v1/**
☐ 当前邮件 Agent 能回答
☐ Agent 写操作先出现确认按钮，确认后才执行
☐ AI 关闭后传统邮箱仍能使用
```
