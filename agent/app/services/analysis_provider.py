from __future__ import annotations

from app.core.config import settings
from app.schemas.agent import AnalysisRequest, AnalysisResponse, ModelInfo
from app.services.rule_engine import RuleEngine


class RulesAnalysisProvider:
    def __init__(self, rule_engine: RuleEngine | None = None) -> None:
        self.rule_engine = rule_engine or RuleEngine()

    def analyze(self, request: AnalysisRequest) -> AnalysisResponse:
        return self.rule_engine.analysis_response(
            subject=request.mail.subject,
            content=request.mail.body_text(),
            sender=request.mail.sender_text(),
            user_categories=request.user_categories,
            behavior_signals=request.behavior_signals,
            model_info=self.model_info(request),
        )

    def model_info(self, request: AnalysisRequest) -> ModelInfo:
        api_key = request.plugin_config.api_key or settings.llm_api_key
        llm_available = bool(request.plugin_config.llm_enabled and api_key)
        if llm_available:
            return ModelInfo(
                provider="DEEPSEEK",
                model="deepseek-chat",
                mode="rules-fallback",
                llmEnabled=True,
                ragTool="MOCK",
                fallbackUsed=True,
            )
        return ModelInfo(
            provider="RULES",
            model="deterministic-rules-v1",
            mode="rules-fallback",
            llmEnabled=False,
            ragTool="MOCK",
            fallbackUsed=True,
        )
