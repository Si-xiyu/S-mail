# Subagent Prompt: agent/dev2 Round 3

You are working in worktree `E:\Code\Smail-worktree\dev2` on branch `agent/dev2`.

Before coding, sync your branch with `agent/main` from the main repository if needed. Another worker is editing `agent/dev1`, and the merge owner will integrate both. Do not revert unrelated changes. Work only under `agent/`.

## Goal

Add the missing Agent-side API contract for executing a write action after the user confirms it.

Round 2 made `POST /plugin/v1/agent/chat` return `pendingActions[]` for write intents. Round 3 should expose an API that frontend/backend can call after user confirmation, while still keeping the Agent decoupled from real backend writes.

## Required Changes

### 1. Add Confirmed Action Execution API

Add a new plugin endpoint, suggested shape:

```http
POST /plugin/v1/agent/actions/execute
```

Suggested request:

```json
{
  "actionId": "s1:88:SET_PRIORITY",
  "userId": 1,
  "confirmed": true,
  "type": "SET_PRIORITY",
  "payload": {
    "mailItemId": 88,
    "priority": "HIGH"
  },
  "pluginConfig": {
    "aiPluginEnabled": true
  },
  "toolPolicy": {
    "agentAutoWriteEnabled": false
  }
}
```

Suggested response:

```json
{
  "status": "DELEGATED",
  "actionId": "s1:88:SET_PRIORITY",
  "execution": "BACKEND_REQUIRED",
  "backendOperation": {
    "method": "POST",
    "path": "/internal/v1/tools/mail-actions/execute",
    "payload": {
      "mailItemId": 88,
      "priority": "HIGH"
    }
  },
  "message": "Action confirmed; backend execution is required."
}
```

The exact schema can differ if there is a cleaner local pattern, but it must express:

- disabled plugin -> no operation
- not confirmed -> rejected/no operation
- confirmed -> delegated backend operation envelope
- no local mail state mutation in Agent

### 2. Preserve Permission Model

Rules:

- Default Agent write behavior remains user-confirmation-first.
- If `agentAutoWriteEnabled=true`, the same execute endpoint may return a delegated backend operation without requiring `confirmed=true`, but it must still not mutate local state.
- Unsupported action types return a clear failed/rejected response.
- Use `mailItemId` in new payloads; accept legacy `mailId`.

### 3. Tests

Add endpoint-level tests for:

- disabled plugin returns `DISABLED` or equivalent no-op status.
- `confirmed=false` and `agentAutoWriteEnabled=false` rejects the operation.
- `confirmed=true` delegates backend operation with `BACKEND_REQUIRED`.
- `agentAutoWriteEnabled=true` delegates without confirmation.
- legacy `mailId` is normalized to `mailItemId`.
- unsupported type is rejected.

Keep status names internally consistent and document them in code/schema names if useful.

## Constraints

- Do not modify frontend/backend.
- Do not perform real backend writes.
- Do not implement real auth or DB persistence.
- Do not change automatic-analysis rule behavior.
- Preserve `POST /api/v1/agent/tasks`.
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
- API/schema changes
- test results
- assumptions for merge owner

Do not merge branches yourself.
