from typing import Any, Literal

from pydantic import BaseModel, Field


AgentTask = Literal["summary", "reply_draft", "analyze"]


class AgentTaskRequest(BaseModel):
    mail_id: int = Field(alias="mailId")
    user_id: int = Field(alias="userId")
    task: AgentTask

    model_config = {"populate_by_name": True}


class AgentStep(BaseModel):
    name: str
    detail: str


class AgentTaskResponse(BaseModel):
    task: str
    status: str
    result: dict[str, Any]
    steps: list[AgentStep] = []


class MailContext(BaseModel):
    mail_id: int
    user_id: int
    sender_email: str
    subject: str
    content_text: str
    content_html: str | None = None
    priority: str = "NORMAL"
    recipients: list[str] = []


class ToolResult(BaseModel):
    ok: bool
    data: dict[str, Any] | None = None
    error: str | None = None
