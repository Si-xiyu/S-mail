# Subagent Prompt: agent/dev2

You are working in worktree `E:\Code\Smail-worktree` on branch `agent/dev2`.

Your scope is **only the `agent/` service** unless you need to read docs for context. Do not modify frontend or backend code. If you need temporary notes, place them under `agent/`.

## Goal

Implement the interactive Agent Plugin side of SmartMail according to:

- `docs/api/agent-plugin-api.md`
- `docs/api/mvp-api.md`
- `docs/design/mvp-architecture.md`
- `CONTEXT.md`

This branch should focus on:

- current-mail chat endpoint
- global-agent chat endpoint behavior through the same API
- tool-router structure
- mock RAG Tool boundary
- pending write actions instead of direct writes

Do not implement automatic mail analysis in this branch except where shared schemas are needed. Avoid conflicting with `agent/dev1`.

## Required Behavior

Implement or refactor the Agent service so it exposes:

```http
POST /plugin/v1/agent/chat
```

The endpoint must accept the documented request shape:

- `sessionId`
- `userId`
- `scope`: `CURRENT_MAIL` or `GLOBAL`
- `message`
- `context`
- `toolPolicy`
- `pluginConfig`

It must return:

- `status`
- `answer`
- `toolCalls`
- `pendingActions`

## Tool Router Requirements

Build a small, explicit tool-router layer with these conceptual tools:

1. `mail_context_tool`
   - For `CURRENT_MAIL`.
   - Calls backend Internal Tool API if enough context is provided, or returns a structured fallback when backend is unavailable and mock is enabled.
   - Must not access database directly.

2. `rag_tool`
   - For `GLOBAL`.
   - MVP implementation may be mock.
   - Preserve the boundary for future BM25 + vector + RRF implementation.
   - Response should identify `source: "MOCK"` for mock records.

3. `mail_action_tool`
   - Does not directly execute writes in this branch.
   - Produces `pendingActions` for actions such as:
     - `SET_PRIORITY`
     - `SET_CATEGORY`
     - `MOVE_TO_JUNK`
     - `MARK_READ`
   - If `agentAutoWriteEnabled=false`, always return pending actions.
   - If `agentAutoWriteEnabled=true`, still only allow whitelisted actions and clearly mark whether execution is delegated to backend. Do not mutate data locally.

## Chat Behavior

For `CURRENT_MAIL`:

- Use current mail context as the primary context.
- Answer questions like:
  - “这封邮件需要我做什么？”
  - “帮我生成回复思路。”
  - “这封邮件安全吗？”
- If the user asks to modify mail state, return a pending action rather than pretending it was done.

For `GLOBAL`:

- Route retrieval-like questions to `rag_tool`.
- Since MVP RAG is mock, return a useful answer that states it is based on available/mock retrieval context.
- Do not claim full mailbox retrieval is implemented.

Plugin disabled behavior:

- If `pluginConfig.aiPluginEnabled=false`, return:

```json
{
  "status": "DISABLED",
  "message": "AI Plugin is disabled"
}
```

Use the documented schema shape consistently; if you include `message`, also keep the API predictable for backend integration.

## Implementation Constraints

- Preserve frontend/backend decoupling: frontend never calls Agent directly.
- Agent must not access database.
- Backend Internal Tool API calls must use configured base URL and internal token.
- Avoid heavy LangChain dependency unless already present. A small in-project router is enough for MVP.
- Keep mock behavior explicit and deterministic.
- Avoid changing endpoint behavior for automatic analysis; that is `agent/dev1`.

## Suggested File Areas

Likely files to inspect/edit:

- `agent/app/main.py`
- `agent/app/api/routes.py`
- `agent/app/schemas/agent.py`
- `agent/app/tools/backend_tools.py`
- `agent/app/services/agent_loop.py`
- `agent/app/core/config.py`
- `agent/tests/`

You may add new files under:

- `agent/app/services/tool_router.py`
- `agent/app/services/rag_tool.py`
- `agent/app/schemas/plugin.py`

Coordinate names carefully so merge conflicts with `agent/dev1` are easy to resolve.

## Tests / Verification

Add focused tests if test infrastructure is present. At minimum verify:

- disabled plugin returns `DISABLED`.
- current-mail chat returns an answer and uses `mail_context_tool`.
- global chat uses `rag_tool` and marks mock source.
- write-intent message returns `pendingActions`.
- no tool directly mutates local data.

Run the lightest available verification, for example:

```powershell
cd agent
python -m compileall app
```

If adding tests, document how to run them.

## Deliverable

At the end, report:

- changed files
- implemented endpoint behavior
- example current-mail chat response
- example global chat response
- verification command and result
- any assumptions for the merge owner

Do not merge branches yourself. The merge owner will merge `agent/dev2`.
