from fastapi import APIRouter, Depends, Header, HTTPException, status

from app.core.config import settings
from app.schemas.agent import (
    AgentTaskRequest,
    AgentTaskResponse,
    AnalysisRequest,
    AnalysisResponse,
    PluginCapabilities,
    PluginHealthResponse,
)
from app.schemas.plugin import (
    ConfirmedActionExecuteRequest,
    ConfirmedActionExecuteResponse,
    PluginChatRequest,
    PluginChatResponse,
)
from app.services.agent_loop import SmartMailAgent
from app.services.analysis_provider import RulesAnalysisProvider
from app.services.tool_router import ToolRouter

router = APIRouter()
plugin_router = APIRouter(prefix="/plugin/v1/agent", tags=["agent-plugin"])
agent = SmartMailAgent()
analysis_provider = RulesAnalysisProvider()
tool_router = ToolRouter()


def verify_plugin_token(x_plugin_token: str | None = Header(default=None, alias="X-Plugin-Token")) -> None:
    if not x_plugin_token or x_plugin_token != settings.plugin_token:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Invalid or missing X-Plugin-Token",
        )


@router.get("/plugin/v1/health", response_model=PluginHealthResponse, tags=["plugin"])
def plugin_health() -> PluginHealthResponse:
    llm_available = bool(settings.llm_api_key)
    return PluginHealthResponse(
        pluginVersion=settings.plugin_version,
        capabilities=PluginCapabilities(
            rules=True,
            llm=llm_available,
            currentMailAgent=True,
            ragTool=settings.rag_mode,
        ),
    )


@router.post(
    "/plugin/v1/analysis/mail",
    response_model=AnalysisResponse,
    tags=["plugin"],
    dependencies=[Depends(verify_plugin_token)],
)
def analyze_mail(request: AnalysisRequest) -> AnalysisResponse:
    model_info = analysis_provider.model_info(request)
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

    return analysis_provider.analyze(request)


@router.post("/api/v1/agent/tasks", response_model=AgentTaskResponse, tags=["agent"])
def run_task(request: AgentTaskRequest) -> AgentTaskResponse:
    return agent.run(request)


@plugin_router.post("/chat", response_model=PluginChatResponse, dependencies=[Depends(verify_plugin_token)])
def chat(request: PluginChatRequest) -> PluginChatResponse:
    return tool_router.chat(request)


@plugin_router.post(
    "/actions/execute",
    response_model=ConfirmedActionExecuteResponse,
    dependencies=[Depends(verify_plugin_token)],
)
def execute_action(request: ConfirmedActionExecuteRequest) -> ConfirmedActionExecuteResponse:
    return tool_router.execute_action(request)
