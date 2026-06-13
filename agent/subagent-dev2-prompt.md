# Subagent Prompt: agent/dev2 Round 2

You are working in worktree `E:\Code\Smail-worktree\dev2` on branch `agent/dev2`.

Before coding, sync your branch with `agent/main` from the main repository if needed. You are not alone in the codebase: another worker is editing `agent/dev1`, and the merge owner will integrate both. Do not revert unrelated changes. Work only under `agent/`.

## Goal

Harden the interactive Agent Plugin contract after Round 1.

Round 1 implemented:

- `POST /plugin/v1/agent/chat`
- ToolRouter
- current-mail context tool
- mock RAG tool
- pending write actions

Round 2 focus: make chat responses and backend internal-tool boundaries easier for backend/frontend integration.

## Required Changes

### 1. Pending Action Shape

Update pending actions to include integration-friendly fields:

```json
{
  "actionId": "local-generated-id-or-null",
  "type": "SET_PRIORITY",
  "label": "标记为 HIGH",
  "payload": { "mailItemId": 88, "priority": "HIGH" },
  "reason": "user confirmation is required",
  "status": "PENDING",
  "execution": "BACKEND_REQUIRED"
}
```

Rules:

- `actionId` may be a deterministic local ID for now.
- `label` should be user-facing and concise.
- Prefer `mailItemId` over `mailId` for new plugin flow.
- Keep compatibility with context containing `mailId` by mapping it into `mailItemId` if no `mailItemId` exists.
- Top-level chat status remains `SUCCEEDED` when pending actions are produced.

### 2. Backend Internal Tool API Path

Update current-mail context retrieval to prefer the documented new API:

```http
GET /internal/v1/tools/mail-items/{itemId}/context
```

Fallback to the old endpoint only if needed for current prototype compatibility:

```http
GET /internal/v1/tools/mails/{mailId}?userId=...
```

Do not remove old compatibility yet.

### 3. Endpoint-Level Tests

Add tests using FastAPI `TestClient` for:

- `POST /plugin/v1/agent/chat` disabled plugin.
- current-mail chat returns `SUCCEEDED`.
- global chat uses mock RAG.
- write intent returns `pendingActions` with `actionId`, `label`, `mailItemId`, and status `PENDING`.

Keep existing service-level tests if useful.

### 4. Contract Consistency

Ensure status values match the documented plugin contract:

- `SUCCEEDED`
- `FAILED`
- `DISABLED`
- `PARTIAL`

Do not reintroduce `SUCCESS` or `NEEDS_ACTION` as top-level response statuses.

## Constraints

- Do not modify automatic-analysis rule behavior unless needed for shared schema compatibility.
- Do not modify frontend/backend.
- Do not access DB.
- Preserve legacy `POST /api/v1/agent/tasks`.
- RAG remains mock.

## Verification

Run:

```powershell
cd agent
$env:PYTHONPYCACHEPREFIX=$env:TEMP
E:\software\Miniconda\python.exe -m compileall app
E:\software\Miniconda\python.exe -B -m unittest discover -s tests
```

## Final Response

Report:

- changed files
- contract changes
- test results
- assumptions for merge owner

Do not merge branches yourself.
