# Backend Subagent 1 Prompt - Round 1

You are backend subagent 1 for SmartMail. Work only in:

- Worktree: `E:\Code\Smail-worktree\backend1`
- Branch: `backend/dev1`

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
- Attachments are MVP required.
- Attachments use "upload while composing, bind on send".
- Draft sync is post-MVP; local autosave is frontend-owned for MVP.
- AI analysis is asynchronous and must not block delivery.

## Ownership

Primary responsibility:

- Attachment module.
- Pending attachment persistence.
- Send-mail binding of pending attachments.
- User settings for AI enablement / Agent auto-write switch.
- Category base model, default categories, and schema support.

Avoid owning:

- Workspace list/detail controller implementation.
- Search API implementation.
- Internal Tool API endpoint contract changes.
- AI analysis task execution service.

If you must touch shared files, keep changes minimal and document them.

## Target Work

Implement a focused backend slice that supports MVP attachment and category/user-setting foundations:

1. Schema
   - Add `pending_attachment`.
   - Ensure `mail_attachment` can represent bound attachments.
   - Add `mail_category` and `mail_category_assignment`.
   - Add `user_setting`.
   - Keep H2/MySQL-compatible SQL as much as practical.

2. Attachment module
   - Add entity, mapper, DTOs, service, controller for:
     - `POST /api/v1/compose/attachments`
     - `DELETE /api/v1/compose/attachments/{pendingAttachmentId}`
     - `GET /api/v1/attachments/{attachmentId}/download`
   - Store uploaded files under configurable local storage, defaulting to `storage/attachments`.
   - Validate basic file size and empty file.
   - Pending attachments belong to uploader and can only be removed by uploader before binding.
   - Download requires the current user to have a visible mailbox item for the attachment's mail, or be the sender/uploader with a visible sent item.

3. Send integration
   - Extend `SendMailRequest` to accept `pendingAttachmentIds`.
   - In `MailService.send`, bind only the current user's pending attachments to the new `mail_message`.
   - Set `mail_message.has_attachment` correctly.
   - Preserve current successful send behavior.

4. User settings and category foundations
   - Add minimal entity/mapper/service support for `user_setting`.
   - Add minimal category entity/mapper/service support.
   - On registration or first access, ensure default categories can exist: `Other`, `Junk Mail`.
   - Keep public APIs minimal if needed; do not overbuild.

5. Tests
   - Add focused tests if the project test setup is usable.
   - At minimum run `mvn test` or document why it cannot be run.

## Constraints

- Keep Spring Boot layering clear: controller/service/mapper/entity/dto/config.
- Use DTO responses; do not expose entities.
- All writes must be scoped to the authenticated user.
- Do not introduce Redis, MQ, vector DB, or external mail protocol dependency.
- Do not modify frontend or agent code.
- Do not make breaking API changes outside this slice unless necessary.

## Final Response

Report:

- Files changed.
- API endpoints added or changed.
- Tests run and result.
- Risks or integration notes for the main architect.
