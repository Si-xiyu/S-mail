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

## Plugin API

```http
GET /plugin/v1/health
POST /plugin/v1/analysis/mail
```

Example analysis request:

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

If `aiPluginEnabled` is false, analysis returns `DISABLED`. If LLM is disabled or no API key is configured, the service uses deterministic rules fallback.

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
