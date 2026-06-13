from fastapi import APIRouter

from app.core.config import settings
from app.schemas.agent import (
    AgentTaskRequest,
    AgentTaskResponse,
    AnalysisRequest,
    AnalysisResponse,
    ModelInfo,
    PluginCapabilities,
    PluginHealthResponse,
)
from app.schemas.plugin import PluginChatRequest, PluginChatResponse
from app.services.agent_loop import SmartMailAgent
from app.services.rule_engine import RuleEngine
from app.services.tool_router import ToolRouter

router = APIRouter()
plugin_router = APIRouter(prefix="/plugin/v1/agent", tags=["agent-plugin"])
agent = SmartMailAgent()
rules = RuleEngine()
tool_router = ToolRouter()


@router.get("/plugin/v1/health", response_model=PluginHealthResponse, tags=["plugin"])
def plugin_health() -> PluginHealthResponse:
    return PluginHealthResponse(
        pluginVersion=settings.plugin_version,
        capabilities=PluginCapabilities(rules=True, llm=False, currentMailAgent=True, ragTool="MOCK"),
    )


@router.post("/plugin/v1/analysis/mail", response_model=AnalysisResponse, tags=["plugin"])
def analyze_mail(request: AnalysisRequest) -> AnalysisResponse:
    model_info = _model_info(request)
    if not request.plugin_config.ai_plugin_enabled:
        return AnalysisResponse(
            status="DISABLED",
            summary=[],
            category=None,
            junk=False,
            priority=None,
            priorityScore=0,
            riskLevel="LOW",
            riskHints=[],
            modelInfo=model_info,
        )

    return rules.analysis_response(
        subject=request.mail.subject,
        content=request.mail.body_text(),
        sender=request.mail.sender_text(),
        user_categories=request.user_categories,
        behavior_signals=request.behavior_signals,
        model_info=model_info,
    )


@router.post("/api/v1/agent/tasks", response_model=AgentTaskResponse, tags=["agent"])
def run_task(request: AgentTaskRequest) -> AgentTaskResponse:
    return agent.run(request)


@plugin_router.post("/chat", response_model=PluginChatResponse)
def chat(request: PluginChatRequest) -> PluginChatResponse:
    return tool_router.chat(request)


def _model_info(_request: AnalysisRequest) -> ModelInfo:
    return ModelInfo(
        provider="rules",
        model="deterministic-rules-v1",
        mode="rules-fallback",
        llmEnabled=False,
        ragTool="MOCK",
    )
