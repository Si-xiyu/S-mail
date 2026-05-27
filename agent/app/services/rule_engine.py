from __future__ import annotations

import re

from app.schemas.agent import MailContext


class RuleEngine:
    urgent_words = ("紧急", "重要", "截止", "马上", "立即", "今天", "明天", "deadline")
    spam_words = ("中奖", "贷款", "免费提现", "点击链接", "博彩", "发票代开", "高收益")
    meeting_words = ("会议", "演示", "汇报", "评审", "答辩")
    coursework_words = ("作业", "实训", "课程", "提交", "老师", "项目")

    def summarize(self, mail: MailContext) -> dict:
        sentences = self._split_sentences(mail.content_text)
        bullets = sentences[:3] or [mail.subject]
        return {
            "subject": mail.subject,
            "summary": bullets,
            "actionItems": self._extract_action_items(mail.content_text),
        }

    def draft_reply(self, mail: MailContext) -> dict:
        sender_name = mail.sender_email.split("@")[0]
        return {
            "draft": (
                f"{sender_name}，您好：\n\n"
                f"邮件已收到。我们会根据「{mail.subject}」中的要求整理进度并按时反馈。\n\n"
                "如有补充材料或格式要求，也请继续告知。\n\n"
                "谢谢。"
            )
        }

    def analyze(self, mail: MailContext) -> dict:
        text = f"{mail.subject}\n{mail.content_text}".lower()
        spam = any(word in text for word in self.spam_words)
        priority = "HIGH" if any(word.lower() in text for word in self.urgent_words) else "NORMAL"
        if spam:
            priority = "LOW"
        labels = self._labels(text)
        return {
            "priority": priority,
            "labels": labels,
            "spam": spam,
            "riskHints": self._risk_hints(text),
        }

    def _split_sentences(self, text: str) -> list[str]:
        cleaned = re.sub(r"\s+", " ", text).strip()
        if not cleaned:
            return []
        parts = re.split(r"[。！？!?；;]\s*", cleaned)
        return [part.strip() for part in parts if part.strip()]

    def _extract_action_items(self, text: str) -> list[str]:
        items = []
        for sentence in self._split_sentences(text):
            if any(word in sentence for word in ("请", "需要", "提交", "准备", "完成", "回复")):
                items.append(sentence)
        return items[:5]

    def _labels(self, text: str) -> list[str]:
        labels = []
        if any(word in text for word in self.meeting_words):
            labels.append("会议")
        if any(word in text for word in self.coursework_words):
            labels.append("课程")
        if "附件" in text:
            labels.append("附件")
        if any(word.lower() in text for word in self.urgent_words):
            labels.append("待处理")
        return labels or ["普通"]

    def _risk_hints(self, text: str) -> list[str]:
        hints = []
        if "http://" in text or "https://" in text:
            hints.append("正文包含链接，请确认来源可信")
        if any(word in text for word in self.spam_words):
            hints.append("命中疑似垃圾邮件关键词")
        return hints
