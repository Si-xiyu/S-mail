# Backend Round2 Integration - 2026-06-14

## Branch

- Integration branch: `backend/integrate-round2`
- Base branch: `backend/main`
- Increment source: `origin/feature/backend-mvp-round2`

## Integration Policy

Backend API and backend documentation remain the contract center. Agent provides plugin capability behind backend calls. Frontend should adapt to backend public APIs and should not define backend contracts.

Round2 was treated as an increment, not as a replacement for `backend/main`.

## Stage Commits

- `8a64f49` - `Integrate backend MVP round2 baseline`
  - Resolved the large add/add conflict set.
  - Absorbed round2 attachment, category, search, workspace, analysis scheduler, H2 dev profile, Maven wrapper, and dev run script work.
  - Preserved backend/main user settings, item-scoped analysis task boundary, Internal Tool API surface, backend architect prompt, and previous backend merge logs.
- `aa93d46` - `Align internal tool and attachment contracts`
  - Kept Internal Tool search payload compatible with Agent plugin documentation.
  - Added `/internal/v1/tools/mail-actions/execute` alias while keeping `/internal/v1/tools/mail-actions`.
  - Restored pending attachment validation and binding semantics expected by tests.

## Merged Backend Capabilities

- Attachment upload, deletion, download, pending-to-mail binding, size/empty validation.
- Category CRUD and default `Other` / `Junk Mail` categories.
- User settings retained for AI enablement and Agent auto-write policy.
- Workspace View API:
  - `GET /api/v1/workspace/views`
  - `GET /api/v1/workspace/mail-items`
  - `GET /api/v1/workspace/mail-items/{itemId}`
- Mailbox operations:
  - read/star/delete
  - move folder
  - category assignment
  - search
- Notification polling retained from `backend/main`.
- AI analysis task scheduler from round2, upgraded to item-scoped task records.
- Agent plugin analysis call boundary:
  - backend automatic analysis calls `/plugin/v1/analysis/mail`
  - legacy `/api/v1/agent/tasks` call path is retained only for existing `/api/v1/ai/mails/*` compatibility.
- Internal Tool API retained and aligned:
  - `GET /internal/v1/tools/mail-items/{itemId}/context`
  - `GET /internal/v1/tools/mail-search`
  - `POST /internal/v1/tools/analysis-results`
  - `POST /internal/v1/tools/mail-actions`
  - `POST /internal/v1/tools/mail-actions/execute`

## Frontend Compatibility Notes

Frontend is not yet stable. The backend keeps old public endpoints such as `/api/v1/mailbox` and `/api/v1/mails/{mailId}` while providing the newer Workspace API as the contract target.

Future frontend work should migrate to:

- `GET /api/v1/workspace/views`
- `GET /api/v1/workspace/mail-items`
- `GET /api/v1/workspace/mail-items/{itemId}`
- compose attachment endpoints
- category, search, mailbox operation endpoints

## Verification

- `mvn compile`
  - Passed after escalation for Maven target write access.
- `mvn test`
  - Passed after escalation for Maven target write access.
  - Result: 2 tests run, 0 failures, 0 errors.
- `mvn package`
  - Not completed in this stage because the required escalation request was rejected by the approval reviewer. No package build failure was observed; the command was not run to completion.

## Remaining Risks

- Backend public Agent session APIs (`/api/v1/agent/sessions`) are still not implemented.
- Automatic analysis writes a consolidated `ANALYSIS` result payload; Workspace parsing is still simple and should be hardened.
- `AiController` legacy `/api/v1/ai/mails/*` still calls `/api/v1/agent/tasks` for compatibility. It should be deprecated after frontend migrates.
- More controller/service tests are needed for Workspace, category, search, Internal Tool, and analysis scheduler flows.
