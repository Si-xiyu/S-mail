# SmartMail Context

SmartMail is a course project for an intelligent mail system. This glossary defines the product and domain language used when discussing requirements, design, and implementation boundaries.

## Language

**AI-Native Mail Workspace**:
The product identity for SmartMail: a mail workspace where automatic summaries, categories, junk separation, and contextual assistant entry points are part of the default experience. The first version still depends on a stable mail delivery loop before advanced AI features can be useful.
_Avoid_: traditional mail client with optional AI, AI chatbot

**SmartMail System Mailbox**:
An email account registered inside the SmartMail system and served by the same SmartMail server. First-version mail delivery is between SmartMail System Mailboxes only.
_Avoid_: external mailbox, internet mailbox

**External Mailbox**:
An email account outside the SmartMail server, such as a public provider mailbox. External Mailbox communication is an extension direction, not part of the first-version core mail loop.
_Avoid_: SmartMail user, internal mailbox

**Mail Delivery Boundary**:
The product boundary that decides where mail can be sent and received. For the first version, the boundary is the same SmartMail server; cross-server mail delivery is out of scope.
_Avoid_: SMTP requirement, public email support

**Draft**:
Unsent mail content owned only by the user who is composing it. When sent, a Draft becomes a delivered mail and appears in the sender's sent mailbox and each valid recipient's inbox.
_Avoid_: unsent delivered mail, hidden inbox mail

**Local Autosave**:
The user-facing guarantee that in-progress Draft content is preserved in the same browser while composing. Local Autosave protects against page refresh or short interruptions before the Draft is explicitly sent or discarded, but does not promise cross-device recovery.
_Avoid_: server delivery, scheduled send

**Draft Sync**:
A future extension where Draft content is synchronized between the user's browser and the SmartMail server. Draft Sync is not required for the first version.
_Avoid_: local autosave

**Mailbox Item**:
A user's private view of a delivered mail, including which mailbox folder it is in and whether that user has read, starred, prioritized, or deleted it. One delivered mail can have many Mailbox Items across sender and recipients.
_Avoid_: mail body, global mail state

**Trash**:
The mailbox folder that holds Mailbox Items after a user deletes them for the first time. Deleting from Trash hides that Mailbox Item from that user without deleting the delivered mail for other users.
_Avoid_: global delete, recipient delete

**Attachment**:
A file sent together with delivered mail. Attachment file content is stored separately from the mail body, while SmartMail keeps attachment metadata for display and controlled download.
_Avoid_: inline mail body, local draft cache

**Attachment Experience**:
The user-facing expectation that attachments feel like a modern webmail service: visible before sending, listed clearly after delivery, and downloadable by users who can view the mail. First-version Attachment Experience does not require online preview, virus scanning, or resumable large-file upload.
_Avoid_: raw file path, hidden upload

**Pending Attachment**:
An uploaded file that belongs to the composing user but has not yet been attached to delivered mail. A Pending Attachment becomes an Attachment when the user sends the Draft that references it.
_Avoid_: delivered attachment, mail body

**Automatic Mail Analysis**:
AI processing that runs after a new mail is received and stores reusable results before the user opens the mail. In the first version, this includes a mail summary and category assignment.
_Avoid_: manual chat, one-off prompt

**Analysis Status**:
The state of Automatic Mail Analysis for a delivered mail, such as pending, succeeded, or failed. Mail delivery does not wait for analysis to complete.
_Avoid_: delivery status, read status

**Core Storage Boundary**:
The first-version product must run with the primary application database and file storage. Additional infrastructure such as Redis, message queues, or vector databases are enhancement dependencies, not MVP requirements.
_Avoid_: mandatory Redis, mandatory queue, mandatory vector database

**Workspace View Model**:
The API-facing shape of mail data optimized for the SmartMail workspace experience. Workspace View Models combine mail, mailbox state, category, attachment indicators, and AI analysis status so the frontend does not have to assemble the primary experience from raw database-shaped records.
_Avoid_: table CRUD response, backend record dump

**Mail Category**:
A user-owned semantic grouping assigned to mail either by the user or by AI. AI may assign mail into an existing category owned by that user or into the user's fallback Other category; categories are distinct from mailbox folders.
_Avoid_: folder, delivery status

**Junk Mail**:
Mail classified as unwanted or risky by the same mail analysis pipeline used for other categories. Junk Mail is a category outcome, not a separate delivery mechanism.
_Avoid_: deleted mail, failed delivery

**Mailbox Folder**:
A system-level location for a Mailbox Item, such as Inbox, Sent, Trash, or Junk. Mailbox Folders organize delivery state and primary navigation; they are distinct from semantic Mail Categories.
_Avoid_: category, label

**Mail Q&A Agent**:
An on-demand assistant that answers questions using the current mail or the user's mail collection as context. Mail Q&A Agent is manually invoked by the user and is separate from Automatic Mail Analysis.
_Avoid_: automatic summary, background classifier

**Mail Collection Q&A**:
A required post-MVP enhancement where the Mail Q&A Agent answers questions across the user's mail collection. It is not part of the first-version core loop, but it must be planned after the MVP is stable.
_Avoid_: first-version delivery requirement, automatic mail analysis

**Workspace Navigation**:
The left-side navigation area for high-level SmartMail views and system entry points, including mail views, user profile, settings, and global assistant access.
_Avoid_: mailbox list only

**Mail Detail Drawer**:
The right-side drawer opened from a mail list item to show the selected mail's details, AI summary, attachments, and current-mail assistant entry point.
_Avoid_: full page replacement, modal-only reader

**Current-Mail Agent**:
A manually invoked assistant opened from the Mail Detail Drawer with the selected mail as its primary context.
_Avoid_: global mailbox agent

**Global Mail Agent**:
A manually invoked assistant opened from Workspace Navigation for mailbox-wide questions. In the first version it may have limited or no mailbox-wide retrieval; full Mail Collection Q&A is a post-MVP enhancement.
_Avoid_: current-mail agent

**RAG Tool**:
A tool exposed to the Mail Q&A Agent for retrieving context from the user's mail collection. In the MVP, the RAG Tool may be mocked behind the same tool boundary; real hybrid retrieval is a post-MVP enhancement.
_Avoid_: mandatory vector search, direct database coupling

**Automatic Analysis Pipeline**:
The background AI execution path for Automatic Mail Analysis after mail delivery. It has system-scoped permission to write only analysis-related outcomes such as summaries, categories, junk classification, and analysis status.
_Avoid_: interactive agent, user-confirmed tool call

**Interactive Tool Router**:
The user-invoked AI execution path for Mail Q&A Agent tools. Read tools may run automatically, while write tools require user confirmation unless the user explicitly enables automatic writes in settings.
_Avoid_: background analysis pipeline, unrestricted automation

## Example Dialogue

Developer: Can a SmartMail user send mail to a QQ or Gmail address in the first version?

Domain expert: No. The first version only delivers mail between SmartMail System Mailboxes on the same server. External Mailbox communication can be described as a future extension.

Developer: Is SmartMail just a normal mailbox with a few AI buttons?

Domain expert: No. SmartMail should feel like an AI-Native Mail Workspace: AI summaries, categories, junk separation, and assistant entry points shape the default mail experience.

Developer: Is a draft already a mail in the recipient's mailbox?

Domain expert: No. A Draft belongs only to its creator until it is sent. After sending, it becomes delivered mail and appears in the sender's sent mailbox and the recipients' inboxes.

Developer: If I start a draft on one computer, must it appear on another computer?

Domain expert: Not in the first version. Local Autosave only guarantees recovery in the same browser; Draft Sync can be added later.

Developer: If one recipient deletes a mail, does it disappear from the sender's sent mailbox?

Domain expert: No. Deletion applies to that user's Mailbox Item only. Other users' Mailbox Items remain visible.

Developer: Are attachments part of the first version or an optional extension?

Domain expert: Attachments are part of the first-version mail loop. Users should be able to attach files before sending and download them after delivery.

Developer: Does choosing a file wait until send time to upload it?

Domain expert: No. The first-version experience uploads selected files while composing, then binds those Pending Attachments to the delivered mail when the user sends.

Developer: When should AI results appear for received mail?

Domain expert: Automatic Mail Analysis runs after receipt and stores results, so the user can see the summary and category when opening the mail.

Developer: Should a new mail wait for AI before appearing in the inbox?

Domain expert: No. Mail delivery completes first. Automatic Mail Analysis fills in summary and category results asynchronously.

Developer: Does the MVP require Redis, a message queue, or a vector database?

Domain expert: No. The MVP should run on the primary database and file storage. Extra infrastructure can enhance later stages.

Developer: Should frontend APIs mirror database tables?

Domain expert: No. Primary frontend APIs should return Workspace View Models that match the mail workspace experience.

Developer: Is the mail assistant the same thing as automatic analysis?

Domain expert: No. Automatic Mail Analysis runs in the background after receipt. Mail Q&A Agent is manually opened by the user for contextual questions.

Developer: Must the first version answer questions across all of a user's mail?

Domain expert: No. The first version focuses on automatic analysis and current-mail Q&A. Mail Collection Q&A is a required post-MVP enhancement after the core loop is stable.

Developer: Where does the user open the assistant?

Domain expert: The Current-Mail Agent opens from the Mail Detail Drawer for the selected mail. The Global Mail Agent opens from Workspace Navigation for mailbox-wide assistance.

Developer: Does the MVP need real retrieval-augmented generation for the Global Mail Agent?

Domain expert: No. The MVP should expose a RAG Tool boundary and can mock its behavior. Real hybrid retrieval is added after the MVP is stable.

Developer: Should automatic summaries and categories use the same permission flow as the interactive assistant?

Domain expert: No. Automatic Analysis Pipeline runs as a background system task with narrowly scoped write permissions. Interactive Tool Router is user-invoked and requires confirmation for write tools by default.

Developer: Are categories and folders the same thing?

Domain expert: No. Folders describe where a Mailbox Item lives in the system navigation. Categories describe what the mail is about.

Developer: Are mail categories shared across all users?

Domain expert: No. Mail Categories belong to individual users. Each user has their own categories, including fallback categories such as Other and Junk Mail.
