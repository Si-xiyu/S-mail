import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    plugin_version: str = os.getenv("SMARTMAIL_AGENT_PLUGIN_VERSION", "0.1.0")
    backend_base_url: str = os.getenv("SMARTMAIL_BACKEND_BASE_URL", "http://localhost:8080")
    internal_token: str = os.getenv("SMARTMAIL_INTERNAL_TOKEN", "smartmail-internal-dev-token")
    mock_on_tool_error: bool = os.getenv("SMARTMAIL_AGENT_MOCK_ON_TOOL_ERROR", "true").lower() == "true"
    timeout_seconds: float = float(os.getenv("SMARTMAIL_AGENT_TIMEOUT_SECONDS", "5"))
    llm_api_key: str | None = os.getenv("SMARTMAIL_LLM_API_KEY")


settings = Settings()
