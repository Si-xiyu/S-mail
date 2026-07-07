# SmartMail Agent

Python FastAPI Agent service for SmartMail. It is optional: when the backend AI switch is off, SmartMail still runs as a traditional mailbox.

## Run with project script

Recommended local startup from repository root:

```powershell
.\scripts\start-agent.ps1
```

If you need to select a specific Python executable:

```powershell
.\scripts\start-agent.ps1 -Python "E:\software\Miniconda\python.exe"
```

The script automatically:

- creates `agent/.env` from `.env.example` if missing;
- creates `agent/config/providers.toml` from `providers.example.toml` if missing;
- starts Uvicorn with `--env-file .env`;
- serves the plugin at `http://127.0.0.1:8000`.

## Local config

Put runtime values and secrets in `agent/.env`:

```env
SMARTMAIL_BACKEND_BASE_URL=http://localhost:8080
SMARTMAIL_INTERNAL_TOKEN=smartmail-internal-dev-token
SMARTMAIL_PLUGIN_TOKEN=smartmail-agent-plugin-dev-token
SMARTMAIL_AGENT_CONFIG=./config/providers.toml
DEEPSEEK_API_KEY=sk-...
SMARTMAIL_AGENT_RAG_MODE=BACKEND
SMARTMAIL_AGENT_MOCK_ON_TOOL_ERROR=false
SMARTMAIL_LOG_LEVEL=INFO
```

`config/providers.toml` stores provider metadata and points to the environment variable that contains the secret:

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

Do not commit `.env` or `config/providers.toml`.

## Manual run fallback

If you do not use the project script:

```powershell
cd agent
python -m pip install -r requirements.txt
python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000 --env-file .env
```

## Plugin API Contract

```http
GET /plugin/v1/health
POST /plugin/v1/analysis/mail
POST /plugin/v1/agent/chat
POST /plugin/v1/agent/actions/execute
```

Backend calls these endpoints with:

```http
X-Plugin-Token: smartmail-agent-plugin-dev-token
```

Agent calls backend internal tools with:

```http
X-Internal-Token: smartmail-internal-dev-token
```

### Automatic analysis

`POST /plugin/v1/analysis/mail` generates summary, category, junk, priority and risk hints. If DeepSeek is unavailable or no key is configured, the rules provider is used as fallback.

### Interactive chat

`POST /plugin/v1/agent/chat` supports:

- current-mail Q&A, using `context.mailItemId`;
- global mailbox search, using backend `/internal/v1/tools/mail-search`;
- write intents, returned as `pendingActions` only.

Example write request: `帮我把广告邮件删掉` searches matching messages and returns `MOVE` actions with `folder=TRASH`. The Agent does not mutate backend state directly; frontend must ask backend to confirm the action.

## Backend public API

Frontend should call Spring Boot, not this service directly:

```http
POST /api/v1/agent/sessions
GET  /api/v1/agent/sessions/{sessionId}
POST /api/v1/agent/sessions/{sessionId}/messages
POST /api/v1/agent/actions/{actionId}/confirm
```

See `docs/api/agent-integration.md` for the full contract and manual debugging guide.

## Legacy Agent API

```http
POST /api/v1/agent/tasks
```

Kept for older AI task endpoints. New UI integration should use the backend public Agent session API.

## Verify

```powershell
cd agent
python -B -m pytest -p no:cacheprovider
```
