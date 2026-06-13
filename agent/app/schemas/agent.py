from typing import Any, Literal

from pydantic import BaseModel, Field


AgentTask = Literal["summary", "reply_draft", "analyze"]
AnalysisStatus = Literal["DISABLED", "SUCCEEDED", "PARTIAL", "FAILED"]
Priority = Literal["LOW", "NORMAL", "HIGH", "URGENT"]
RiskLevel = Literal["LOW", "MEDIUM", "HIGH"]


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
    steps: list[AgentStep] = Field(default_factory=list)


class MailContext(BaseModel):
    mail_id: int
    user_id: int
    sender_email: str
    subject: str
    content_text: str
    content_html: str | None = None
    priority: str = "NORMAL"
    recipients: list[str] = Field(default_factory=list)


class ToolResult(BaseModel):
    ok: bool
    data: dict[str, Any] | None = None
    error: str | None = None


class PluginCapabilities(BaseModel):
    rules: bool = True
    llm: bool = False
    current_mail_agent: bool = Field(default=True, alias="currentMailAgent")
    rag_tool: str = Field(default="MOCK", alias="ragTool")

    model_config = {"populate_by_name": True}


class PluginHealthResponse(BaseModel):
    status: str = "UP"
    plugin_version: str = Field(alias="pluginVersion")
    capabilities: PluginCapabilities = Field(default_factory=PluginCapabilities)

    model_config = {"populate_by_name": True}


class MailPayload(BaseModel):
    subject: str = ""
    content: str = ""
    content_text: str = Field(default="", alias="contentText")
    content_html: str | None = Field(default=None, alias="contentHtml")
    sender: str | None = None
    sender_email: str | None = Field(default=None, alias="senderEmail")
    from_address: str | None = Field(default=None, alias="from")
    recipients: list[str] = Field(default_factory=list)

    model_config = {"populate_by_name": True}

    def body_text(self) -> str:
        return self.content or self.content_text or self.content_html or ""

    def sender_text(self) -> str:
        return self.sender_email or self.sender or self.from_address or ""


class PluginConfig(BaseModel):
    ai_plugin_enabled: bool = Field(default=True, alias="aiPluginEnabled")
    llm_enabled: bool = Field(default=False, alias="llmEnabled")
    api_key: str | None = Field(default=None, alias="apiKey")

    model_config = {"populate_by_name": True, "extra": "allow"}


class AnalysisRequest(BaseModel):
    task_id: str = Field(alias="taskId")
    user_id: str | int = Field(alias="userId")
    mail_item_id: str | int = Field(alias="mailItemId")
    mail: MailPayload
    user_categories: list[Any] = Field(default_factory=list, alias="userCategories")
    behavior_signals: dict[str, Any] = Field(default_factory=dict, alias="behaviorSignals")
    plugin_config: PluginConfig = Field(default_factory=PluginConfig, alias="pluginConfig")

    model_config = {"populate_by_name": True}


class ModelInfo(BaseModel):
    provider: str
    model: str
    mode: str
    llm_enabled: bool = Field(alias="llmEnabled")
    rag_tool: str = Field(alias="ragTool")

    model_config = {"populate_by_name": True}


class AnalysisResponse(BaseModel):
    status: AnalysisStatus
    summary: list[str]
    category: str | None
    junk: bool
    priority: Priority | None
    priority_score: int = Field(alias="priorityScore")
    risk_level: RiskLevel = Field(alias="riskLevel")
    risk_hints: list[str] = Field(alias="riskHints")
    model_info: ModelInfo = Field(alias="modelInfo")

    model_config = {"populate_by_name": True}
