from __future__ import annotations

import json
from typing import Any

import httpx

from app.core.config import settings
from app.schemas.agent import MailContext, ToolResult


class BackendToolClient:
    """Client for the backend Internal Tool API.

    All methods return a :class:`ToolResult`. Network errors are captured and
    classified so callers can decide whether to use explicit mock fallback.
    """

    def __init__(self) -> None:
        self.base_url = settings.backend_base_url.rstrip("/")
        self.headers = {"X-Internal-Token": settings.internal_token}

    # ------------------------------------------------------------------
    # Context
    # ------------------------------------------------------------------
    def get_current_mail_context(self, context: dict[str, Any], user_id: int | str) -> ToolResult:
        """Fetch the current-mail context from the backend.

        The canonical backend contract uses ``mailItemId``. ``mailId`` is
        accepted as a legacy alias but is never sent to the backend as-is.
        """
        mail_item_id = _first_present(context, "mailItemId", "mail_item_id", "mailId", "mail_id")
        if mail_item_id is None:
            if not settings.mock_on_tool_error:
                return ToolResult(ok=False, error="CURRENT_MAIL context must include mailItemId or mailId")
            return ToolResult(
                ok=True,
                data={
                    **self._mock_mail(0, _coerce_user_id(user_id), "missing mailItemId/mailId in plugin context"),
                    "source": "MOCK",
                },
            )

        item_id = _coerce_int(mail_item_id)
        result = self._get_mail_item_context(item_id, _coerce_user_id(user_id))
        if result.ok and result.data is not None:
            result.data.setdefault("source", "BACKEND")
            return result

        # Legacy fallback: some older backend builds expose /mails/{mailId}
        legacy = self.get_mail(item_id, _coerce_user_id(user_id))
        if legacy.ok and legacy.data is not None:
            legacy.data.setdefault("source", "BACKEND")
            return legacy

        if settings.mock_on_tool_error:
            return ToolResult(
                ok=True,
                data={
                    **self._mock_mail(item_id, _coerce_user_id(user_id), result.error or "context endpoint failed"),
                    "source": "MOCK",
                },
            )
        return result

    def _get_mail_item_context(self, mail_item_id: int, user_id: int) -> ToolResult:
        return self._get_json(
            f"/internal/v1/tools/mail-items/{mail_item_id}/context",
            params={"userId": user_id},
            timeout=settings.chat_timeout_seconds,
        )

    def get_mail(self, mail_id: int, user_id: int) -> ToolResult:
        return self._get_json(
            f"/internal/v1/tools/mails/{mail_id}",
            params={"userId": user_id},
            timeout=settings.chat_timeout_seconds,
        )

    # ------------------------------------------------------------------
    # Search
    # ------------------------------------------------------------------
    def search_mail(
        self,
        user_id: int | str,
        query: str,
        folder: str | None = None,
        limit: int = 10,
    ) -> ToolResult:
        params: dict[str, Any] = {"userId": _coerce_user_id(user_id), "keyword": query, "limit": limit}
        if folder is not None:
            params["folder"] = folder.upper()
        return self._get_json("/internal/v1/tools/mail-search", params=params, timeout=settings.chat_timeout_seconds)

    # ------------------------------------------------------------------
    # Writebacks
    # ------------------------------------------------------------------
    def save_ai_result(self, mail_id: int, user_id: int, result_type: str, result: dict[str, Any]) -> ToolResult:
        return self._post_json(
            "/internal/v1/tools/analysis-results",
            json={
                "mailId": mail_id,
                "userId": user_id,
                "resultType": result_type.upper(),
                "resultJson": json.dumps(result, ensure_ascii=False),
                "status": "SUCCEEDED",
            },
            timeout=settings.analysis_timeout_seconds,
        )

    def set_priority(self, mail_id: int, user_id: int, priority: str) -> ToolResult:
        return self._post_json(
            f"/internal/v1/tools/mails/{mail_id}/priority",
            json={"userId": user_id, "priority": priority},
            timeout=settings.chat_timeout_seconds,
        )

    # ------------------------------------------------------------------
    # HTTP helpers
    # ------------------------------------------------------------------
    def _get_json(self, path: str, params: dict[str, Any] | None = None, timeout: float | None = None) -> ToolResult:
        try:
            response = httpx.get(
                f"{self.base_url}{path}",
                params=params,
                headers=self.headers,
                timeout=timeout,
            )
            return self._parse_response(response)
        except Exception as exc:
            return ToolResult(ok=False, error=self._classify_error(exc))

    def _post_json(self, path: str, json: dict[str, Any], timeout: float | None = None) -> ToolResult:
        try:
            response = httpx.post(
                f"{self.base_url}{path}",
                headers=self.headers,
                json=json,
                timeout=timeout,
            )
            return self._parse_response(response)
        except Exception as exc:
            return ToolResult(ok=False, error=self._classify_error(exc))

    def _parse_response(self, response: httpx.Response) -> ToolResult:
        try:
            response.raise_for_status()
        except httpx.HTTPStatusError as exc:
            detail = f"backend returned {exc.response.status_code}"
            try:
                body = exc.response.json()
                if isinstance(body, dict) and body.get("message"):
                    detail += f": {body['message']}"
            except Exception:
                pass
            return ToolResult(ok=False, error=detail)

        try:
            envelope = response.json()
        except Exception as exc:
            return ToolResult(ok=False, error=f"invalid JSON from backend: {exc}")

        if not isinstance(envelope, dict):
            return ToolResult(ok=True, data=envelope)

        code = envelope.get("code")
        if code is not None and code != 0:
            message = envelope.get("message", "backend error")
            return ToolResult(ok=False, error=f"backend error {code}: {message}")

        data = envelope.get("data", envelope)
        return ToolResult(ok=True, data=data)

    @staticmethod
    def _classify_error(exc: Exception) -> str:
        if isinstance(exc, httpx.TimeoutException):
            return f"BACKEND_TIMEOUT: {exc}"
        if isinstance(exc, httpx.ConnectError):
            return f"BACKEND_UNREACHABLE: {exc}"
        if isinstance(exc, httpx.HTTPStatusError):
            return f"BACKEND_HTTP_{exc.response.status_code}: {exc}"
        return f"BACKEND_ERROR: {exc}"

    def _mock_mail(self, mail_id: int, user_id: int, reason: str) -> dict[str, Any]:
        return {
            "mailId": mail_id,
            "mailItemId": mail_id,
            "itemId": mail_id,
            "userId": user_id,
            "senderEmail": "teacher@example.com",
            "subject": "项目阶段汇报提醒",
            "contentText": "请各组在明天下午前提交项目进度，并准备 5 分钟阶段性演示。需要说明已完成的基础邮件功能、数据库设计和 AI 插件计划。",
            "contentHtml": None,
            "folder": "INBOX",
            "priority": "NORMAL",
            "recipients": ["student@example.com"],
            "mockReason": reason,
        }


def to_mail_context(data: dict[str, Any]) -> MailContext:
    return MailContext(
        mail_id=_first_present(data, "mailItemId", "mail_item_id", "mailId", "mail_id"),
        user_id=_first_present(data, "userId", "user_id"),
        sender_email=_first_present(data, "senderEmail", "sender_email"),
        subject=data.get("subject", ""),
        content_text=data.get("contentText") or data.get("content_text") or "",
        content_html=data.get("contentHtml") or data.get("content_html"),
        priority=data.get("priority", "NORMAL"),
        recipients=data.get("recipients", []),
    )


def _coerce_int(value: Any) -> int:
    try:
        return int(value)
    except (TypeError, ValueError):
        return 0


def _coerce_user_id(value: int | str) -> int:
    return _coerce_int(value)


def _first_present(data: dict[str, Any], *keys: str) -> Any:
    for key in keys:
        if key in data:
            return data[key]
    return None
