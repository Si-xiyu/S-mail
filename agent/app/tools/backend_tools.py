from __future__ import annotations

import json
from typing import Any

import httpx

from app.core.config import settings
from app.schemas.agent import MailContext, ToolResult


class BackendToolClient:
    def __init__(self) -> None:
        self.base_url = settings.backend_base_url.rstrip("/")
        self.headers = {"X-Internal-Token": settings.internal_token}

    def get_mail(self, mail_id: int, user_id: int) -> ToolResult:
        try:
            response = httpx.get(
                f"{self.base_url}/internal/v1/tools/mails/{mail_id}",
                params={"userId": user_id},
                headers=self.headers,
                timeout=settings.timeout_seconds,
            )
            response.raise_for_status()
            payload = response.json()["data"]
            return ToolResult(ok=True, data=payload)
        except Exception as exc:
            if not settings.mock_on_tool_error:
                return ToolResult(ok=False, error=str(exc))
            return ToolResult(ok=True, data=self._mock_mail(mail_id, user_id, str(exc)))

    def save_ai_result(self, mail_id: int, user_id: int, result_type: str, result: dict[str, Any]) -> ToolResult:
        try:
            response = httpx.post(
                f"{self.base_url}/internal/v1/tools/ai-results",
                headers=self.headers,
                json={
                    "mailId": mail_id,
                    "userId": user_id,
                    "resultType": result_type.upper(),
                    "resultJson": json.dumps(result, ensure_ascii=False),
                    "status": "SUCCESS",
                },
                timeout=settings.timeout_seconds,
            )
            response.raise_for_status()
            return ToolResult(ok=True, data={"saved": True})
        except Exception as exc:
            return ToolResult(ok=False, error=str(exc))

    def set_priority(self, mail_id: int, user_id: int, priority: str) -> ToolResult:
        try:
            response = httpx.post(
                f"{self.base_url}/internal/v1/tools/mails/{mail_id}/priority",
                headers=self.headers,
                json={"userId": user_id, "priority": priority},
                timeout=settings.timeout_seconds,
            )
            response.raise_for_status()
            return ToolResult(ok=True, data={"priority": priority})
        except Exception as exc:
            return ToolResult(ok=False, error=str(exc))

    def _mock_mail(self, mail_id: int, user_id: int, reason: str) -> dict[str, Any]:
        return {
            "mailId": mail_id,
            "userId": user_id,
            "senderEmail": "teacher@example.com",
            "subject": "项目阶段汇报提醒",
            "contentText": "请各组在明天下午前提交项目进度，并准备 5 分钟阶段性演示。需要说明已完成的基础邮件功能、数据库设计和 AI 插件计划。",
            "contentHtml": None,
            "priority": "NORMAL",
            "recipients": ["student@example.com"],
            "mockReason": reason,
        }


def to_mail_context(data: dict[str, Any]) -> MailContext:
    return MailContext(
        mail_id=data.get("mailId") or data.get("mail_id"),
        user_id=data.get("userId") or data.get("user_id"),
        sender_email=data.get("senderEmail") or data.get("sender_email"),
        subject=data.get("subject", ""),
        content_text=data.get("contentText") or data.get("content_text") or "",
        content_html=data.get("contentHtml") or data.get("content_html"),
        priority=data.get("priority", "NORMAL"),
        recipients=data.get("recipients", []),
    )
