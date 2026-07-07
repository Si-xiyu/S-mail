# SmartMail Agent 对接与调试指南

本文面向前端、后端和 Agent 联调。当前约束：前端只调用 Spring Boot；Spring Boot 是权限和持久化中心；Python Agent 不直接访问数据库，只通过后端 Internal Tool API 读取或生成待确认动作。

## 1. 前端调用的后端公开 API

### 创建 Agent 会话

```http
POST /api/v1/agent/sessions
Authorization: Bearer <jwt>
Content-Type: application/json
```

请求：

```json
{
  "scope": "GLOBAL",
  "context": {}
}
```

当前邮件右栏场景使用：

```json
{
  "scope": "CURRENT_MAIL",
  "context": { "mailItemId": 88 }
}
```

响应 `data`：

```json
{
  "sessionId": "uuid",
  "scope": "CURRENT_MAIL",
  "context": { "mailItemId": 88 },
  "status": "ACTIVE",
  "messages": [],
  "pendingActions": []
}
```

### 发送用户消息

```http
POST /api/v1/agent/sessions/{sessionId}/messages
Authorization: Bearer <jwt>
Content-Type: application/json
```

请求：

```json
{ "message": "帮我搜索 Google 的邮件" }
```

响应 `data`：

```json
{
  "sessionId": "uuid",
  "status": "SUCCEEDED",
  "answer": "找到以下相关邮件...",
  "assistantMessage": {
    "role": "ASSISTANT",
    "content": "找到以下相关邮件...",
    "status": "SUCCEEDED",
    "toolCalls": []
  },
  "pendingActions": []
}
```

写操作示例：用户输入 `帮我把广告邮件删掉`。后端会让 Agent 搜索候选邮件，返回待确认动作：

```json
{
  "pendingActions": [
    {
      "actionId": "uuid:88:MOVE",
      "type": "MOVE",
      "label": "移入回收站: Big discount ad",
      "payload": {
        "mailItemId": 88,
        "userId": 1,
        "folder": "TRASH",
        "action": "MOVE"
      },
      "status": "PENDING",
      "execution": "BACKEND_REQUIRED"
    }
  ]
}
```

### 确认执行待确认动作

```http
POST /api/v1/agent/actions/{actionId}/confirm
Authorization: Bearer <jwt>
Content-Type: application/json
```

请求：

```json
{ "confirmed": true }
```

后端会校验 action 归属当前用户，然后调用 `InternalToolService.executeAction`。`confirmed=false` 会把动作标记为 `CANCELLED`。

## 2. 后端调用 Agent Plugin API

后端调用 Python Agent 时携带：

```http
X-Plugin-Token: smartmail-agent-plugin-dev-token
```

主要接口：

```http
GET  /plugin/v1/health
POST /plugin/v1/analysis/mail
POST /plugin/v1/agent/chat
POST /plugin/v1/agent/actions/execute
```

`POST /plugin/v1/agent/chat` 的请求由后端生成，核心字段：

```json
{
  "sessionId": "uuid",
  "userId": 1,
  "scope": "GLOBAL",
  "message": "帮我把广告邮件删掉",
  "context": {},
  "toolPolicy": { "agentAutoWriteEnabled": false },
  "pluginConfig": { "aiPluginEnabled": true }
}
```

Agent Plugin 返回 `pendingActions`，但不直接写数据库。

## 3. Agent 调用后端 Internal Tool API

Agent 调用后端时携带：

```http
X-Internal-Token: smartmail-internal-dev-token
```

当前使用的 Internal Tool：

```http
GET  /internal/v1/tools/mail-items/{itemId}/context?userId=1
GET  /internal/v1/tools/mail-search?userId=1&keyword=Google&limit=5
POST /internal/v1/tools/analysis-results
POST /internal/v1/tools/mail-actions/execute
```

支持的写操作白名单：

- `MARK_READ`
- `MARK_UNREAD`
- `SET_READ`
- `STAR`
- `UNSTAR`
- `MOVE_TO_JUNK`
- `MOVE`，`folder` 只能是 `INBOX`、`JUNK`、`TRASH`
- `SET_PRIORITY`，值只能是 `LOW`、`NORMAL`、`HIGH`、`URGENT`
- `SET_CATEGORY`

## 4. Provider 配置

Agent 的自动分析和右栏聊天使用分离配置。复制示例文件：

```powershell
cd agent
Copy-Item config/providers.example.toml config/providers.toml
```

`config/providers.toml` 示例：

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

密钥不写入配置文件，使用环境变量引用，风格类似 Claude Code / Codex 的自定义 Provider 配置：配置文件保存 provider、base_url、model 和 env 名称；真实 token 从环境变量读取。

PowerShell：

```powershell
$env:SMARTMAIL_AGENT_CONFIG = "./config/providers.toml"
$env:DEEPSEEK_API_KEY = "sk-..."
```

也可以覆盖单个通道：

```powershell
$env:SMARTMAIL_AGENT_ANALYSIS_PROVIDER = "deepseek"
$env:SMARTMAIL_AGENT_ANALYSIS_MODEL = "deepseek-chat"
$env:SMARTMAIL_AGENT_CHAT_PROVIDER = "deepseek"
$env:SMARTMAIL_AGENT_CHAT_MODEL = "deepseek-chat"
```

DeepSeek 使用 OpenAI-compatible Chat Completions：`base_url=https://api.deepseek.com`，模型常用 `deepseek-chat`。该接口无服务端会话状态，Agent 每次请求都会传入当前邮件或搜索结果上下文。

## 5. 本地联调步骤

### 启动后端

```powershell
cd backend
mvn spring-boot:run
```

默认：`http://localhost:8080`。

### 启动 Agent

```powershell
cd agent
pip install -r requirements.txt
$env:SMARTMAIL_BACKEND_BASE_URL = "http://localhost:8080"
$env:SMARTMAIL_INTERNAL_TOKEN = "smartmail-internal-dev-token"
$env:SMARTMAIL_PLUGIN_TOKEN = "smartmail-agent-plugin-dev-token"
uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```

### 开启后端 AI Plugin

`backend/src/main/resources/application-dev.yml` 当前默认 `smartmail.ai.enabled=false`。要联调 Agent，把它改为 true 或通过环境变量覆盖：

```powershell
$env:SMARTMAIL_AI_ENABLED = "true"
$env:SMARTMAIL_AGENT_BASE_URL = "http://localhost:8000"
$env:SMARTMAIL_AGENT_PLUGIN_TOKEN = "smartmail-agent-plugin-dev-token"
```

### 调试顺序

1. 注册/登录拿 JWT。
2. `POST /api/v1/agent/sessions` 创建 `GLOBAL` 或 `CURRENT_MAIL` 会话。
3. `POST /api/v1/agent/sessions/{sessionId}/messages` 发送：
   - `帮我搜索 Google 的邮件`
   - `帮我把广告邮件删掉`
   - `这封邮件需要我做什么？`
4. 如果返回 `pendingActions`，调用 `POST /api/v1/agent/actions/{actionId}/confirm`。
5. 到邮箱列表确认移动、已读、优先级或分类变化。

## 6. AI 关闭时的预期

当 `SMARTMAIL_AI_ENABLED=false` 或用户设置 `aiEnabled=false`：

- 后端公开 Agent message API 返回 `DISABLED`。
- 后端不调用 Python Agent。
- 注册、登录、收发、搜索、附件、文件夹等传统邮箱能力保持可用。