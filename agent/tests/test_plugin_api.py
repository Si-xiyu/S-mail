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


if __name__ == "__main__":
    unittest.main()
