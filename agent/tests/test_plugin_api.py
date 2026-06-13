import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import app
from app.schemas.agent import ToolResult


class PluginApiTests(unittest.TestCase):
    def setUp(self) -> None:
        self.client = TestClient(app)

    def test_plugin_health_contract(self) -> None:
        response = self.client.get("/plugin/v1/health")

        self.assertEqual(response.status_code, 200)
        self.assertEqual(
            response.json(),
            {
                "status": "UP",
                "pluginVersion": "0.1.0",
                "capabilities": {
                    "rules": True,
                    "llm": False,
                    "currentMailAgent": True,
                    "ragTool": "MOCK",
                },
            },
        )

    def test_analysis_disabled_returns_no_analysis(self) -> None:
        response = self.client.post(
            "/plugin/v1/analysis/mail",
            json={
                "taskId": "task-1",
                "userId": "user-1",
                "mailItemId": "mail-1",
                "mail": {"subject": "Quarterly review", "content": "Please review the report."},
                "userCategories": ["Work", "Junk Mail"],
                "behaviorSignals": {"frequentSender": True},
                "pluginConfig": {"aiPluginEnabled": False, "llmEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "DISABLED")
        self.assertEqual(body["summary"], [])
        self.assertIsNone(body["category"])
        self.assertIsNone(body["priority"])

    def test_rules_fallback_detects_junk_risk_and_priority(self) -> None:
        response = self.client.post(
            "/plugin/v1/analysis/mail",
            json={
                "taskId": "task-2",
                "userId": "user-1",
                "mailItemId": "mail-2",
                "mail": {
                    "subject": "Urgent click to claim your prize",
                    "content": (
                        "Winner! Click here https://example.test to verify your account. "
                        "Your verification code and free prize expire today."
                    ),
                    "senderEmail": "notice@secure-prize.xyz",
                },
                "userCategories": ["Work", "Junk Mail", "Finance"],
                "behaviorSignals": {"recentJunkSender": True, "frequentSender": True},
                "pluginConfig": {"aiPluginEnabled": True, "llmEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "SUCCEEDED")
        self.assertEqual(body["category"], "Junk Mail")
        self.assertIs(body["junk"], True)
        self.assertEqual(body["priority"], "LOW")
        self.assertGreaterEqual(body["priorityScore"], 0)
        self.assertLessEqual(body["priorityScore"], 39)
        self.assertIn(body["riskLevel"], {"HIGH", "CRITICAL"})
        self.assertLessEqual(len(body["summary"]), 3)
        self.assertEqual(body["modelInfo"]["mode"], "rules-fallback")

    def test_agent_chat_disabled_plugin(self) -> None:
        response = self.client.post(
            "/plugin/v1/agent/chat",
            json={
                "sessionId": "s1",
                "userId": 1,
                "scope": "CURRENT_MAIL",
                "message": "What should I do with this email?",
                "pluginConfig": {"aiPluginEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "DISABLED")
        self.assertEqual(body["toolCalls"], [])
        self.assertEqual(body["pendingActions"], [])

    def test_agent_chat_current_mail_returns_succeeded(self) -> None:
        with patch("app.api.routes.tool_router.backend_tools.get_current_mail_context") as get_context:
            get_context.return_value = ToolResult(
                ok=True,
                data={
                    "mailId": 88,
                    "userId": 1,
                    "senderEmail": "teacher@example.com",
                    "subject": "Project update",
                    "contentText": "Please send the project update by tomorrow.",
                    "priority": "NORMAL",
                    "recipients": ["student@example.com"],
                    "source": "BACKEND",
                },
            )

            response = self.client.post(
                "/plugin/v1/agent/chat",
                json={
                    "sessionId": "s1",
                    "userId": 1,
                    "scope": "CURRENT_MAIL",
                    "message": "Summarize this email",
                    "context": {"mailItemId": 88},
                },
            )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "SUCCEEDED")
        self.assertEqual(body["toolCalls"][0]["tool"], "mail_context_tool")
        self.assertEqual(body["toolCalls"][0]["source"], "BACKEND")
        self.assertEqual(body["pendingActions"], [])

    def test_agent_chat_global_uses_mock_rag(self) -> None:
        response = self.client.post(
            "/plugin/v1/agent/chat",
            json={
                "sessionId": "s1",
                "userId": 1,
                "scope": "GLOBAL",
                "message": "Find recent project updates",
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "SUCCEEDED")
        self.assertIn("mock retrieval", body["answer"])
        self.assertEqual(body["toolCalls"][0]["tool"], "rag_tool")
        self.assertEqual(body["toolCalls"][0]["source"], "MOCK")

    def test_agent_chat_write_intent_returns_pending_action_contract(self) -> None:
        response = self.client.post(
            "/plugin/v1/agent/chat",
            json={
                "sessionId": "s1",
                "userId": 1,
                "scope": "CURRENT_MAIL",
                "message": "Set priority to high",
                "context": {"mailId": 88},
                "toolPolicy": {"agentAutoWriteEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "SUCCEEDED")
        action = body["pendingActions"][0]
        self.assertEqual(action["actionId"], "s1:88:SET_PRIORITY")
        self.assertEqual(action["label"], "Set priority: HIGH")
        self.assertEqual(action["payload"]["mailItemId"], 88)
        self.assertNotIn("mailId", action["payload"])
        self.assertEqual(action["status"], "PENDING")


if __name__ == "__main__":
    unittest.main()
