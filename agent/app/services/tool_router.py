from __future__ import annotations

from typing import Any

from app.schemas.plugin import (
    BackendOperation,
    ConfirmedActionExecuteRequest,
    ConfirmedActionExecuteResponse,
    PendingAction,
    PluginChatRequest,
    PluginChatResponse,
    ToolCallRecord,
)
from app.services.rag_tool import RagTool
from app.tools.backend_tools import BackendToolClient, to_mail_context


BACKEND_ACTION_EXECUTE_PATH = "/internal/v1/tools/mail-actions/execute"
ACTION_KEYWORDS: dict[str, tuple[str, dict[str, Any], str]] = {
    "MOVE_TO_JUNK": ("MOVE_TO_JUNK", {}, "移入 Junk"),
    "MARK_READ": ("MARK_READ", {"read": True}, "标记为已读"),
    "SET_PRIORITY": ("SET_PRIORITY", {"priority": "HIGH"}, "标记为高优先级"),
    "SET_CATEGORY": ("SET_CATEGORY", {}, "设置分类"),
}


class ToolRouter:
    def __init__(self) -> None:
        self.backend_tools = BackendToolClient()
        self.rag_tool = RagTool()

    def chat(self, request: PluginChatRequest) -> PluginChatResponse:
        if not _plugin_enabled(request.plugin_config):
            return PluginChatResponse(
                status="DISABLED",
                answer="AI Plugin 已关闭，系统处于基础邮箱模式。",
                tool_calls=[],
                pending_actions=[],
            )

        if request.scope == "GLOBAL":
            return self._handle_global(request)
        return self._handle_current_mail(request)

    def execute_action(self, request: ConfirmedActionExecuteRequest) -> ConfirmedActionExecuteResponse:
        if not _plugin_enabled(request.plugin_config):
            return ConfirmedActionExecuteResponse(
                status="DISABLED",
                actionId=request.action_id,
                execution="NONE",
                message="AI Plugin 已关闭，未执行任何操作。",
            )

        if request.type not in ACTION_KEYWORDS:
            return ConfirmedActionExecuteResponse(
                status="REJECTED",
                actionId=request.action_id,
                execution="NONE",
                message=f"不支持的操作类型: {request.type}。",
            )

        payload = _normalized_action_payload(request.payload)
        if request.type == "SET_CATEGORY" and payload.get("categoryId") is None:
            return ConfirmedActionExecuteResponse(
                status="REJECTED",
                actionId=request.action_id,
                execution="NONE",
                message="SET_CATEGORY 必须提供 categoryId。",
            )

        if not request.confirmed and not _auto_write_enabled(request.tool_policy):
            return ConfirmedActionExecuteResponse(
                status="REJECTED",
                actionId=request.action_id,
                execution="NONE",
                message="操作未确认且未开启 agentAutoWriteEnabled。",
            )

        return ConfirmedActionExecuteResponse(
            status="DELEGATED",
            actionId=request.action_id,
            execution="BACKEND_REQUIRED",
            backendOperation=BackendOperation(
                method="POST",
                path=BACKEND_ACTION_EXECUTE_PATH,
                payload=payload,
            ),
            message="操作已确认，将由后端执行。",
        )

    # ------------------------------------------------------------------
    # Current mail
    # ------------------------------------------------------------------
    def _handle_current_mail(self, request: PluginChatRequest) -> PluginChatResponse:
        mail_result = self.backend_tools.get_current_mail_context(request.context, request.user_id)
        tool_call = ToolCallRecord(
            tool="mail_context_tool",
            status="SUCCEEDED" if mail_result.ok else "FAILED",
            input={"scope": request.scope, "contextKeys": sorted(request.context.keys())},
            output=mail_result.data or {},
            source=(mail_result.data or {}).get("source"),
            error=mail_result.error,
        )

        if not mail_result.ok or mail_result.data is None:
            return PluginChatResponse(
                status="FAILED",
                answer="无法从后端获取当前邮件上下文，且未启用 mock 降级。",
                tool_calls=[tool_call],
            )

        mail = to_mail_context(mail_result.data)

        # Write intent: only generate whitelisted pending actions.
        action_type, _, _ = _detect_action(request.message)
        if action_type is not None and _mail_item_id_from_context(request.context) is None:
            return PluginChatResponse(
                status="FAILED",
                answer="写操作需要 mailItemId 上下文，但未提供。",
                tool_calls=[tool_call],
            )

        pending_actions = self._mail_action_tool(request)
        if pending_actions:
            return PluginChatResponse(
                status="SUCCEEDED",
                answer=_describe_pending_actions(pending_actions),
                tool_calls=[
                    tool_call,
                    ToolCallRecord(
                        tool="mail_action_tool",
                        status="PENDING",
                        input={"message": request.message, "autoWrite": _auto_write_enabled(request.tool_policy)},
                        output={"pendingActions": [action.model_dump(by_alias=True) for action in pending_actions]},
                    ),
                ],
                pending_actions=pending_actions,
            )

        return PluginChatResponse(
            status="SUCCEEDED",
            answer=_answer_current_mail(request.message, mail.subject, mail.sender_email, mail.content_text),
            tool_calls=[tool_call],
        )

    def _mail_action_tool(self, request: PluginChatRequest) -> list[PendingAction]:
        action_type, payload, label = _detect_action(request.message)
        if action_type is None:
            return []

        mail_item_id = _mail_item_id_from_context(request.context)
        if mail_item_id is None:
            return []

        if action_type == "SET_CATEGORY":
            category_id = _category_id_from_context(request.context)
            if category_id is None:
                return []
            payload = {**payload, "categoryId": category_id}

        auto_write = _auto_write_enabled(request.tool_policy)
        reason = (
            "agentAutoWriteEnabled=true，允许自动委派后端执行。"
            if auto_write
            else "agentAutoWriteEnabled=false，需要用户在界面确认。"
        )
        full_payload = {**payload, "mailItemId": mail_item_id, "userId": request.user_id}
        return [
            PendingAction(
                actionId=_action_id(request.session_id, action_type, mail_item_id),
                type=action_type,
                label=label,
                payload=full_payload,
                reason=reason,
                status="PENDING",
                execution="BACKEND_REQUIRED",
            )
        ]

    # ------------------------------------------------------------------
    # Global
    # ------------------------------------------------------------------
    def _handle_global(self, request: PluginChatRequest) -> PluginChatResponse:
        search_result = self.backend_tools.search_mail(
            request.user_id,
            request.message,
            folder=None,
            limit=5,
        )

        records: list[dict[str, Any]] = []
        if search_result.ok and isinstance(search_result.data, list):
            records = search_result.data[:5]

        if records:
            tool_call = ToolCallRecord(
                tool="mail_search_tool",
                status="SUCCEEDED",
                input={"query": request.message, "limit": 5},
                output={"records": records, "source": "BACKEND"},
                source="BACKEND",
            )
            answer = _answer_from_records(request.message, records)
        else:
            records, tool_call = self.rag_tool.search(request.message, request.context)
            answer = (
                "暂未从后端检索到相关邮件，当前返回 mock 示例结果。"
                "完整邮箱问答将在接入 BM25 + 向量检索后提供。"
                f"\n示例匹配: {', '.join(r.get('title') or r.get('subject') or 'unknown' for r in records)}。"
            )

        return PluginChatResponse(status="SUCCEEDED", answer=answer, tool_calls=[tool_call])


def _plugin_enabled(plugin_config: dict[str, Any]) -> bool:
    for key in ("aiPluginEnabled", "enabled", "pluginEnabled"):
        if key in plugin_config:
            return bool(plugin_config[key])
    return True


def _auto_write_enabled(tool_policy: dict[str, Any]) -> bool:
    return bool(tool_policy.get("agentAutoWriteEnabled", False))


def _detect_action(message: str) -> tuple[Any, dict[str, Any], str | None]:
    text = message.lower()

    # Junk / spam
    if any(word in text for word in ("垃圾", "junk", "spam", "广告")) and any(
        word in text for word in ("移", "move", "标为", "mark", "删", "delete")
    ):
        return ACTION_KEYWORDS["MOVE_TO_JUNK"]

    # Read
    if any(word in text for word in ("已读", "read")) and any(
        word in text for word in ("标", "mark", "设", "set")
    ):
        return ACTION_KEYWORDS["MARK_READ"]

    # Priority
    if any(word in text for word in ("优先级", "priority", "重要", "urgent", "高优先级")) and any(
        word in text for word in ("设", "set", "标", "mark", "改", "提升")
    ):
        return ACTION_KEYWORDS["SET_PRIORITY"]

    # Category
    if any(word in text for word in ("分类", "category", "标签", "label")) and any(
        word in text for word in ("设", "set", "标", "mark", "加", "add", "移到", "move")
    ):
        return ACTION_KEYWORDS["SET_CATEGORY"]

    return None, {}, None


def _mail_item_id_from_context(context: dict[str, Any]) -> Any:
    return context.get("mailItemId") or context.get("mail_item_id") or context.get("mailId") or context.get("mail_id")


def _category_id_from_context(context: dict[str, Any]) -> Any:
    return context.get("categoryId") or context.get("category_id")


def _normalized_action_payload(payload: dict[str, Any]) -> dict[str, Any]:
    normalized = dict(payload)
    mail_item_id = _mail_item_id_from_context(normalized)
    for legacy_key in ("mailId", "mail_id", "mail_item_id"):
        normalized.pop(legacy_key, None)
    if mail_item_id is not None:
        normalized["mailItemId"] = mail_item_id
    return normalized


def _action_id(session_id: str, action_type: str, mail_item_id: Any) -> str:
    mail_part = "unknown" if mail_item_id is None else str(mail_item_id)
    return f"{session_id}:{mail_part}:{action_type}".replace(" ", "-")


def _shorten(content: str, limit: int = 180) -> str:
    cleaned = " ".join(content.split())
    return cleaned[:limit] + ("..." if len(cleaned) > limit else "")


def _answer_current_mail(message: str, subject: str, sender: str, content: str) -> str:
    text = message.lower()
    summary = _shorten(content)
    if any(word in text for word in ("总结", "摘要", "summarize", "summary")):
        return f"这封邮件来自 {sender}，主题是「{subject}」。要点如下：{summary}"
    if any(word in text for word in ("做什么", "需要我", "action", "todo", "task")):
        return f"邮件来自 {sender}，主题「{subject}」。建议你先确认诉求并按内容处理：{summary}"
    if any(word in text for word in ("回复", "reply")):
        return (
            f"回复思路：先确认已收到「{subject}」，再回应关键事项，"
            f"最后给出你的下一步或预计完成时间。可参考内容：{summary}"
        )
    if any(word in text for word in ("安全", "safe", "phishing", "钓鱼", "风险")):
        risk = "正文中包含链接或紧急措辞，建议人工核验发件人和链接域名。" if _looks_risky(content) else "当前未看到明显高风险信号，但仍应核验发件人和附件/链接。"
        return f"安全判断：{risk} 发件人：{sender}；主题：「{subject}」。"
    return f"基于当前邮件上下文，主题是「{subject}」，发件人是 {sender}。内容摘要：{summary}"


def _looks_risky(content: str) -> bool:
    lowered = content.lower()
    return any(
        token in lowered
        for token in ("password", "verify", "urgent", "http://", "验证码", "密码", "立即", "点击", "中奖", "转账")
    )


def _describe_pending_actions(actions: list[PendingAction]) -> str:
    if not actions:
        return "已准备好操作，等待确认。"
    labels = ", ".join(action.label for action in actions)
    return f"我可以帮你执行以下操作：{labels}。请在界面确认后由后端执行。"


def _answer_from_records(message: str, records: list[dict[str, Any]]) -> str:
    if not records:
        return "未找到相关邮件。"
    parts = []
    for idx, record in enumerate(records[:3], start=1):
        subject = record.get("subject") or record.get("title") or "无主题"
        sender = record.get("senderEmail") or "未知发件人"
        snippet = record.get("snippet") or record.get("preview") or ""
        parts.append(f"{idx}. 「{subject}」 ({sender}) {snippet}")
    return "找到以下相关邮件：\n" + "\n".join(parts) + "\n你可以让我对其中一封邮件执行摘要或操作。"
