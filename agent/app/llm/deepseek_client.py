from __future__ import annotations

import json
import re
from typing import Any, Literal

import httpx

from app.core.config import FeatureProviderConfig, settings


FeatureName = Literal["analysis", "chat"]


class DeepSeekClient:
    """OpenAI-compatible client used by analysis and chat channels."""

    def __init__(
        self,
        feature: FeatureName = "analysis",
        api_key: str | None = None,
        http_client: Any | None = None,
    ) -> None:
        self.feature = feature
        self.config: FeatureProviderConfig = settings.analysis if feature == "analysis" else settings.chat
        self.base_url = self.config.base_url.rstrip("/")
        self.api_key = api_key or settings.api_key_for(feature)
        self.model = self.config.model
        self.timeout = self.config.timeout_seconds
        self.http_client = http_client or httpx

    def available(self) -> bool:
        return self.config.provider.lower() != "rules" and bool(self.api_key)

    def chat_completion(
        self,
        messages: list[dict[str, str]],
        temperature: float = 0.3,
        max_tokens: int = 1024,
    ) -> dict[str, Any]:
        if not self.api_key:
            raise DeepSeekError("DEEPSEEK_NO_KEY", "DeepSeek API key is not configured")

        try:
            response = self.http_client.post(
                f"{self.base_url}/chat/completions",
                headers={
                    "Authorization": f"Bearer {self.api_key}",
                    "Content-Type": "application/json",
                },
                json={
                    "model": self.model,
                    "messages": messages,
                    "temperature": temperature,
                    "max_tokens": max_tokens,
                },
                timeout=self.timeout,
            )
            response.raise_for_status()
            return response.json()
        except httpx.TimeoutException as exc:
            raise DeepSeekError("DEEPSEEK_TIMEOUT", f"DeepSeek request timed out: {exc}") from exc
        except httpx.HTTPStatusError as exc:
            body = _safe_json(exc.response)
            message = body.get("error", {}).get("message") if isinstance(body, dict) else None
            raise DeepSeekError(
                f"DEEPSEEK_HTTP_{exc.response.status_code}",
                message or f"DeepSeek HTTP error: {exc.response.status_code}",
            ) from exc
        except DeepSeekError:
            raise
        except Exception as exc:
            raise DeepSeekError("DEEPSEEK_ERROR", str(exc)) from exc

    def text_completion(
        self,
        messages: list[dict[str, str]],
        temperature: float = 0.2,
        max_tokens: int = 800,
    ) -> str:
        return _extract_content(self.chat_completion(messages, temperature=temperature, max_tokens=max_tokens))

    def json_completion(
        self,
        messages: list[dict[str, str]],
        temperature: float = 0.1,
        max_tokens: int = 800,
    ) -> dict[str, Any]:
        return _parse_json_object(self.text_completion(messages, temperature=temperature, max_tokens=max_tokens))

    def generate_analysis(
        self,
        subject: str,
        content: str,
        sender: str,
        user_categories: list[Any],
    ) -> dict[str, Any]:
        system_prompt = """You are an email analysis assistant. Analyze the email and return a JSON object with exactly these keys:
- summary: list of 1-3 short Chinese bullet points
- categoryName: one of the provided user categories, or "Other"
- junk: boolean
- priority: one of LOW, NORMAL, HIGH, URGENT
- priorityScore: integer 0-100
- riskLevel: one of NONE, LOW, MEDIUM, HIGH
- riskHints: list of short Chinese risk hints, empty if none
Return only the JSON object, no markdown."""

        category_names = [str(c.get("name") if isinstance(c, dict) else c) for c in user_categories]
        user_prompt = f"""User categories: {category_names}
Sender: {sender}
Subject: {subject}
Content:
{content}
"""
        return self.json_completion(
            [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.1,
            max_tokens=900,
        )

    def answer_current_mail(self, message: str, mail: dict[str, Any]) -> str:
        system_prompt = (
            "你是 SmartMail 邮件助手。只基于给定邮件内容回答；不知道就说不知道。"
            "不要声称已经执行写操作；涉及删除、移动、标记等操作时，只说明需要用户确认。"
        )
        user_prompt = f"""用户问题：{message}

当前邮件：
发件人：{mail.get('senderEmail') or mail.get('sender_email')}
主题：{mail.get('subject')}
正文：{mail.get('contentText') or mail.get('content_text') or ''}
"""
        return self.text_completion(
            [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            temperature=0.2,
            max_tokens=600,
        )

    def answer_search_results(self, message: str, records: list[dict[str, Any]]) -> str:
        compact_records = [
            {
                "mailItemId": r.get("mailItemId") or r.get("itemId"),
                "senderEmail": r.get("senderEmail"),
                "subject": r.get("subject") or r.get("title"),
                "snippet": r.get("snippet") or r.get("preview"),
                "folder": r.get("folder"),
            }
            for r in records[:5]
        ]
        system_prompt = (
            "你是 SmartMail 邮件助手。根据检索结果用中文简洁回答。"
            "不要编造不存在的邮件；写操作必须提示等待用户确认。"
        )
        return self.text_completion(
            [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": f"用户问题：{message}\n检索结果 JSON：{json.dumps(compact_records, ensure_ascii=False)}"},
            ],
            temperature=0.2,
            max_tokens=700,
        )

    def extract_search_query(self, message: str) -> str:
        system_prompt = (
            "从用户中文或英文邮件助手请求中抽取后端邮件搜索关键词。"
            "只返回 JSON：{\"query\":\"关键词\"}。如果没有明确关键词，返回用户原句中的核心名词。"
        )
        raw = self.json_completion(
            [
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": message},
            ],
            temperature=0.0,
            max_tokens=120,
        )
        query = str(raw.get("query") or "").strip()
        return query or message


class DeepSeekError(Exception):
    def __init__(self, code: str, message: str) -> None:
        self.code = code
        self.message = message
        super().__init__(f"[{code}] {message}")


def _extract_content(completion: dict[str, Any]) -> str:
    choices = completion.get("choices", [])
    if not choices:
        raise DeepSeekError("DEEPSEEK_EMPTY", "Empty completion choices")
    message = choices[0].get("message", {})
    return str(message.get("content", "")).strip()


def _parse_json_object(text: str) -> dict[str, Any]:
    cleaned = _strip_markdown_fence(text)
    try:
        parsed = json.loads(cleaned)
    except json.JSONDecodeError:
        match = re.search(r"\{.*\}", cleaned, flags=re.DOTALL)
        if not match:
            raise DeepSeekError("DEEPSEEK_JSON", "Response does not contain a JSON object")
        try:
            parsed = json.loads(match.group(0))
        except json.JSONDecodeError as exc:
            raise DeepSeekError("DEEPSEEK_JSON", f"Invalid JSON: {exc}") from exc
    if not isinstance(parsed, dict):
        raise DeepSeekError("DEEPSEEK_JSON", "Response is not a JSON object")
    return parsed


def _strip_markdown_fence(text: str) -> str:
    cleaned = text.strip()
    if cleaned.startswith("```"):
        cleaned = cleaned.removeprefix("```").strip()
        if cleaned.lower().startswith("json"):
            cleaned = cleaned[4:].strip()
        if cleaned.endswith("```"):
            cleaned = cleaned[:-3].strip()
    return cleaned


def _safe_json(response: httpx.Response) -> Any:
    try:
        return response.json()
    except Exception:
        return {}
