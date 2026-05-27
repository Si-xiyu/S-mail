from __future__ import annotations

from app.schemas.agent import AgentStep, AgentTaskRequest, AgentTaskResponse
from app.services.rule_engine import RuleEngine
from app.tools.backend_tools import BackendToolClient, to_mail_context


class SmartMailAgent:
    def __init__(self) -> None:
        self.tools = BackendToolClient()
        self.rules = RuleEngine()

    def run(self, request: AgentTaskRequest) -> AgentTaskResponse:
        steps: list[AgentStep] = [
            AgentStep(name="plan", detail=f"准备执行 {request.task} 任务"),
            AgentStep(name="tool:get_mail", detail="通过后端 internal API 获取邮件上下文"),
        ]
        mail_result = self.tools.get_mail(request.mail_id, request.user_id)
        if not mail_result.ok or mail_result.data is None:
            steps.append(AgentStep(name="error", detail=mail_result.error or "无法获取邮件"))
            return AgentTaskResponse(task=request.task, status="FAILED", result={"message": "无法获取邮件"}, steps=steps)

        mail = to_mail_context(mail_result.data)
        if request.task == "summary":
            result = self.rules.summarize(mail)
            result_type = "SUMMARY"
        elif request.task == "reply_draft":
            result = self.rules.draft_reply(mail)
            result_type = "REPLY_DRAFT"
        else:
            result = self.rules.analyze(mail)
            result_type = "ANALYZE"
            priority = result.get("priority")
            if isinstance(priority, str):
                priority_result = self.tools.set_priority(mail.mail_id, mail.user_id, priority)
                steps.append(AgentStep(name="tool:set_priority", detail="已请求后端更新邮件优先级" if priority_result.ok else priority_result.error or "优先级更新失败"))

        steps.append(AgentStep(name="reason", detail="使用规则引擎生成结构化结果"))
        save_result = self.tools.save_ai_result(mail.mail_id, mail.user_id, result_type, result)
        steps.append(AgentStep(name="tool:save_ai_result", detail="AI 结果已写回后端" if save_result.ok else save_result.error or "AI 结果写回失败"))
        return AgentTaskResponse(task=request.task, status="SUCCESS", result=result, steps=steps)
