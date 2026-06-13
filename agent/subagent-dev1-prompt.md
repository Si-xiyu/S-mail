# Subagent Prompt: agent/dev1

You are working in worktree `E:\Code\Smail-worktree` on branch `agent/dev1`.

Your scope is **only the `agent/` service** unless you need to read docs for context. Do not modify frontend or backend code. If you need temporary notes, place them under `agent/`.

## Goal

Implement the Agent Plugin automatic-analysis side of SmartMail according to the API contract in:

- `docs/api/agent-plugin-api.md`
- `docs/design/course-alignment.md`
- `docs/design/mvp-architecture.md`
- `CONTEXT.md`

The result should let the backend call the Agent Plugin as a decoupled service for:

- health check
- automatic mail analysis
- rules fallback when LLM is disabled or unavailable

Do not implement DeepSeek, Ollama, vector search, or real RAG in this branch. Keep provider boundaries clear so later work can plug them in.

## Required Behavior

Implement or refactor the Agent service so it exposes:

```http
GET  /plugin/v1/health
POST /plugin/v1/analysis/mail
```

The analysis endpoint must accept the request shape documented in `docs/api/agent-plugin-api.md` and return:

- `status`
- `summary`
- `category`
- `junk`
- `priority`
- `priorityScore`
- `riskLevel`
- `riskHints`
- `modelInfo`

It must support these plugin states:

- AI plugin disabled: return `DISABLED` without attempting analysis.
- LLM disabled / no API key: use deterministic rules fallback.
- Rules analysis succeeds: return `SUCCEEDED`.
- Partial failures: return `PARTIAL` where appropriate.

## Rules Fallback Requirements

The rules fallback should cover course-aligned intelligent functions:

- Summary: derive 1-3 short bullet points from subject/content.
- Category: choose from `userCategories`; if no match, return `Other`; if junk, return `Junk Mail` if present.
- Junk detection: suspicious words, phishing-like phrases, repeated promotion words, obvious spam patterns.
- Priority prediction: combine content signals and `behaviorSignals`.
- Security risk hints:
  - URL present in body.
  - suspicious sender wording/domain mismatch signals if available.
  - words about prizes, loans, transfers, passwords, verification codes, urgent click requests.

Priority scoring guideline:

- `LOW`: 0-39
- `NORMAL`: 40-69
- `HIGH`: 70-89
- `URGENT`: 90-100

Behavior signals should influence score:

- frequent/replied sender increases score.
- recently marked junk sender decreases score and may increase junk confidence.

## Implementation Constraints

- Preserve existing service shape where reasonable, but refactor if needed.
- Keep schemas explicit with Pydantic models.
- Avoid hardcoding Chinese-only logic; include mixed Chinese/English keywords where useful.
- Do not let Agent access a database.
- Do not call backend Internal Tool APIs from automatic analysis unless the documented request already contains enough context. The backend is expected to pass the context for this endpoint.
- Do not silently swallow invalid input; return meaningful validation errors through FastAPI/Pydantic.
- Keep output deterministic for tests.

## Suggested File Areas

Likely files to inspect/edit:

- `agent/app/main.py`
- `agent/app/api/routes.py`
- `agent/app/schemas/agent.py`
- `agent/app/services/rule_engine.py`
- `agent/app/services/agent_loop.py`
- `agent/app/core/config.py`
- `agent/tests/`

You may add new files under:

- `agent/app/schemas/`
- `agent/app/services/`
- `agent/app/api/`

## Tests / Verification

Add focused tests if test infrastructure is present. At minimum verify:

- health endpoint returns capabilities.
- analysis returns `DISABLED` when `aiPluginEnabled=false`.
- analysis returns summary/category/junk/priority/risk fields for a normal course-related email.
- spam email maps to `Junk Mail` where available.
- behavior signals influence priority.

Run the lightest available verification, for example:

```powershell
cd agent
python -m compileall app
```

If adding tests, document how to run them.

## Deliverable

At the end, report:

- changed files
- implemented endpoints
- example request/response
- verification command and result
- any assumptions for the merge owner

Do not merge branches yourself. The merge owner will merge `agent/dev1`.
