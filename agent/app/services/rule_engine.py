from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any

from app.schemas.agent import AnalysisCategory, AnalysisResponse, MailContext, ModelInfo


@dataclass(frozen=True)
class AnalysisResult:
    summary: list[str]
    category: AnalysisCategory | str
    junk: bool
    priority: str
    priority_score: int
    risk_level: str
    risk_hints: list[str]


class RuleEngine:
    urgent_words = (
        "urgent",
        "asap",
        "immediately",
        "deadline",
        "due today",
        "due tomorrow",
        "action required",
        "last chance",
        "final notice",
        "important",
    )
    spam_words = (
        "winner",
        "prize",
        "lottery",
        "free money",
        "cash bonus",
        "guaranteed",
        "limited offer",
        "act now",
        "click here",
        "unsubscribe",
        "promotion",
        "promotional",
        "discount",
        "coupon",
        "casino",
        "loan",
        "invoice attached",
    )
    phishing_phrases = (
        "verify your account",
        "verification code",
        "reset your password",
        "password expires",
        "confirm your identity",
        "bank transfer",
        "wire transfer",
        "urgent click",
        "login immediately",
        "account suspended",
        "security alert",
    )
    meeting_words = ("meeting", "invite", "agenda", "minutes", "schedule", "call", "review")
    work_words = ("project", "task", "deliverable", "report", "approval", "contract", "invoice")
    url_pattern = re.compile(r"https?://|www\.", re.IGNORECASE)
    sender_pattern = re.compile(r"^[^@\s]+@([^@\s]+)$")

    def summarize(self, mail: MailContext) -> dict[str, Any]:
        sentences = self._split_sentences(mail.content_text)
        bullets = sentences[:3] or [mail.subject]
        return {
            "subject": mail.subject,
            "summary": bullets,
            "actionItems": self._extract_action_items(mail.content_text),
        }

    def draft_reply(self, mail: MailContext) -> dict[str, str]:
        sender_name = mail.sender_email.split("@")[0] if mail.sender_email else "there"
        return {
            "draft": (
                f"Hi {sender_name},\n\n"
                f"I received your email about \"{mail.subject}\" and will review the details.\n\n"
                "Thanks."
            )
        }

    def analyze(self, mail: MailContext) -> dict[str, Any]:
        text = f"{mail.subject}\n{mail.content_text}"
        result = self.analyze_mail(
            subject=mail.subject,
            content=mail.content_text,
            sender=mail.sender_email,
            user_categories=[],
            behavior_signals={},
        )
        return {
            "priority": result.priority,
            "labels": [self._category_name(result.category)],
            "spam": result.junk,
            "riskHints": result.risk_hints,
            "summary": result.summary,
            "priorityScore": result.priority_score,
            "riskLevel": result.risk_level,
            "textLength": len(text),
        }

    def analyze_mail(
        self,
        *,
        subject: str,
        content: str,
        sender: str,
        user_categories: list[Any],
        behavior_signals: dict[str, Any],
    ) -> AnalysisResult:
        text = f"{subject}\n{content}".strip()
        lower_text = text.lower()
        categories = self._category_options(user_categories)
        risk_hints = self._risk_hints(lower_text, sender)
        junk = self._is_junk(lower_text, behavior_signals)
        category = self._choose_category(lower_text, categories, junk)
        score = self._priority_score(lower_text, behavior_signals, junk)
        return AnalysisResult(
            summary=self._summary(subject, content),
            category=category,
            junk=junk,
            priority=self._priority_from_score(score),
            priority_score=score,
            risk_level=self._risk_level(risk_hints, junk),
            risk_hints=risk_hints,
        )

    def analysis_response(
        self,
        *,
        subject: str,
        content: str,
        sender: str,
        user_categories: list[Any],
        behavior_signals: dict[str, Any],
        model_info: ModelInfo,
    ) -> AnalysisResponse:
        result = self.analyze_mail(
            subject=subject,
            content=content,
            sender=sender,
            user_categories=user_categories,
            behavior_signals=behavior_signals,
        )
        return AnalysisResponse(
            status="SUCCEEDED",
            summary=result.summary,
            category=result.category,
            junk=result.junk,
            priority=result.priority,
            priorityScore=result.priority_score,
            riskLevel=result.risk_level,
            riskHints=result.risk_hints,
            modelInfo=model_info,
        )

    def _summary(self, subject: str, content: str) -> list[str]:
        candidates = self._split_sentences(content)
        if subject.strip():
            candidates.insert(0, subject.strip())
        bullets: list[str] = []
        for item in candidates:
            clean = self._shorten(item)
            if clean and clean not in bullets:
                bullets.append(clean)
            if len(bullets) == 3:
                break
        return bullets or ["No substantive mail content provided."]

    def _split_sentences(self, text: str) -> list[str]:
        cleaned = re.sub(r"\s+", " ", text or "").strip()
        if not cleaned:
            return []
        parts = re.split(r"(?<=[.!?])\s+|[;\n]+", cleaned)
        return [part.strip(" -\t") for part in parts if part.strip(" -\t")]

    def _shorten(self, text: str, limit: int = 120) -> str:
        clean = re.sub(r"\s+", " ", text).strip()
        if len(clean) <= limit:
            return clean
        return clean[: limit - 3].rstrip() + "..."

    def _extract_action_items(self, text: str) -> list[str]:
        action_words = ("please", "need", "required", "submit", "prepare", "complete", "reply", "review")
        return [
            sentence
            for sentence in self._split_sentences(text)
            if any(word in sentence.lower() for word in action_words)
        ][:5]

    def _category_options(self, user_categories: list[Any]) -> list[AnalysisCategory]:
        categories: list[AnalysisCategory] = []
        for category in user_categories:
            if isinstance(category, str):
                name = category.strip()
                category_id = None
            elif isinstance(category, dict):
                raw = category.get("name") or category.get("label") or category.get("category")
                name = str(raw).strip() if raw is not None else ""
                category_id = category.get("id")
            else:
                name = str(category).strip()
                category_id = None
            if name:
                categories.append(AnalysisCategory(id=category_id, name=name))
        return categories

    def _choose_category(self, text: str, categories: list[AnalysisCategory], junk: bool) -> AnalysisCategory:
        if junk:
            junk_category = self._find_category(categories, ("junk",)) or self._find_category(categories, ("spam",))
            if junk_category:
                return junk_category
        for category in categories:
            words = [part for part in re.split(r"[\s_\-/]+", category.name.lower()) if len(part) >= 3]
            if words and any(word in text for word in words):
                return category
        if self._find_terms(text, self.meeting_words):
            return self._find_category(categories, ("meeting", "calendar", "work")) or self._default_category(categories)
        if self._find_terms(text, self.work_words):
            return self._find_category(categories, ("work", "project", "business")) or self._default_category(categories)
        return self._default_category(categories)

    def _is_junk(self, text: str, behavior_signals: dict[str, Any]) -> bool:
        spam_hits = sum(1 for word in self.spam_words if word in text)
        phishing_hits = sum(1 for phrase in self.phishing_phrases if phrase in text)
        repeated_promo = len(re.findall(r"\b(free|sale|offer|discount|deal|promo)\b", text)) >= 3
        recent_junk_sender = self._truthy_signal(behavior_signals, "recentJunkSender", "recent_junk_sender")
        return recent_junk_sender or spam_hits >= 2 or phishing_hits >= 2 or (spam_hits >= 1 and repeated_promo)

    def _priority_score(self, text: str, behavior_signals: dict[str, Any], junk: bool) -> int:
        score = 50
        score += 18 if self._find_terms(text, self.urgent_words) else 0
        score += 10 if self._find_terms(text, self.meeting_words) else 0
        score += 8 if self._find_terms(text, self.work_words) else 0
        score += 12 if self._truthy_signal(behavior_signals, "frequentSender", "frequent_sender") else 0
        score += 10 if self._truthy_signal(behavior_signals, "repliedSender", "replied_sender") else 0
        score += 8 if self._truthy_signal(behavior_signals, "directToUser", "direct_to_user") else 0
        score -= 35 if self._truthy_signal(behavior_signals, "recentJunkSender", "recent_junk_sender") else 0
        score -= 30 if junk else 0
        return max(0, min(100, score))

    def _priority_from_score(self, score: int) -> str:
        if score >= 90:
            return "URGENT"
        if score >= 70:
            return "HIGH"
        if score >= 40:
            return "NORMAL"
        return "LOW"

    def _risk_hints(self, text: str, sender: str = "") -> list[str]:
        hints: list[str] = []
        if self.url_pattern.search(text):
            hints.append("URL present in message body.")
        sender_hint = self._sender_risk_hint(sender)
        if sender_hint:
            hints.append(sender_hint)
        risk_terms = {
            "prize": "Prize or lottery wording detected.",
            "loan": "Loan or financing wording detected.",
            "transfer": "Transfer or payment wording detected.",
            "password": "Password-related wording detected.",
            "verification code": "Verification-code wording detected.",
            "urgent click": "Urgent click wording detected.",
        }
        for term, hint in risk_terms.items():
            if term in text and hint not in hints:
                hints.append(hint)
        if self._find_terms(text, self.phishing_phrases):
            hints.append("Phishing-like account or identity phrase detected.")
        if self._find_terms(text, self.spam_words):
            hints.append("Spam or promotion wording detected.")
        return hints

    def _sender_risk_hint(self, sender: str) -> str | None:
        if not sender:
            return None
        lowered = sender.lower()
        match = self.sender_pattern.match(lowered)
        domain = match.group(1) if match else lowered
        suspicious_parts = ("secure", "verify", "notice", "support", "account", "billing")
        suspicious_tlds = (".zip", ".top", ".xyz", ".click", ".loan")
        if any(part in domain for part in suspicious_parts) or domain.endswith(suspicious_tlds):
            return "Suspicious sender or domain wording detected."
        return None

    def _risk_level(self, hints: list[str], junk: bool) -> str:
        if junk or len(hints) >= 3:
            return "HIGH"
        if hints:
            return "MEDIUM"
        return "LOW"

    def _find_terms(self, text: str, terms: tuple[str, ...]) -> bool:
        return any(term in text for term in terms)

    def _find_category(
        self, categories: list[AnalysisCategory], terms: tuple[str, ...]
    ) -> AnalysisCategory | None:
        for category in categories:
            lower_category = category.name.lower()
            if any(term in lower_category for term in terms):
                return category
        return None

    def _default_category(self, categories: list[AnalysisCategory]) -> AnalysisCategory:
        return self._find_category(categories, ("other",)) or AnalysisCategory(name="Other")

    def _category_name(self, category: AnalysisCategory | str) -> str:
        if isinstance(category, AnalysisCategory):
            return category.name
        return category

    def _truthy_signal(self, signals: dict[str, Any], *names: str) -> bool:
        return any(bool(signals.get(name)) for name in names)
