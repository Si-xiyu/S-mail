# SmartMail Agent Demo Integration Status

## Branch

- Integration branch: `fullstack/agent-demo-integration`
- Base branch: `feature/agent-mvp-demo`
- Integrated branches:
  - `backend/agent-api-hardening`
  - `feature/agent-frontend-integration`
  - `docs/agent-integration-final`

## Goal

Deliver a runnable full-stack demo for the user-facing SmartMail scenario:

1. Traditional mail features remain usable when AI is disabled.
2. Frontend calls only Spring Boot public APIs.
3. Spring Boot owns authentication, permissions, persistence, and confirmed write actions.
4. Agent Plugin exposes smart capabilities and calls only backend Internal Tool APIs.
5. Right-panel Agent supports current-mail and global mailbox interaction.

## Integrated capabilities

- Backend Agent public APIs:
  - `POST /api/v1/agent/sessions`
  - `GET /api/v1/agent/sessions/{sessionId}`
  - `POST /api/v1/agent/sessions/{sessionId}/messages`
  - `POST /api/v1/agent/actions/{actionId}/confirm`
- Backend Internal Tool APIs for Agent:
  - `GET /internal/v1/tools/mail-items/{itemId}/context`
  - `GET /internal/v1/tools/mail-search`
  - `POST /internal/v1/tools/analysis-results`
  - `POST /internal/v1/tools/mail-actions/execute`
- Frontend Agent workspace panel:
  - global Agent entry from sidebar
  - current-mail Agent panel when a mail is selected
  - pending action confirm/cancel controls
  - AI disabled-state fallback
- Settings integration:
  - frontend uses `PATCH /api/v1/users/me/settings`
  - backend combines global `SMARTMAIL_AI_ENABLED` with per-user `aiEnabled`
- Agent Plugin:
  - plugin health, automatic analysis, chat, and delegated action contracts are present
  - DeepSeek/provider configuration remains separate for analysis and chat

## Integration fix added in this branch

`AgentSessionService` now generates a backend action id when the Agent Plugin returns a pending action without `actionId`.

Reason: `agent-plugin-api.md` allows pending actions to omit `actionId`, while frontend confirmation requires an action id returned by the backend. The backend now preserves plugin-provided ids and fills missing ids with a stable backend-generated value.

## Validation

Commands run from `fullstack/agent-demo-integration`:

```powershell
cd backend
mvn clean test
```

Result: 35 tests passed, 0 failures.

```powershell
cd agent
E:\software\Miniconda\python.exe -m pytest
```

Result: 33 tests passed, 0 failures, 1 Starlette/httpx deprecation warning.

```powershell
cd frontend
npm.cmd install
npm.cmd run build
```

Result: build passed. Vite reported non-blocking Rollup annotation warnings and bundle-size warning.

## HTTP smoke result

Runtime smoke was executed with:

- Agent: `http://127.0.0.1:8000`
- Backend: `http://127.0.0.1:8080`
- Backend AI enabled via environment:
  - `SMARTMAIL_AI_ENABLED=true`
  - `SMARTMAIL_AGENT_BASE_URL=http://127.0.0.1:8000`
  - `SMARTMAIL_AGENT_PLUGIN_TOKEN=smartmail-agent-plugin-dev-token`

Covered user scenario:

1. Register `@smail.com` user.
2. Enable AI setting for the user.
3. Send a demo mail to self.
4. Verify the mail appears in Inbox workspace list.
5. Create `CURRENT_MAIL` Agent session with `mailItemId`.
6. Ask current-mail question.
7. Ask Agent to delete the advertisement mail.
8. Confirm returned pending action.
9. Verify the item appears in Trash.

Observed smoke result:

```json
{
  "currentMailAgentStatus": "SUCCEEDED",
  "pendingActionType": "MOVE",
  "confirmStatus": "EXECUTED",
  "trashContainsItem": true
}
```

## Known non-blocking issues

- `npm install` reports 4 dependency audit findings. This does not block the demo build but should be reviewed separately.
- Frontend production bundle exceeds Vite's default 500 kB warning threshold. This is not a functional blocker for MVP demo.
- Runtime smoke was done through HTTP API. Manual browser UI smoke is still recommended before presentation.
- A temporary local directory `agent/.tmp/` may exist in the worktree from runtime logging; it is not tracked by Git.

## Handoff recommendation

Use `fullstack/agent-demo-integration` as the candidate demo integration branch. If manual UI smoke passes, merge it back into the release/demo branch selected by the team.