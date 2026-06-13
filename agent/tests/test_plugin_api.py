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
        self.assertEqual(body["modelInfo"]["provider"], "RULES")
        self.assertEqual(body["modelInfo"]["mode"], "rules-fallback")

    def test_category_object_input_preserves_id_and_name(self) -> None:
        response = self.client.post(
            "/plugin/v1/analysis/mail",
            json={
                "taskId": "task-category-object",
                "userId": "user-1",
                "mailItemId": "mail-category-object",
                "mail": {
                    "subject": "Project report approval",
                    "content": "Please review the project report before approval.",
                },
                "userCategories": [
                    {"id": 7, "name": "Project"},
                    {"id": 8, "name": "Other"},
                ],
                "behaviorSignals": {},
                "pluginConfig": {"aiPluginEnabled": True, "llmEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["category"], {"id": 7, "name": "Project"})

    def test_string_category_input_returns_category_object(self) -> None:
        response = self.client.post(
            "/plugin/v1/analysis/mail",
            json={
                "taskId": "task-category-string",
                "userId": "user-1",
                "mailItemId": "mail-category-string",
                "mail": {
                    "subject": "Work meeting",
                    "content": "The meeting agenda is ready for review.",
                },
                "userCategories": ["Work", "Other"],
                "behaviorSignals": {},
                "pluginConfig": {"aiPluginEnabled": True, "llmEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        self.assertEqual(response.json()["category"], {"id": None, "name": "Work"})

    def test_rules_fallback_detects_junk_risk_priority_and_category_object(self) -> None:
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
                "userCategories": [
                    {"id": 1, "name": "Work"},
                    {"id": 2, "name": "Junk Mail"},
                    {"id": 3, "name": "Finance"},
                ],
                "behaviorSignals": {"recentJunkSender": True, "frequentSender": True},
                "pluginConfig": {"aiPluginEnabled": True, "llmEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "SUCCEEDED")
        self.assertEqual(body["category"], {"id": 2, "name": "Junk Mail"})
        self.assertIs(body["junk"], True)
        self.assertEqual(body["priority"], "LOW")
        self.assertGreaterEqual(body["priorityScore"], 0)
        self.assertLessEqual(body["priorityScore"], 39)
        self.assertIn(body["riskLevel"], {"LOW", "MEDIUM", "HIGH"})
        self.assertNotEqual(body["riskLevel"], "CRITICAL")
        self.assertLessEqual(len(body["summary"]), 3)
        self.assertEqual(body["modelInfo"]["provider"], "RULES")
        self.assertEqual(body["modelInfo"]["mode"], "rules-fallback")
        self.assertIs(body["modelInfo"]["fallbackUsed"], True)

    def test_model_info_reports_deepseek_boundary_with_rules_fallback(self) -> None:
        response = self.client.post(
            "/plugin/v1/analysis/mail",
            json={
                "taskId": "task-model-info",
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
        )

        self.assertEqual(response.status_code, 200)
        model_info = response.json()["modelInfo"]
        self.assertEqual(model_info["provider"], "DEEPSEEK")
        self.assertEqual(model_info["mode"], "rules-fallback")
        self.assertIs(model_info["llmEnabled"], True)
        self.assertIs(model_info["fallbackUsed"], True)

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
