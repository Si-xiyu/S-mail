from __future__ import annotations

from typing import Any

from app.schemas.plugin import PendingAction, PluginChatRequest, PluginChatResponse, ToolCallRecord
from app.services.rag_tool import RagTool
from app.tools.backend_tools import BackendToolClient, to_mail_context


ACTION_KEYWORDS: dict[str, tuple[str, dict[str, Any]]] = {
    "MOVE_TO_JUNK": ("MOVE_TO_JUNK", {}),
    "MARK_READ": ("MARK_READ", {"read": True}),
    "SET_PRIORITY": ("SET_PRIORITY", {"priority": "HIGH"}),
    "SET_CATEGORY": ("SET_CATEGORY", {"category": "FOLLOW_UP"}),
}


class ToolRouter:
    def __init__(self) -> None:
        self.backend_tools = BackendToolClient()
        self.rag_tool = RagTool()

    def chat(self, request: PluginChatRequest) -> PluginChatResponse:
        if not _plugin_enabled(request.plugin_config):
            return PluginChatResponse(
                status="DISABLED",
                answer="AI Plugin is disabled. Basic Mail Mode remains available.",
            )

        if request.scope == "GLOBAL":
            return self._handle_global(request)
        return self._handle_current_mail(request)

    def _handle_current_mail(self, request: PluginChatRequest) -> PluginChatResponse:
        pending_actions = self._mail_action_tool(request)
        if pending_actions:
            return PluginChatResponse(
                status="NEEDS_ACTION",
                answer="I prepared the requested mail action for backend handling. No local mail state was changed by the Agent.",
                tool_calls=[
                    ToolCallRecord(
                        tool="mail_action_tool",
                        status="PENDING",
                        input={"message": request.message, "agentAutoWriteEnabled": _auto_write_enabled(request.tool_policy)},
                        output={"pendingActions": [action.model_dump(by_alias=True) for action in pending_actions]},
                    )
                ],
                pending_actions=pending_actions,
            )

        mail_result = self.backend_tools.get_current_mail_context(request.context, request.user_id)
        tool_call = ToolCallRecord(
            tool="mail_context_tool",
            status="SUCCESS" if mail_result.ok else "FAILED",
            input={"scope": request.scope, "contextKeys": sorted(request.context.keys())},
            output=mail_result.data or {},
            source=(mail_result.data or {}).get("source"),
            error=mail_result.error,
        )
        if not mail_result.ok or mail_result.data is None:
            return PluginChatResponse(
                status="FAILED",
                answer="I could not retrieve current-mail context from the backend, and mock fallback is disabled.",
                tool_calls=[tool_call],
            )

        mail = to_mail_context(mail_result.data)
        return PluginChatResponse(
            status="SUCCESS",
            answer=_answer_current_mail(request.message, mail.subject, mail.sender_email, mail.content_text),
            tool_calls=[tool_call],
        )

    def _handle_global(self, request: PluginChatRequest) -> PluginChatResponse:
        records, tool_call = self.rag_tool.search(request.message, request.context)
        answer = (
            "This answer is based on available/mock retrieval context. "
            "Full mailbox retrieval is not implemented in this Agent MVP. "
            f"Mock matches: {', '.join(record['title'] for record in records)}."
        )
        return PluginChatResponse(status="SUCCESS", answer=answer, tool_calls=[tool_call])

    def _mail_action_tool(self, request: PluginChatRequest) -> list[PendingAction]:
        action_type, payload = _detect_action(request.message)
        if action_type is None:
            return []

        auto_write = _auto_write_enabled(request.tool_policy)
        status = "BACKEND_DELEGATION_PREPARED" if auto_write else "PENDING"
        reason = (
            "agentAutoWriteEnabled=true and action is whitelisted; backend delegation required."
            if auto_write
            else "agentAutoWriteEnabled=false; user confirmation is required."
        )
        mail_id = request.context.get("mailId") or request.context.get("mail_id")
        full_payload = {**payload}
        if mail_id is not None:
            full_payload["mailId"] = mail_id
        full_payload["userId"] = request.user_id
        return [
            PendingAction(
                type=action_type,
                payload=full_payload,
                reason=reason,
                status=status,
                execution="BACKEND_DELEGATION_ONLY",
            )
        ]


def _plugin_enabled(plugin_config: dict[str, Any]) -> bool:
    for key in ("aiPluginEnabled", "enabled", "pluginEnabled"):
        if key in plugin_config:
            return bool(plugin_config[key])
    return True


def _auto_write_enabled(tool_policy: dict[str, Any]) -> bool:
    return bool(tool_policy.get("agentAutoWriteEnabled", False))


def _detect_action(message: str) -> tuple[Any, dict[str, Any]]:
    text = message.lower()
    if any(word in text for word in ("垃圾", "junk", "spam")) and any(word in text for word in ("移", "move", "标为", "mark")):
        return ACTION_KEYWORDS["MOVE_TO_JUNK"]
    if any(word in text for word in ("已读", "read")) and any(word in text for word in ("标", "mark", "设", "set")):
        return ACTION_KEYWORDS["MARK_READ"]
    if any(word in text for word in ("优先级", "priority", "重要", "urgent")) and any(word in text for word in ("设", "set", "标", "mark", "改")):
        return ACTION_KEYWORDS["SET_PRIORITY"]
    if any(word in text for word in ("分类", "category", "标签", "label")) and any(word in text for word in ("设", "set", "标", "mark", "加", "add")):
        return ACTION_KEYWORDS["SET_CATEGORY"]
    return None, {}


def _answer_current_mail(message: str, subject: str, sender: str, content: str) -> str:
    text = message.lower()
    summary = _shorten(content)
    if any(word in text for word in ("做什么", "需要我", "action", "todo", "task")):
        return f"这封邮件来自 {sender}，主题是“{subject}”。你需要先确认邮件诉求，再按邮件内容处理：{summary}"
    if any(word in text for word in ("回复", "reply")):
        return f"回复思路：先确认已收到“{subject}”，再回应关键事项，最后给出你的下一步或预计完成时间。可参考邮件内容：{summary}"
    if any(word in text for word in ("安全", "safe", "phishing", "钓鱼")):
        risk = "存在链接或紧急措辞时需要人工核验发件人和链接域名。" if _looks_risky(content) else "未从当前上下文看到明显高风险信号，但仍应核验发件人和附件/链接。"
        return f"安全判断：{risk} 发件人：{sender}；主题：“{subject}”。"
    return f"基于当前邮件上下文，主题是“{subject}”，发件人是 {sender}。可用摘要：{summary}"


def _shorten(content: str) -> str:
    cleaned = " ".join(content.split())
    return cleaned[:180] + ("..." if len(cleaned) > 180 else "")


def _looks_risky(content: str) -> bool:
    lowered = content.lower()
    return any(token in lowered for token in ("password", "verify", "urgent", "http://", "验证码", "密码", "立即", "点击"))
