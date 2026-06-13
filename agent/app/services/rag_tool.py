from __future__ import annotations

from typing import Any

from app.schemas.plugin import ToolCallRecord


class RagTool:
    """MVP retrieval boundary for future BM25 + vector + RRF implementation."""

    def search(self, query: str, context: dict[str, Any]) -> tuple[list[dict[str, Any]], ToolCallRecord]:
        records = [
            {
                "id": "mock-mail-001",
                "source": "MOCK",
                "title": "Project progress reminder",
                "snippet": "A mock mail asks the recipient to submit project progress and prepare a short update.",
                "score": 0.72,
            },
            {
                "id": "mock-mail-002",
                "source": "MOCK",
                "title": "Security notice",
                "snippet": "A mock security notice recommends checking sender identity before opening links.",
                "score": 0.61,
            },
        ]
        limit = _coerce_positive_int(context.get("limit"), default=2)
        selected = records[:limit]
        return selected, ToolCallRecord(
            tool="rag_tool",
            status="MOCK",
            input={"query": query, "futurePipeline": ["BM25", "VECTOR", "RRF"]},
            output={"records": selected},
            source="MOCK",
        )


def _coerce_positive_int(value: Any, default: int) -> int:
    try:
        parsed = int(value)
    except (TypeError, ValueError):
        return default
    return parsed if parsed > 0 else default
