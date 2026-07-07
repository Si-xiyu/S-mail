# SmartMail Demo 快速启动与人工测试指南

本文面向当前集成分支 `fullstack/agent-demo-integration` 的人工测试。当前 Demo 是三服务架构：

```text
Frontend http://127.0.0.1:5173
  -> Spring Boot Backend http://localhost:8080
      -> Python Agent Plugin http://127.0.0.1:8000
          -> Backend Internal Tool API
```

关键规则：

- 前端只调用后端 `/api/v1/**`，不要让前端直连 Agent。
- 后端是鉴权、权限、业务写入和持久化中心。
- Agent 只暴露能力和工具调用结果，不直接访问数据库。
- 邮箱地址必须使用 `@smail.com` 后缀。
- AI 功能可关闭；关闭后传统邮箱功能仍应可用。

---

## 1. 环境要求

在项目根目录执行以下检查：

```powershell
java -version
mvn -v
node -v
npm -v
python --version
```

建议版本：

| 组件 | 建议 |
| --- | --- |
| Java | 17+ |
| Maven | 3.6+ |
| Node.js | 18+ / 20+ |
| npm | 8+ |
| Python | 3.11+，当前环境 Python 3.13 已验证可跑测试 |

---

## 2. 首次依赖安装

### Agent 依赖

```powershell
cd agent
python -m pip install -r requirements.txt
```

如果本机有指定 Python，例如：

```powershell
E:\software\Miniconda\python.exe -m pip install -r requirements.txt
```

### 前端依赖

```powershell
cd frontend
npm install
```

说明：`npm install` 当前可能提示 `npm audit` 风险，不影响本地 Demo 启动和构建；安全升级后续单独处理。

---

## 3. API 与环境变量配置

### 3.1 后端配置

默认开发环境使用 H2 内存数据库，不需要安装 MySQL。配置来源：

```text
backend/src/main/resources/application-dev.yml
```

开发 profile 下默认：

```text
server.port=8080
H2 console=/h2-console
smartmail.ai.enabled=false
smartmail.ai.agent-base-url=http://localhost:8000
smartmail.internal-token=smartmail-internal-dev-token
smartmail.ai.plugin-token=smartmail-agent-plugin-dev-token
```

人工测试 Agent 联调时，必须在启动后端前设置：

```powershell
$env:SMARTMAIL_AI_ENABLED = "true"
$env:SMARTMAIL_AGENT_BASE_URL = "http://127.0.0.1:8000"
$env:SMARTMAIL_AGENT_PLUGIN_TOKEN = "smartmail-agent-plugin-dev-token"
$env:SMARTMAIL_INTERNAL_TOKEN = "smartmail-internal-dev-token"
```

如需测试“关闭 AI 后传统邮箱仍可用”：

```powershell
$env:SMARTMAIL_AI_ENABLED = "false"
```

### 3.2 Agent 配置

Agent 调后端 Internal Tool API 需要：

```powershell
$env:SMARTMAIL_BACKEND_BASE_URL = "http://localhost:8080"
$env:SMARTMAIL_INTERNAL_TOKEN = "smartmail-internal-dev-token"
$env:SMARTMAIL_PLUGIN_TOKEN = "smartmail-agent-plugin-dev-token"
```

### 3.3 DeepSeek / Provider 可选配置

不配置大模型 Key 时，Agent 会使用规则/Mock fallback，仍可完成本地 Demo。

如果要接 DeepSeek：

```powershell
cd agent
Copy-Item config\providers.example.toml config\providers.toml
$env:SMARTMAIL_AGENT_CONFIG = ".\config\providers.toml"
$env:DEEPSEEK_API_KEY = "sk-你的key"
```

`providers.toml` 示例格式：

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

注意：API Key 不写死在配置文件中，只通过环境变量读取。

### 3.4 前端 API 代理

前端 Vite 代理已配置：

```text
frontend/vite.config.ts
/api -> http://localhost:8080
```

浏览器 Network 中应该看到前端请求：

```text
http://127.0.0.1:5173/api/v1/...
```

不应该看到前端直接请求：

```text
http://127.0.0.1:8000/plugin/v1/...
```

---

## 4. 启动顺序（三个终端）

建议顺序：Agent -> 后端 -> 前端。后端也可以先启动，但 Agent 场景测试前必须保证 8000 可用。

### 终端 1：启动 Agent

```powershell
cd agent
$env:SMARTMAIL_BACKEND_BASE_URL = "http://localhost:8080"
$env:SMARTMAIL_INTERNAL_TOKEN = "smartmail-internal-dev-token"
$env:SMARTMAIL_PLUGIN_TOKEN = "smartmail-agent-plugin-dev-token"
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

验证：

```powershell
Invoke-RestMethod http://127.0.0.1:8000/plugin/v1/health
```

预期包含：

```json
{
  "status": "UP",
  "pluginVersion": "0.2.0"
}
```

### 终端 2：启动后端

```powershell
cd backend
$env:SMARTMAIL_AI_ENABLED = "true"
$env:SMARTMAIL_AGENT_BASE_URL = "http://127.0.0.1:8000"
$env:SMARTMAIL_AGENT_PLUGIN_TOKEN = "smartmail-agent-plugin-dev-token"
$env:SMARTMAIL_INTERNAL_TOKEN = "smartmail-internal-dev-token"
mvn spring-boot:run
```

验证：

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```

预期：

```json
{ "status": "UP" }
```

### 终端 3：启动前端

```powershell
cd frontend
npm install
npm run dev
```

打开：

```text
http://127.0.0.1:5173
```

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

验证点：

- 登录成功后进入邮箱工作台。
- 浏览器 Console 中有 token：

```javascript
localStorage.getItem('smartmail_token')
```

### 5.2 传统邮箱能力

至少覆盖：

- 发送邮件：收件人必须是 `@smail.com`。
- 收件箱能看到收到的邮件。
- 点击邮件后右侧详情正常展示。
- 标记已读后邮件不能从 Inbox 消失。
- 删除邮件后第一次应进入 Trash。
- Trash 中能看到被删除邮件。
- 搜索邮件关键词能返回结果。

推荐测试数据：先注册两个用户，或者给自己发邮件。

```text
from/to: demo1@smail.com
subject: Google Ads Demo
body: Google Ads promotion. This is an advertisement email for demo deletion.
```

### 5.3 当前邮件 Agent 场景

1. 打开 Inbox。
2. 点击一封邮件。
3. 右侧应显示 Agent 对话区。
4. 输入：

```text
这封邮件需要我做什么？
```

预期：

- Agent 返回摘要/说明。
- Network 中前端只请求 `/api/v1/agent/sessions/**`。
- 后端会转发到 Agent `/plugin/v1/agent/chat`。

### 5.4 Agent 写操作确认场景

打开广告测试邮件后输入：

```text
帮我把这封广告邮件删掉
```

预期：

- Agent 不应直接删除。
- 前端应显示待确认动作。
- 点击确认后，后端执行写操作。
- 邮件移动到 Trash。

### 5.5 全局 Agent 搜索场景

点击侧边栏 SmartMail 助手入口，输入：

```text
帮我搜索 Google 的邮件
```

预期：

- Agent 返回相关邮件结果或说明。
- 如果有 pending action，必须走确认后才执行。

### 5.6 AI 关闭场景

方式 A：从前端设置中关闭 AI。

方式 B：重启后端前设置：

```powershell
$env:SMARTMAIL_AI_ENABLED = "false"
```

预期：

- 传统邮箱功能仍可用：注册、登录、收发、搜索、删除、Trash。
- Agent 对话返回 disabled/fallback 提示。
- 前端不应因为 Agent 不可用而崩溃。

---

## 6. PowerShell API Smoke 可选脚本

在 Agent 和后端均启动后，可用下面脚本快速验证核心链路。

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

## 7. 常用测试账号说明

当前开发环境使用 H2 内存数据库，后端重启后数据会重置。建议每次测试直接注册新账号。

邮箱格式必须是：

```text
任意名称@smail.com
```

不允许：

```text
xxx@gmail.com
xxx@smartmail.local
xxx@example.com
```

---

## 8. 常用 API 速查

### 注册

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/register" -Method Post -ContentType 'application/json' -Body '{"email":"demo@smail.com","username":"Demo","password":"Password123!"}'
```

### 登录

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body '{"email":"demo@smail.com","password":"Password123!"}'
```

### 工作区视图

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/workspace/views" -Headers @{Authorization="Bearer YOUR_TOKEN"}
```

### 邮件列表

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/workspace/mail-items?view=inbox&page=1&pageSize=20" -Headers @{Authorization="Bearer YOUR_TOKEN"}
```

### 创建 Agent 会话

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/agent/sessions" -Method Post -Headers @{Authorization="Bearer YOUR_TOKEN"} -ContentType 'application/json' -Body '{"scope":"GLOBAL","context":{}}'
```

---

## 9. 故障排查

| 现象 | 处理 |
| --- | --- |
| 8080 被占用 | `netstat -ano | findstr :8080`，然后 `taskkill /PID <PID> /F` |
| 8000 被占用 | `netstat -ano | findstr :8000`，然后 `taskkill /PID <PID> /F` |
| 5173 被占用 | Vite 会提示新端口；建议先释放 5173，避免代理测试混乱 |
| Agent 返回 disabled | 确认后端启动前设置了 `SMARTMAIL_AI_ENABLED=true`，并在前端设置中开启 AI |
| Agent 调用失败 | 确认 Agent 8000 已启动，后端 `SMARTMAIL_AGENT_BASE_URL=http://127.0.0.1:8000` |
| 前端 API 失败 | 确认后端 8080 已启动，检查 `frontend/vite.config.ts` 的 `/api` 代理 |
| 注册失败 | 邮箱必须是 `@smail.com`，密码不要为空 |
| 邮件发送失败 | 收件人必须是已注册的 `@smail.com` 用户；未注册用户会 delivery failed |
| Inbox 阅读后邮件消失 | 这是阻塞 bug；当前集成分支预期不会发生，请记录 Network 和后端日志 |
| 删除后 Trash 看不到 | 这是阻塞 bug；当前集成分支预期第一次删除进入 Trash |
| npm audit 报漏洞 | 非启动阻塞；先记录，后续单独升级依赖 |

---

## 10. 测试前最终检查清单

```text
☐ Agent 8000 已启动，/plugin/v1/health 返回 UP
☐ Backend 8080 已启动，/actuator/health 返回 UP
☐ Backend 启动前已设置 SMARTMAIL_AI_ENABLED=true
☐ Frontend 5173 已启动
☐ 浏览器打开 http://127.0.0.1:5173
☐ Network 中前端只请求 /api/v1/**
☐ 注册邮箱使用 @smail.com
☐ 能发送邮件到已注册用户或自己
☐ 当前邮件 Agent 能回答
☐ Agent 写操作先出现确认按钮，确认后才执行
☐ AI 关闭后传统邮箱仍能使用
```
