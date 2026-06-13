import unittest

from fastapi.testclient import TestClient

from app.main import app


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


if __name__ == "__main__":
    unittest.main()
