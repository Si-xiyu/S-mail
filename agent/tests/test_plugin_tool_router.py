from __future__ import annotations

import unittest

from app.schemas.plugin import PluginChatRequest
from app.services.tool_router import ToolRouter


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

        self.assertEqual(response.status, "SUCCESS")
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

        self.assertEqual(response.status, "NEEDS_ACTION")
        self.assertEqual(response.pending_actions[0].type, "MARK_READ")
        self.assertEqual(response.pending_actions[0].status, "PENDING")
        self.assertEqual(response.pending_actions[0].payload["mailId"], 42)

    def test_auto_write_prepares_backend_delegation_only(self) -> None:
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

        self.assertEqual(response.status, "NEEDS_ACTION")
        self.assertEqual(response.pending_actions[0].type, "SET_PRIORITY")
        self.assertEqual(response.pending_actions[0].status, "BACKEND_DELEGATION_PREPARED")
        self.assertEqual(response.pending_actions[0].execution, "BACKEND_DELEGATION_ONLY")


if __name__ == "__main__":
    unittest.main()
