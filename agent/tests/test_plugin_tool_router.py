from __future__ import annotations

import unittest
from unittest.mock import Mock, patch

from app.schemas.agent import ToolResult
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
        self.assertIn("关闭", response.answer)
        self.assertEqual(response.tool_calls, [])
        self.assertEqual(response.pending_actions, [])

    def test_global_no_backend_results_returns_plain_no_match_message(self) -> None:
        with patch.object(self.router.backend_tools, "search_mail", return_value=ToolResult(ok=True, data=[])):
            response = self.router.chat(
                PluginChatRequest(
                    sessionId="s1",
                    userId=1,
                    scope="GLOBAL",
                    message="最近有哪些项目进度邮件？",
                )
            )

        self.assertEqual(response.status, "SUCCEEDED")
        self.assertNotIn("mock", response.answer.lower())
        self.assertIn("没有找到", response.answer)
        self.assertEqual(response.tool_calls[0].tool, "mail_search_tool")
        self.assertEqual(response.tool_calls[0].source, "BACKEND")

    def test_global_identity_question_returns_assistant_intro_without_search(self) -> None:
        with patch.object(self.router.backend_tools, "search_mail") as search_mail:
            response = self.router.chat(
                PluginChatRequest(
                    sessionId="s1",
                    userId=1,
                    scope="GLOBAL",
                    message="你是谁？",
                )
            )

        self.assertEqual(response.status, "SUCCEEDED")
        self.assertIn("SmartMail 邮箱助手", response.answer)
        self.assertEqual(response.tool_calls, [])
        search_mail.assert_not_called()

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
        self.assertEqual(response.pending_actions[0].label, "标记为已读")
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

    def test_write_request_without_mail_item_context_is_rejected_before_backend_delegation(self) -> None:
        response = self.router.chat(
            PluginChatRequest(
                sessionId="s1",
                userId=1,
                scope="CURRENT_MAIL",
                message="Set priority to high",
                context={},
                toolPolicy={"agentAutoWriteEnabled": False},
            )
        )

        self.assertEqual(response.status, "FAILED")
        self.assertEqual(response.pending_actions, [])

    def test_set_category_action_payload_uses_backend_category_id_contract(self) -> None:
        response = self.router.chat(
            PluginChatRequest(
                sessionId="s1",
                userId=1,
                scope="CURRENT_MAIL",
                message="Set category",
                context={"mailItemId": 7, "categoryId": 12},
                toolPolicy={"agentAutoWriteEnabled": False},
            )
        )

        self.assertEqual(response.status, "SUCCEEDED")
        self.assertEqual(response.pending_actions[0].type, "SET_CATEGORY")
        self.assertEqual(response.pending_actions[0].payload["categoryId"], 12)
        self.assertNotIn("category", response.pending_actions[0].payload)


    def test_global_delete_ad_mail_creates_trash_pending_actions(self) -> None:
        with patch.object(self.router.backend_tools, "search_mail", return_value=ToolResult(ok=True, data=[
            {
                "mailItemId": 88,
                "subject": "Big discount ad",
                "senderEmail": "ads@example.com",
                "snippet": "limited promotion",
            }
        ])) as search_mail:
            response = self.router.chat(
                PluginChatRequest(
                    sessionId="s1",
                    userId=1,
                    scope="GLOBAL",
                    message="帮我把广告邮件删掉",
                    toolPolicy={"agentAutoWriteEnabled": False},
                )
            )

        self.assertEqual(response.status, "SUCCEEDED")
        search_mail.assert_called_once()
        self.assertEqual(response.pending_actions[0].type, "MOVE")
        self.assertEqual(response.pending_actions[0].payload["mailItemId"], 88)
        self.assertEqual(response.pending_actions[0].payload["folder"], "TRASH")
        self.assertEqual(response.pending_actions[0].payload["action"], "MOVE")
        self.assertIn("确认", response.answer)
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
        self.assertTrue(get.call_args_list[0].args[0].endswith("/internal/v1/tools/mail-items/88/context"))
        self.assertEqual(get.call_args_list[0].kwargs["params"], {"userId": 1})
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


    def test_search_mail_calls_backend_search_endpoint(self) -> None:
        response = Mock()
        response.raise_for_status.return_value = None
        response.json.return_value = {
            "code": 0,
            "data": [
                {
                    "mailItemId": 88,
                    "subject": "Project update",
                    "snippet": "Please send the project update.",
                    "source": "BACKEND",
                    "score": 1.0,
                }
            ],
        }

        with patch("app.tools.backend_tools.httpx.get", return_value=response) as get:
            result = self.client.search_mail(1, "project", folder="inbox", limit=5)

        self.assertTrue(result.ok)
        self.assertEqual(result.data, response.json.return_value["data"])
        self.assertTrue(get.call_args.args[0].endswith("/internal/v1/tools/mail-search"))
        self.assertEqual(get.call_args.kwargs["params"], {"userId": 1, "keyword": "project", "limit": 5, "folder": "INBOX"})

    def test_backend_timeout_is_classified(self) -> None:
        with patch("app.tools.backend_tools.httpx.get", side_effect=Exception("timed out")):
            result = self.client.get_mail(1, 1)

        self.assertFalse(result.ok)
        self.assertIn("BACKEND_ERROR", result.error)


if __name__ == "__main__":
    unittest.main()
