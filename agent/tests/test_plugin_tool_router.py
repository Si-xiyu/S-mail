from __future__ import annotations

import unittest
from unittest.mock import Mock, patch

from app.schemas.plugin import PluginChatRequest
from app.services.tool_router import ToolRouter
from app.tools.backend_tools import BackendToolClient, to_mail_context


class ToolRouterTest(unittest.TestCase):
    def setUp(self) -> None:
        self.router = ToolRouter()

    def test_disabled_plugin_returns_disabled(self) -> None:
        response = self.router.chat(
            PluginChatRequest(
                sessionId="s1",
                userId=1,
                scope="CURRENT_MAIL",
                message="这封邮件需要我做什么？",
                pluginConfig={"aiPluginEnabled": False},
            )
        )

        self.assertEqual(response.status, "DISABLED")
        self.assertIn("disabled", response.answer.lower())
        self.assertEqual(response.tool_calls, [])
        self.assertEqual(response.pending_actions, [])

    def test_global_uses_mock_rag_tool(self) -> None:
        response = self.router.chat(
            PluginChatRequest(
                sessionId="s1",
                userId=1,
                scope="GLOBAL",
                message="最近有哪些项目进度邮件？",
            )
        )

        self.assertEqual(response.status, "SUCCEEDED")
        self.assertIn("mock retrieval", response.answer)
        self.assertEqual(response.tool_calls[0].tool, "rag_tool")
        self.assertEqual(response.tool_calls[0].source, "MOCK")
        self.assertEqual(response.tool_calls[0].output["records"][0]["source"], "MOCK")

    def test_current_mail_write_request_becomes_pending_action(self) -> None:
        response = self.router.chat(
            PluginChatRequest(
                sessionId="s1",
                userId=1,
                scope="CURRENT_MAIL",
                message="把这封邮件标为已读",
                context={"mailId": 42},
                toolPolicy={"agentAutoWriteEnabled": False},
            )
        )

        self.assertEqual(response.status, "SUCCEEDED")
        self.assertEqual(response.pending_actions[0].type, "MARK_READ")
        self.assertEqual(response.pending_actions[0].action_id, "s1:42:MARK_READ")
        self.assertEqual(response.pending_actions[0].label, "Mark as read")
        self.assertEqual(response.pending_actions[0].status, "PENDING")
        self.assertEqual(response.pending_actions[0].payload["mailItemId"], 42)

    def test_auto_write_keeps_pending_backend_required_action(self) -> None:
        response = self.router.chat(
            PluginChatRequest(
                sessionId="s1",
                userId=1,
                scope="CURRENT_MAIL",
                message="设置为高优先级",
                context={"mailId": 7},
                toolPolicy={"agentAutoWriteEnabled": True},
            )
        )

        self.assertEqual(response.status, "SUCCEEDED")
        self.assertEqual(response.pending_actions[0].type, "SET_PRIORITY")
        self.assertEqual(response.pending_actions[0].status, "PENDING")
        self.assertEqual(response.pending_actions[0].execution, "BACKEND_REQUIRED")
        self.assertEqual(response.pending_actions[0].payload["mailItemId"], 7)


class BackendToolClientTest(unittest.TestCase):
    def setUp(self) -> None:
        self.client = BackendToolClient()

    def test_current_mail_context_prefers_mail_item_context_path(self) -> None:
        response = Mock()
        response.raise_for_status.return_value = None
        response.json.return_value = {
            "data": {
                "mailItemId": 88,
                "userId": 1,
                "senderEmail": "teacher@example.com",
                "subject": "Project update",
                "contentText": "Please send the project update.",
            }
        }

        with patch("app.tools.backend_tools.httpx.get", return_value=response) as get:
            result = self.client.get_current_mail_context({"mailItemId": 88}, 1)

        self.assertTrue(result.ok)
        self.assertEqual(result.data["source"], "BACKEND")
        self.assertTrue(get.call_args.args[0].endswith("/internal/v1/tools/mail-items/88/context"))
        self.assertEqual(to_mail_context(result.data).mail_id, 88)

    def test_current_mail_context_falls_back_to_legacy_mail_path(self) -> None:
        legacy_response = Mock()
        legacy_response.raise_for_status.return_value = None
        legacy_response.json.return_value = {
            "data": {
                "mailId": 42,
                "userId": 1,
                "senderEmail": "teacher@example.com",
                "subject": "Legacy mail",
                "contentText": "Legacy context response.",
            }
        }

        with patch("app.tools.backend_tools.httpx.get", side_effect=[Exception("new path unavailable"), legacy_response]) as get:
            result = self.client.get_current_mail_context({"mailId": 42}, 1)

        self.assertTrue(result.ok)
        self.assertEqual(result.data["source"], "BACKEND")
        self.assertTrue(get.call_args_list[0].args[0].endswith("/internal/v1/tools/mail-items/42/context"))
        self.assertTrue(get.call_args_list[1].args[0].endswith("/internal/v1/tools/mails/42"))
        self.assertEqual(get.call_args_list[1].kwargs["params"], {"userId": 1})


if __name__ == "__main__":
    unittest.main()
