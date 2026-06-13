from fastapi import APIRouter

from app.schemas.agent import AgentTaskRequest, AgentTaskResponse
from app.schemas.plugin import PluginChatRequest, PluginChatResponse
from app.services.agent_loop import SmartMailAgent
from app.services.tool_router import ToolRouter

router = APIRouter(prefix="/api/v1/agent", tags=["agent"])
plugin_router = APIRouter(prefix="/plugin/v1/agent", tags=["agent-plugin"])
agent = SmartMailAgent()
tool_router = ToolRouter()


@router.post("/tasks", response_model=AgentTaskResponse)
def run_task(request: AgentTaskRequest) -> AgentTaskResponse:
    return agent.run(request)


@plugin_router.post("/chat", response_model=PluginChatResponse)
def chat(request: PluginChatRequest) -> PluginChatResponse:
    return tool_router.chat(request)
