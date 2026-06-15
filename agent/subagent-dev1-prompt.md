# Subagent Prompt: agent/dev1 Round 3

You are working in worktree `E:\Code\Smail-worktree\dev1` on branch `agent/dev1`.

Before coding, sync your branch with `agent/main` from the main repository if needed. Another worker is editing `agent/dev2`, and the merge owner will integrate both. Do not revert unrelated changes. Work only under `agent/`.

## Goal

Harden the automatic-analysis plugin contract so frontend/backend teams can integrate it without reading implementation details.

Round 2 already added structured category responses, provider/fallback modelInfo, disabled-plugin behavior, and deterministic rules fallback. Round 3 focus is documentation-grade contract stability plus tests, not real LLM/network calls.

## Required Changes

### 1. Add/Improve Agent Plugin API Documentation

Update `agent/README.md` with a concise but integration-ready section for:

- `GET /plugin/v1/health`
- `POST /plugin/v1/analysis/mail`
- `POST /plugin/v1/agent/chat` only at a high level, because dev2 owns detailed write-action behavior

Document:

- Basic Mail Mode when `aiPluginEnabled=false`.
- Analysis response fields: `summary`, `category`, `junk`, `priority`, `priorityScore`, `riskLevel`, `riskHints`, `modelInfo`.
- Category object contract, including `{ id, name }`, `{ name }`, `Other`, and `Junk Mail`.
- `modelInfo.provider`, `modelInfo.mode`, `modelInfo.fallbackUsed`.
- The current limit: DeepSeek/Ollama/vector RAG are integration points only; no network calls in this agent prototype.

Keep this README human-readable and useful for frontend/backend developers. Avoid a long essay.

### 2. Add Stable Example Fixtures

Add a small test fixture module or JSON examples under `agent/tests/` that captures representative requests/responses for:

- normal incoming mail
- junk/phishing-like incoming mail
- disabled AI plugin
- category id/name preservation

Prefer Python test fixtures if the current test style makes JSON files unnecessary.

### 3. Expand Analysis Contract Tests

Add focused tests for:

- `modelInfo.provider == "RULES"` when `llmEnabled=false`.
- `modelInfo.provider == "DEEPSEEK"` and fallback mode when `llmEnabled=true` and an API key is present.
- disabled plugin still returns no category and no priority.
- `riskLevel` is only `LOW | MEDIUM | HIGH`.
- priority is only `LOW | NORMAL | HIGH | URGENT` when present.

Do not require network, real DeepSeek, Ollama, vectors, Redis, or database.

## Constraints

- Do not modify frontend/backend.
- Do not implement real LLM calls.
- Do not modify the write-action execution API files unless needed for shared docs wording.
- Preserve `POST /api/v1/agent/tasks`.
- Keep tests deterministic.

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
- documentation/contract changes
- test results
- assumptions for merge owner

Do not merge branches yourself.
