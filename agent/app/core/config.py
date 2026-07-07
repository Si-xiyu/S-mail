from __future__ import annotations

import os
from dataclasses import dataclass
from pathlib import Path
from typing import Any

try:
    import tomllib
except ModuleNotFoundError:  # pragma: no cover - Python < 3.11
    import tomli as tomllib


AGENT_ROOT = Path(__file__).resolve().parents[2]


@dataclass(frozen=True)
class FeatureProviderConfig:
    provider: str
    model: str
    base_url: str
    api_key_env: str
    timeout_seconds: float


@dataclass(frozen=True)
class Settings:
    plugin_version: str = os.getenv("SMARTMAIL_AGENT_PLUGIN_VERSION", "0.2.0")
    backend_base_url: str = os.getenv("SMARTMAIL_BACKEND_BASE_URL", "http://localhost:8080")
    internal_token: str = os.getenv("SMARTMAIL_INTERNAL_TOKEN", "smartmail-internal-dev-token")
    plugin_token: str = os.getenv("SMARTMAIL_PLUGIN_TOKEN", "smartmail-agent-plugin-dev-token")
    rag_mode: str = os.getenv("SMARTMAIL_AGENT_RAG_MODE", "MOCK")
    mock_on_tool_error: bool = os.getenv("SMARTMAIL_AGENT_MOCK_ON_TOOL_ERROR", "false").lower() == "true"
    config_path: str = os.getenv("SMARTMAIL_AGENT_CONFIG", str(AGENT_ROOT / "config" / "providers.toml"))
    log_level: str = os.getenv("SMARTMAIL_LOG_LEVEL", "INFO")

    @property
    def analysis(self) -> FeatureProviderConfig:
        return _feature_config("analysis", self.config_path)

    @property
    def chat(self) -> FeatureProviderConfig:
        return _feature_config("chat", self.config_path)

    @property
    def analysis_timeout_seconds(self) -> float:
        return self.analysis.timeout_seconds

    @property
    def chat_timeout_seconds(self) -> float:
        return self.chat.timeout_seconds

    @property
    def timeout_seconds(self) -> float:
        return float(os.getenv("SMARTMAIL_AGENT_TIMEOUT_SECONDS", "5"))

    @property
    def llm_api_key(self) -> str | None:
        return self.api_key_for("analysis")

    @property
    def provider(self) -> str:
        return self.analysis.provider.upper()

    @property
    def deepseek_base_url(self) -> str:
        return self.analysis.base_url

    @property
    def deepseek_model(self) -> str:
        return self.analysis.model

    def api_key_for(self, feature: str) -> str | None:
        return _api_key(self.analysis if feature == "analysis" else self.chat)

    def safe_dict(self) -> dict[str, Any]:
        return {
            "plugin_version": self.plugin_version,
            "backend_base_url": self.backend_base_url,
            "rag_mode": self.rag_mode,
            "mock_on_tool_error": self.mock_on_tool_error,
            "analysis": self.analysis,
            "chat": self.chat,
            "secrets": "***",
        }


def _load_config(path: str) -> dict[str, Any]:
    file = Path(path)
    if not file.is_file():
        return {}
    with file.open("rb") as handle:
        return tomllib.load(handle)


def _feature_config(feature: str, path: str) -> FeatureProviderConfig:
    data = _load_config(path)
    feature_data = data.get("features", {}).get(feature, {})
    default_provider = "rules" if feature == "analysis" else "deepseek"
    provider_name = str(feature_data.get("provider", default_provider)).lower()
    provider_data = data.get("providers", {}).get(provider_name, {})
    env_prefix = "SMARTMAIL_AGENT_ANALYSIS" if feature == "analysis" else "SMARTMAIL_AGENT_CHAT"

    return FeatureProviderConfig(
        provider=os.getenv(f"{env_prefix}_PROVIDER", provider_name).lower(),
        model=os.getenv(
            f"{env_prefix}_MODEL",
            str(feature_data.get("model") or provider_data.get("model") or "deepseek-chat"),
        ),
        base_url=os.getenv(
            f"{env_prefix}_BASE_URL",
            str(provider_data.get("base_url") or "https://api.deepseek.com"),
        ).rstrip("/"),
        api_key_env=str(provider_data.get("api_key_env") or "DEEPSEEK_API_KEY"),
        timeout_seconds=float(
            os.getenv(
                f"{env_prefix}_TIMEOUT_SECONDS",
                str(feature_data.get("timeout_seconds") or provider_data.get("timeout_seconds") or 30),
            )
        ),
    )


def _api_key(config: FeatureProviderConfig) -> str | None:
    return os.getenv(config.api_key_env) or os.getenv("SMARTMAIL_LLM_API_KEY")


settings = Settings()
