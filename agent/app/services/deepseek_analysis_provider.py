from __future__ import annotations

from typing import Any

from app.core.config import settings
from app.llm.deepseek_client import DeepSeekClient, DeepSeekError
from app.schemas.agent import AnalysisCategory, AnalysisRequest, AnalysisResponse, ModelInfo
from app.services.analysis_provider import RulesAnalysisProvider


class DeepSeekAnalysisProvider:
    def __init__(self, deepseek_client: DeepSeekClient | None = None) -> None:
        self.client = deepseek_client or DeepSeekClient(feature="analysis")
        self.fallback = RulesAnalysisProvider()

    def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        if not self.client.available():
            return self._fallback(request, "DEEPSEEK_NO_KEY")

        try:
            raw = self.client.generate_analysis(
                subject=request.mail.subject,
                content=request.mail.body_text(),
                sender=request.mail.sender_text(),
                user_categories=request.user_categories,
            )
        except DeepSeekError as exc:
            return self._fallback(request, exc.code)

        return self._to_response(request, raw)

    def model_info(self, request: AnalysisRequest) -> ModelInfo:
        return ModelInfo(
            provider="DEEPSEEK",
            model=settings.analysis.model,
            mode="llm",
            llmEnabled=True,
            ragTool=settings.rag_mode,
            fallbackUsed=False,
        )

    def _to_response(self, request: AnalysisRequest, raw: dict[str, Any]) -> AnalysisResponse:
        category = self._resolve_category(raw.get("categoryName"), request.user_categories)
        return AnalysisResponse(
            status="SUCCEEDED",
            summary=_as_string_list(raw.get("summary"), fallback=self._fallback_summary(request)),
            category=category,
            junk=bool(raw.get("junk", False)),
            priority=_normalize_priority(raw.get("priority")),
            priorityScore=_coerce_score(raw.get("priorityScore")),
            riskLevel=_normalize_risk(raw.get("riskLevel")),
            riskHints=_as_string_list(raw.get("riskHints")),
            modelInfo=self.model_info(request),
        )

    def _fallback(self, request: AnalysisRequest, reason: str) -> AnalysisResponse:
        response = self.fallback.analyze(request)
        response.model_info.provider = "DEEPSEEK"
        response.model_info.mode = "rules-fallback"
        response.model_info.llm_enabled = bool(request.plugin_config.api_key or settings.api_key_for("analysis"))
        response.model_info.fallback_used = True
        return response

    def _resolve_category(self, name: Any, user_categories: list[Any]) -> AnalysisCategory | None:
        if not name:
            return None
        text = str(name).strip()
        for category in user_categories:
            if isinstance(category, dict):
                if category.get("name") == text:
                    return AnalysisCategory(id=category.get("id"), name=text)
            elif isinstance(category, str) and category.strip() == text:
                return AnalysisCategory(name=text)
        return AnalysisCategory(name=text)

    def _fallback_summary(self, request: AnalysisRequest) -> list[str]:
        fallback = self.fallback.rule_engine.summarize(request.mail)
        return fallback.get("summary", [])


def _as_string_list(value: Any, fallback: list[str] | None = None) -> list[str]:
    if isinstance(value, list):
        return [str(item) for item in value]
    if fallback is not None:
        return fallback
    return []


def _normalize_priority(value: Any) -> str | None:
    if not value:
        return None
    text = str(value).strip().upper()
    return text if text in {"LOW", "NORMAL", "HIGH", "URGENT"} else None


def _normalize_risk(value: Any) -> str:
    if not value:
        return "NONE"
    text = str(value).strip().upper()
    return text if text in {"NONE", "LOW", "MEDIUM", "HIGH"} else "NONE"


def _coerce_score(value: Any) -> int:
    try:
        score = int(value)
    except (TypeError, ValueError):
        return 0
    return max(0, min(100, score))
