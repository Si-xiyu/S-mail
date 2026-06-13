from __future__ import annotations

from typing import Any, Literal

from pydantic import BaseModel, Field


PluginScope = Literal["CURRENT_MAIL", "GLOBAL"]
PluginStatus = Literal["SUCCEEDED", "DISABLED", "PARTIAL", "FAILED"]
MailActionType = Literal["SET_PRIORITY", "SET_CATEGORY", "MOVE_TO_JUNK", "MARK_READ"]
ActionExecutionStatus = Literal["DELEGATED", "DISABLED", "REJECTED", "FAILED"]
ActionExecutionMode = Literal["BACKEND_REQUIRED", "NONE"]


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
    input: dict[str, Any] = Field(default_factory=dict)
    output: dict[str, Any] = Field(default_factory=dict)
    source: str | None = None
    error: str | None = None


class PendingAction(BaseModel):
    action_id: str | None = Field(default=None, alias="actionId")
    type: MailActionType
    label: str
    payload: dict[str, Any] = Field(default_factory=dict)
    reason: str
    status: str = "PENDING"
    execution: str = "BACKEND_REQUIRED"

    model_config = {"populate_by_name": True}


class PluginChatResponse(BaseModel):
    status: PluginStatus
    answer: str
    tool_calls: list[ToolCallRecord] = Field(default_factory=list, alias="toolCalls")
    pending_actions: list[PendingAction] = Field(default_factory=list, alias="pendingActions")

    model_config = {"populate_by_name": True}


class ConfirmedActionExecuteRequest(BaseModel):
    action_id: str = Field(alias="actionId")
    user_id: int | str = Field(alias="userId")
    confirmed: bool = False
    type: str
    payload: dict[str, Any] = Field(default_factory=dict)
    plugin_config: dict[str, Any] = Field(default_factory=dict, alias="pluginConfig")
    tool_policy: dict[str, Any] = Field(default_factory=dict, alias="toolPolicy")

    model_config = {"populate_by_name": True}


class BackendOperation(BaseModel):
    method: Literal["POST"]
    path: str
    payload: dict[str, Any] = Field(default_factory=dict)


class ConfirmedActionExecuteResponse(BaseModel):
    status: ActionExecutionStatus
    action_id: str = Field(alias="actionId")
    execution: ActionExecutionMode = "NONE"
    backend_operation: BackendOperation | None = Field(default=None, alias="backendOperation")
    message: str

    model_config = {"populate_by_name": True}
