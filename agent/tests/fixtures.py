from __future__ import annotations

from copy import deepcopy
from typing import Any


ANALYSIS_REQUESTS: dict[str, dict[str, Any]] = {
    "normal_incoming_mail": {
        "taskId": "fixture-normal",
        "userId": "user-1",
        "mailItemId": "mail-normal",
        "mail": {
            "subject": "Project report approval",
            "content": "Please review the project report before approval.",
            "senderEmail": "manager@example.com",
        },
        "userCategories": ["Work", "Other"],
        "behaviorSignals": {"frequentSender": True, "repliedSender": True},
        "pluginConfig": {"aiPluginEnabled": True, "llmEnabled": False},
    },
    "junk_phishing_like_mail": {
        "taskId": "fixture-junk",
        "userId": "user-1",
        "mailItemId": "mail-junk",
        "mail": {
            "subject": "Urgent click to claim your prize",
            "content": (
                "Winner! Click here https://example.test to verify your account. "
                "Your verification code and free prize expire today."
            ),
            "senderEmail": "notice@secure-prize.xyz",
        },
        "userCategories": [
            {"id": 1, "name": "Work"},
            {"id": 2, "name": "Junk Mail"},
            {"id": 3, "name": "Finance"},
        ],
        "behaviorSignals": {"recentJunkSender": True, "frequentSender": True},
        "pluginConfig": {"aiPluginEnabled": True, "llmEnabled": False},
    },
    "disabled_ai_plugin": {
        "taskId": "fixture-disabled",
        "userId": "user-1",
        "mailItemId": "mail-disabled",
        "mail": {
            "subject": "Quarterly review",
            "content": "Please review the report.",
            "senderEmail": "manager@example.com",
        },
        "userCategories": ["Work", "Junk Mail"],
        "behaviorSignals": {"frequentSender": True},
        "pluginConfig": {"aiPluginEnabled": False, "llmEnabled": False},
    },
    "category_id_name_preservation": {
        "taskId": "fixture-category-object",
        "userId": "user-1",
        "mailItemId": "mail-category-object",
        "mail": {
            "subject": "Project report approval",
            "content": "Please review the project report before approval.",
            "senderEmail": "manager@example.com",
        },
        "userCategories": [
            {"id": 7, "name": "Project"},
            {"id": 8, "name": "Other"},
        ],
        "behaviorSignals": {},
        "pluginConfig": {"aiPluginEnabled": True, "llmEnabled": False},
    },
    "deepseek_configured_rules_fallback": {
        "taskId": "fixture-model-info",
        "userId": "user-1",
        "mailItemId": "mail-model-info",
        "mail": {"subject": "Status", "content": "Project status update."},
        "userCategories": ["Project", "Other"],
        "behaviorSignals": {},
        "pluginConfig": {
            "aiPluginEnabled": True,
            "llmEnabled": True,
            "apiKey": "test-key",
        },
    },
}


EXPECTED_ANALYSIS_RESPONSES: dict[str, dict[str, Any]] = {
    "normal_incoming_mail": {
        "status": "SUCCEEDED",
        "category": {"id": None, "name": "Work"},
        "junk": False,
        "priority": "URGENT",
        "riskLevel": "LOW",
        "modelInfo": {
            "provider": "RULES",
            "mode": "rules-fallback",
            "fallbackUsed": True,
        },
    },
    "junk_phishing_like_mail": {
        "status": "SUCCEEDED",
        "category": {"id": 2, "name": "Junk Mail"},
        "junk": True,
        "priority": "LOW",
        "riskLevel": "HIGH",
        "modelInfo": {
            "provider": "RULES",
            "mode": "rules-fallback",
            "fallbackUsed": True,
        },
    },
    "disabled_ai_plugin": {
        "status": "DISABLED",
        "summary": [],
        "category": None,
        "junk": False,
        "priority": None,
        "priorityScore": 0,
        "riskLevel": "LOW",
        "riskHints": [],
        "modelInfo": {
            "provider": "RULES",
            "mode": "rules-fallback",
            "fallbackUsed": True,
        },
    },
    "category_id_name_preservation": {
        "status": "SUCCEEDED",
        "category": {"id": 7, "name": "Project"},
        "junk": False,
        "priority": "NORMAL",
        "riskLevel": "LOW",
        "modelInfo": {
            "provider": "RULES",
            "mode": "rules-fallback",
            "fallbackUsed": True,
        },
    },
    "deepseek_configured_rules_fallback": {
        "status": "SUCCEEDED",
        "category": {"id": None, "name": "Project"},
        "modelInfo": {
            "provider": "DEEPSEEK",
            "mode": "rules-fallback",
            "llmEnabled": True,
            "fallbackUsed": True,
        },
    },
}


def analysis_request(name: str) -> dict[str, Any]:
    return deepcopy(ANALYSIS_REQUESTS[name])
