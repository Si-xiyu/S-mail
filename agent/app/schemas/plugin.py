from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field


PluginScope = Literal["CURRENT_MAIL", "GLOBAL"]
PluginStatus = Literal["SUCCESS", "DISABLED", "NEEDS_ACTION", "FAILED"]
MailActionType = Literal["SET_PRIORITY", "SET_CATEGORY", "MOVE_TO_JUNK", "MARK_READ"]


class PluginChatRequest(BaseModel):
    session_id: str = Field(alias="sessionId")
    user_id: int | str = Field(alias="userId")
    scope: PluginScope
    message: str
    context: dict[str, Any] = Field(default_factory=dict)
    tool_policy: dict[str, Any] = Field(default_factory=dict, alias="toolPolicy")
    plugin_config: dict[str, Any] = Field(default_factory=dict, alias="pluginConfig")

    model_config = {"populate_by_name": True}


class ToolCallRecord(BaseModel):
    tool: str
    status: str
    input: dict[str, Any] = {}
    output: dict[str, Any] = {}
    source: str | None = None
    error: str | None = None


class PendingAction(BaseModel):
    type: MailActionType
    payload: dict[str, Any] = {}
    reason: str
    status: str = "PENDING"
    execution: str = "BACKEND_REQUIRED"


class PluginChatResponse(BaseModel):
    status: PluginStatus
    answer: str
    tool_calls: list[ToolCallRecord] = Field(default_factory=list, alias="toolCalls")
    pending_actions: list[PendingAction] = Field(default_factory=list, alias="pendingActions")

    model_config = {"populate_by_name": True}
