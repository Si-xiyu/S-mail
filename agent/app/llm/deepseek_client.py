from __future__ import annotations

import json
from typing import Any

import httpx

from app.core.config import settings


class DeepSeekClient:
    """Minimal OpenAI-compatible client for DeepSeek Chat API."""

    def __init__(self) -> None:
        self.base_url = settings.deepseek_base_url.rstrip("/")
        self.api_key = settings.llm_api_key
        self.model = settings.deepseek_model
        self.timeout = settings.analysis_timeout_seconds

    def available(self) -> bool:
        return bool(self.api_key)

    def chat_completion(
        self,
        messages: list[dict[str, str]],
        temperature: float = 0.3,
        max_tokens: int = 1024,
    ) -> dict[str, Any]:
        if not self.api_key:
            raise DeepSeekError("DEEPSEEK_NO_KEY", "DeepSeek API key is not configured")

        try:
            response = httpx.post(
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
        except Exception as exc:
            raise DeepSeekError("DEEPSEEK_ERROR", str(exc)) from exc

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
        messages = [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": user_prompt},
        ]
        completion = self.chat_completion(messages)
        text = _extract_content(completion)
        return _parse_json_object(text)


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
    # Some models wrap JSON in markdown fences.
    cleaned = text
    if cleaned.startswith("```"):
        cleaned = cleaned.strip("`")
        if cleaned.lower().startswith("json"):
            cleaned = cleaned[4:]
    cleaned = cleaned.strip()
    try:
        parsed = json.loads(cleaned)
    except json.JSONDecodeError as exc:
        raise DeepSeekError("DEEPSEEK_JSON", f"Invalid JSON: {exc}") from exc
    if not isinstance(parsed, dict):
        raise DeepSeekError("DEEPSEEK_JSON", "Response is not a JSON object")
    return parsed


def _safe_json(response: httpx.Response) -> Any:
    try:
        return response.json()
    except Exception:
        return {}
