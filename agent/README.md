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

## Main API

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
