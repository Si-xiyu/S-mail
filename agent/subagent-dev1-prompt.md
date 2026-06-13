# Subagent Prompt: agent/dev1 Round 2

You are working in worktree `E:\Code\Smail-worktree\dev1` on branch `agent/dev1`.

Before coding, sync your branch with `agent/main` from the main repository if needed. You are not alone in the codebase: another worker is editing `agent/dev2`, and the merge owner will integrate both. Do not revert unrelated changes. Work only under `agent/`.

## Goal

Harden the automatic-analysis Agent Plugin contract after Round 1.

Round 1 implemented:

- `GET /plugin/v1/health`
- `POST /plugin/v1/analysis/mail`
- deterministic rules fallback

Round 2 focus: make automatic-analysis responses match the documented API contract more closely and improve provider/config boundaries without implementing real network LLM calls.

## Required Changes

### 1. Structured Category Response

Current analysis may return `category` as a string. Update it to return a structured object when possible:

```json
{
  "category": {
    "id": 1,
    "name": "课程"
  }
}
```

Rules:

- If `userCategories` contains objects with `id` and `name`, preserve both.
- If `userCategories` contains strings, return at least `{ "name": "..." }`.
- If no match, return `Other` if available.
- If junk, return `Junk Mail` if available.
- Disabled plugin may return `category: null`.

Keep backward-compatible parsing for existing string and dict category inputs.

### 2. Analysis Model Info / Provider Boundary

Create a small provider boundary for analysis, but keep it local and deterministic:

- `RulesAnalysisProvider` or similarly named class can wrap current rule engine.
- `provider` in `modelInfo` should reflect config:
  - `RULES` when `llmEnabled=false`
  - `DEEPSEEK` only if config says LLM enabled and API key exists, but still return rules fallback with `mode: "rules-fallback"` until real LLM is implemented.
- Do not make network calls.
- Clearly expose `fallbackUsed: true` if useful.

### 3. Risk / Priority Contract Cleanup

Keep risk levels within:

- `LOW`
- `MEDIUM`
- `HIGH`

No `CRITICAL`.

Keep priority within:

- `LOW`
- `NORMAL`
- `HIGH`
- `URGENT`

### 4. Tests

Update/add tests under `agent/tests/`:

- category object input preserves id/name.
- string category input still works.
- junk maps to Junk Mail object.
- disabled plugin returns category `null`.
- modelInfo reports fallback mode.

## Constraints

- Do not modify interactive chat/tool router files unless absolutely necessary.
- Do not modify frontend/backend.
- Do not access DB.
- Preserve `POST /api/v1/agent/tasks`.
- Use deterministic rules only.

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
