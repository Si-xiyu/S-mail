import unittest
from unittest.mock import patch

from fastapi.testclient import TestClient

from app.main import app
from app.schemas.agent import ToolResult
from fixtures import EXPECTED_ANALYSIS_RESPONSES, analysis_request


RISK_LEVELS = {"LOW", "MEDIUM", "HIGH"}
PRIORITIES = {"LOW", "NORMAL", "HIGH", "URGENT"}


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
            json=analysis_request("disabled_ai_plugin"),
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "DISABLED")
        self.assertEqual(body["summary"], [])
        self.assertIsNone(body["category"])
        self.assertIsNone(body["priority"])
        self.assertEqual(body["modelInfo"]["provider"], "RULES")
        self.assertEqual(body["modelInfo"]["mode"], "rules-fallback")
        self.assertEqual(body["modelInfo"]["fallbackUsed"], True)

    def test_category_object_input_preserves_id_and_name(self) -> None:
        response = self.client.post(
            "/plugin/v1/analysis/mail",
            json=analysis_request("category_id_name_preservation"),
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
            json=analysis_request("junk_phishing_like_mail"),
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
            json=analysis_request("deepseek_configured_rules_fallback"),
        )

        self.assertEqual(response.status_code, 200)
        model_info = response.json()["modelInfo"]
        self.assertEqual(model_info["provider"], "DEEPSEEK")
        self.assertEqual(model_info["mode"], "rules-fallback")
        self.assertIs(model_info["llmEnabled"], True)
        self.assertIs(model_info["fallbackUsed"], True)

    def test_analysis_fixture_contracts_are_stable(self) -> None:
        for name, expected in EXPECTED_ANALYSIS_RESPONSES.items():
            with self.subTest(fixture=name):
                response = self.client.post("/plugin/v1/analysis/mail", json=analysis_request(name))

                self.assertEqual(response.status_code, 200)
                body = response.json()
                for field, expected_value in expected.items():
                    if field == "modelInfo":
                        for model_field, model_expected_value in expected_value.items():
                            self.assertEqual(body["modelInfo"][model_field], model_expected_value)
                    else:
                        self.assertEqual(body[field], expected_value)

    def test_analysis_model_info_provider_rules_when_llm_disabled(self) -> None:
        response = self.client.post(
            "/plugin/v1/analysis/mail",
            json=analysis_request("normal_incoming_mail"),
        )

        self.assertEqual(response.status_code, 200)
        model_info = response.json()["modelInfo"]
        self.assertEqual(model_info["provider"], "RULES")
        self.assertEqual(model_info["mode"], "rules-fallback")
        self.assertIs(model_info["llmEnabled"], False)
        self.assertIs(model_info["fallbackUsed"], True)

    def test_analysis_risk_level_and_priority_enums(self) -> None:
        for name in EXPECTED_ANALYSIS_RESPONSES:
            with self.subTest(fixture=name):
                response = self.client.post("/plugin/v1/analysis/mail", json=analysis_request(name))

                self.assertEqual(response.status_code, 200)
                body = response.json()
                self.assertIn(body["riskLevel"], RISK_LEVELS)
                if body["priority"] is not None:
                    self.assertIn(body["priority"], PRIORITIES)

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

    def test_execute_action_disabled_plugin_returns_noop(self) -> None:
        response = self.client.post(
            "/plugin/v1/agent/actions/execute",
            json={
                "actionId": "s1:88:SET_PRIORITY",
                "userId": 1,
                "confirmed": True,
                "type": "SET_PRIORITY",
                "payload": {"mailItemId": 88, "priority": "HIGH"},
                "pluginConfig": {"aiPluginEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "DISABLED")
        self.assertEqual(body["execution"], "NONE")
        self.assertIsNone(body["backendOperation"])

    def test_execute_action_without_confirmation_rejects_when_auto_write_disabled(self) -> None:
        response = self.client.post(
            "/plugin/v1/agent/actions/execute",
            json={
                "actionId": "s1:88:SET_PRIORITY",
                "userId": 1,
                "confirmed": False,
                "type": "SET_PRIORITY",
                "payload": {"mailItemId": 88, "priority": "HIGH"},
                "toolPolicy": {"agentAutoWriteEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "REJECTED")
        self.assertEqual(body["execution"], "NONE")
        self.assertIsNone(body["backendOperation"])

    def test_execute_action_confirmed_delegates_backend_operation(self) -> None:
        response = self.client.post(
            "/plugin/v1/agent/actions/execute",
            json={
                "actionId": "s1:88:SET_PRIORITY",
                "userId": 1,
                "confirmed": True,
                "type": "SET_PRIORITY",
                "payload": {"mailItemId": 88, "priority": "HIGH"},
                "toolPolicy": {"agentAutoWriteEnabled": False},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "DELEGATED")
        self.assertEqual(body["actionId"], "s1:88:SET_PRIORITY")
        self.assertEqual(body["execution"], "BACKEND_REQUIRED")
        self.assertEqual(
            body["backendOperation"],
            {
                "method": "POST",
                "path": "/internal/v1/tools/mail-actions/execute",
                "payload": {"mailItemId": 88, "priority": "HIGH"},
            },
        )

    def test_execute_action_auto_write_delegates_without_confirmation(self) -> None:
        response = self.client.post(
            "/plugin/v1/agent/actions/execute",
            json={
                "actionId": "s1:88:MARK_READ",
                "userId": 1,
                "confirmed": False,
                "type": "MARK_READ",
                "payload": {"mailItemId": 88, "read": True},
                "toolPolicy": {"agentAutoWriteEnabled": True},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "DELEGATED")
        self.assertEqual(body["execution"], "BACKEND_REQUIRED")
        self.assertEqual(body["backendOperation"]["payload"], {"mailItemId": 88, "read": True})

    def test_execute_action_legacy_mail_id_is_normalized(self) -> None:
        response = self.client.post(
            "/plugin/v1/agent/actions/execute",
            json={
                "actionId": "s1:88:MOVE_TO_JUNK",
                "userId": 1,
                "confirmed": True,
                "type": "MOVE_TO_JUNK",
                "payload": {"mailId": 88},
            },
        )

        self.assertEqual(response.status_code, 200)
        payload = response.json()["backendOperation"]["payload"]
        self.assertEqual(payload["mailItemId"], 88)
        self.assertNotIn("mailId", payload)

    def test_execute_action_unsupported_type_is_rejected(self) -> None:
        response = self.client.post(
            "/plugin/v1/agent/actions/execute",
            json={
                "actionId": "s1:88:DELETE",
                "userId": 1,
                "confirmed": True,
                "type": "DELETE",
                "payload": {"mailItemId": 88},
            },
        )

        self.assertEqual(response.status_code, 200)
        body = response.json()
        self.assertEqual(body["status"], "REJECTED")
        self.assertEqual(body["execution"], "NONE")
        self.assertIsNone(body["backendOperation"])


if __name__ == "__main__":
    unittest.main()
