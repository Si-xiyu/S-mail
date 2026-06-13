# SmartMail Agent

Python FastAPI Agent service for SmartMail.

## Run

```powershell
cd agent
python -m venv .venv
.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --host 127.0.0.1 --port 8000
```

## Plugin API Contract

```http
GET /plugin/v1/health
POST /plugin/v1/analysis/mail
POST /plugin/v1/agent/chat
```

### `GET /plugin/v1/health`

Returns the prototype plugin status and advertised capabilities:

```json
{
  "status": "UP",
  "pluginVersion": "0.1.0",
  "capabilities": {
    "rules": true,
    "llm": false,
    "currentMailAgent": true,
    "ragTool": "MOCK"
  }
}
```

### `POST /plugin/v1/analysis/mail`

Runs deterministic mail analysis for frontend/backend integration tests.

```json
{
  "taskId": "task-1",
  "userId": "user-1",
  "mailItemId": "mail-1",
  "mail": {
    "subject": "Urgent review needed",
    "content": "Please review the report by today.",
    "senderEmail": "manager@example.com"
  },
  "userCategories": ["Work", "Junk Mail"],
  "behaviorSignals": {"frequentSender": true, "repliedSender": true},
  "pluginConfig": {"aiPluginEnabled": true, "llmEnabled": false}
}
```

Successful response fields:

- `summary`: up to three short strings derived from subject/body.
- `category`: category object or `null`. Returned objects use `{ "id": <id or null>, "name": "<name>" }`.
- `junk`: boolean junk/phishing-like classification.
- `priority`: `LOW`, `NORMAL`, `HIGH`, `URGENT`, or `null` when analysis is disabled.
- `priorityScore`: integer score from 0 to 100.
- `riskLevel`: `LOW`, `MEDIUM`, or `HIGH`.
- `riskHints`: list of short risk indicators.
- `modelInfo`: provider metadata for the analysis path.

Category input accepts strings and objects. String inputs such as `"Work"` return
`{ "id": null, "name": "Work" }`. Object inputs preserve `id` and `name`, for
example `{ "id": 7, "name": "Project" }`. `{ "name": "Other" }` is the normal
fallback category. `"Junk Mail"` is selected when junk/spam-like content is
detected and that category is available.

`modelInfo.provider` is `RULES` when `llmEnabled=false` or no API key is present.
When `llmEnabled=true` and an API key is provided, `modelInfo.provider` reports
`DEEPSEEK`, but `modelInfo.mode` remains `rules-fallback` and
`modelInfo.fallbackUsed` is `true`. DeepSeek, Ollama, vector RAG, Redis, and
database integrations are contract placeholders in this agent prototype; the
analysis endpoint does not make network calls.

Basic Mail Mode: when `pluginConfig.aiPluginEnabled=false`, the endpoint returns
`status: "DISABLED"` with `summary: []`, `category: null`, `priority: null`,
`priorityScore: 0`, `riskLevel: "LOW"`, and no risk hints. This lets the mail
client continue without AI-derived labels or priority.

### `POST /plugin/v1/agent/chat`

High-level chat entry point for current-mail and global mail assistance. It
returns an `answer`, `toolCalls`, and optional `pendingActions`. Write-action
execution remains backend-owned: this prototype reports backend-required pending
actions and preserves `POST /api/v1/agent/tasks` for the legacy task API.

## Legacy Agent API

```http
POST /api/v1/agent/tasks
```

Request:

```json
{
  "mailId": 1,
  "userId": 1,
  "task": "summary"
}
```

Supported tasks:

- `summary`
- `reply_draft`
- `analyze`

The Agent calls Spring Boot internal tools to fetch mail context, save AI results, and update priority. If the backend is unavailable, mock fallback is enabled by default for local demos.

## Verify

```powershell
cd agent
python -m compileall app
python -m unittest discover -s tests
```
