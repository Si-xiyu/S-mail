from fastapi import APIRouter

from app.schemas.agent import AgentTaskRequest, AgentTaskResponse
from app.services.agent_loop import SmartMailAgent

router = APIRouter(prefix="/api/v1/agent", tags=["agent"])
agent = SmartMailAgent()


@router.post("/tasks", response_model=AgentTaskResponse)
def run_task(request: AgentTaskRequest) -> AgentTaskResponse:
    return agent.run(request)
