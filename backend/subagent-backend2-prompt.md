# Backend Subagent 2 Prompt - Round 1

You are backend subagent 2 for SmartMail. Work only in:

- Worktree: `E:\Code\Smail-worktree\backend2`
- Branch: `backend/dev2`

You are not alone in the codebase. Another backend worker is editing a separate worktree/branch in parallel. Do not revert unrelated changes. Keep your write scope focused and avoid broad rewrites.

## Required Context

Read these files first:

- `CONTEXT.md`
- `项目架构与技术选型.md`
- `SmartMail项目全貌.md`
- `docs/README.md`
- `docs/api/mvp-api.md`
- `docs/design/mvp-architecture.md`
- `backend/pom.xml`
- `backend/src/main/resources/schema.sql`

Current product boundary:

- MVP mail delivery is only between SmartMail system users on the same server.
- Do not implement real SMTP/IMAP/POP3 as the main path.
- Frontend should consume Workspace View Models instead of raw table-shaped CRUD.
- Agent does not read the database directly; it uses backend Internal Tool API.
- AI analysis is asynchronous and must not block delivery.

## Ownership

Primary responsibility:

- Workspace API.
- Mailbox management API alignment.
- Search and Junk/folder movement.
- Notification polling/event foundation.
- Internal Tool API contract alignment.
- AI analysis task/result service boundary and Agent analysis invocation.

Avoid owning:

- Attachment upload/download implementation.
- Pending attachment binding implementation.
- User setting/category schema except for reading fields needed by your slice.

If you must touch shared files, keep changes minimal and document them.

## Target Work

Implement a focused backend slice that moves current APIs toward the MVP contract:

1. Workspace API
   - Add `GET /api/v1/workspace/views`.
   - Add `GET /api/v1/workspace/mail-items`.
   - Add `GET /api/v1/workspace/mail-items/{itemId}`.
   - Return Workspace View Models with item id, mail id, folder, sender, subject, summary preview, category if available, analysis status, read/starred/priority/hasAttachment/receivedAt.
   - Detail response should use `itemId` as the access boundary, not raw `mailId`.

2. Mailbox management
   - Align APIs with docs where practical:
     - `PATCH /api/v1/mailbox/items/{itemId}/read`
     - `PATCH /api/v1/mailbox/items/{itemId}/star`
     - `POST /api/v1/mailbox/items/{itemId}/move`
     - `DELETE /api/v1/mailbox/items/{itemId}`
   - Implement move to `INBOX`, `JUNK`, `TRASH`, `SENT` only where valid.
   - Preserve current delete semantics: first delete moves to Trash, deleting from Trash hides the current user's item.
   - Add keyword search support in workspace list if feasible.

3. Internal Tool API
   - Add or align:
     - `GET /internal/v1/tools/mail-items/{itemId}/context`
     - `GET /internal/v1/tools/mail-search`
     - `POST /internal/v1/tools/analysis-results`
     - `POST /internal/v1/tools/mail-actions`
   - Keep existing internal token check.
   - Read tools must enforce user/item ownership through mailbox items.
   - Write tool actions should be narrowly scoped, e.g. mark read, star, move to junk, set priority.

4. AI analysis task boundary
   - Add schema/entity/mapper/service if useful for `ai_analysis_task`.
   - After mail delivery, provide a service method that can enqueue or mark analysis pending for recipient mailbox items.
   - If calling external Agent is too much for this round, keep a clear adapter boundary and fallback status.
   - Do not block mail delivery.

5. Notification foundation
   - Add a lightweight polling endpoint if feasible, e.g. unread/new mail counts since timestamp.
   - Avoid Redis/SSE/WebSocket for this round.

6. Tests
   - Add focused tests if the project test setup is usable.
   - At minimum run `mvn test` or document why it cannot be run.

## Constraints

- Keep Spring Boot layering clear: controller/service/mapper/entity/dto/config.
- Use DTO responses; do not expose entities.
- All writes must be scoped to the authenticated user or valid internal token plus explicit user id.
- Do not introduce Redis, MQ, vector DB, or external mail protocol dependency.
- Do not modify frontend or agent code.
- Do not make broad rewrites of authentication or mail sending.

## Final Response

Report:

- Files changed.
- API endpoints added or changed.
- Tests run and result.
- Risks or integration notes for the main architect.
