import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    plugin_version: str = os.getenv("SMARTMAIL_AGENT_PLUGIN_VERSION", "0.1.0")
    backend_base_url: str = os.getenv("SMARTMAIL_BACKEND_BASE_URL", "http://localhost:8080")
    internal_token: str = os.getenv("SMARTMAIL_INTERNAL_TOKEN", "smartmail-internal-dev-token")
    plugin_token: str = os.getenv("SMARTMAIL_PLUGIN_TOKEN", "smartmail-agent-plugin-dev-token")
    provider: str = os.getenv("SMARTMAIL_AGENT_PROVIDER", "RULES")
    rag_mode: str = os.getenv("SMARTMAIL_AGENT_RAG_MODE", "MOCK")
    mock_on_tool_error: bool = os.getenv("SMARTMAIL_AGENT_MOCK_ON_TOOL_ERROR", "true").lower() == "true"
    analysis_timeout_seconds: float = float(os.getenv("SMARTMAIL_AGENT_ANALYSIS_TIMEOUT_SECONDS", "10"))
    chat_timeout_seconds: float = float(os.getenv("SMARTMAIL_AGENT_CHAT_TIMEOUT_SECONDS", "15"))
    # Backward-compatible alias used by legacy client code
    timeout_seconds: float = float(os.getenv("SMARTMAIL_AGENT_TIMEOUT_SECONDS", "5"))
    llm_api_key: str | None = os.getenv("SMARTMAIL_LLM_API_KEY")
    deepseek_base_url: str = os.getenv("SMARTMAIL_DEEPSEEK_BASE_URL", "https://api.deepseek.com")
    deepseek_model: str = os.getenv("SMARTMAIL_DEEPSEEK_MODEL", "deepseek-chat")
    log_level: str = os.getenv("SMARTMAIL_LOG_LEVEL", "INFO")

    def safe_dict(self) -> dict[str, str | bool | float | None]:
        """Return a dict safe for logging (secrets redacted)."""
        return {
            "plugin_version": self.plugin_version,
            "backend_base_url": self.backend_base_url,
            "internal_token": "***",
            "plugin_token": "***",
            "provider": self.provider,
            "rag_mode": self.rag_mode,
            "mock_on_tool_error": self.mock_on_tool_error,
            "analysis_timeout_seconds": self.analysis_timeout_seconds,
            "chat_timeout_seconds": self.chat_timeout_seconds,
            "timeout_seconds": self.timeout_seconds,
            "llm_api_key": "***" if self.llm_api_key else None,
            "deepseek_base_url": self.deepseek_base_url,
            "deepseek_model": self.deepseek_model,
            "log_level": self.log_level,
        }


settings = Settings()
