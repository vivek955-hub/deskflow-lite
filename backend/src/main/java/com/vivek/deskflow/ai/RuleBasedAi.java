package com.vivek.deskflow.ai;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.vivek.deskflow.ai.AiService.AiText;
import com.vivek.deskflow.ai.AiService.TriageResult;
import com.vivek.deskflow.ticket.Category;
import com.vivek.deskflow.ticket.Priority;

/**
 * The offline brain: keyword scoring and templates. No API key, no network, no cost.
 * It is the default, and also the safety net whenever the LLM is slow, down, or returns nonsense.
 */
@Component
public class RuleBasedAi {
    private static final String SOURCE = "RULES";

    private static final Map<Category, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();
    static {
        CATEGORY_KEYWORDS.put(Category.BILLING, List.of("invoice", "billing", "bill", "charged", "charge", "refund",
                "payment", "paid", "subscription", "pricing", "price", "receipt", "card", "renewal", "gst"));
        CATEGORY_KEYWORDS.put(Category.ACCOUNT, List.of("password", "login", "log in", "sign in", "signin", "account",
                "locked", "otp", "verification", "email change", "profile", "two-factor", "2fa", "reset"));
        CATEGORY_KEYWORDS.put(Category.TECHNICAL, List.of("error", "bug", "crash", "crashes", "not working", "broken",
                "slow", "timeout", "500", "404", "fails", "failed", "api", "outage", "down", "loading", "upload",
                "export", "sync"));
        CATEGORY_KEYWORDS.put(Category.FEATURE_REQUEST, List.of("feature", "suggestion", "would be nice",
                "please add", "can you add", "wish", "improvement", "enhancement", "support for", "dark mode"));
    }

    private static final List<String> URGENT_WORDS = List.of("urgent", "asap", "immediately", "outage",
            "production down", "site is down", "system is down", "data loss", "lost all", "security", "breach",
            "hacked", "emergency", "critical");
    private static final List<String> HIGH_WORDS = List.of("cannot", "can't", "cant", "unable", "blocked",
            "not working", "crash", "crashes", "failed", "fails", "charged twice", "double charged", "locked out",
            "error", "deadline");
    private static final List<String> LOW_WORDS = List.of("feature", "suggestion", "would be nice", "feedback",
            "question", "how do i", "how to", "curious", "wondering", "no rush", "whenever");

    public TriageResult triage(String subject, String description) {
        String subjectText = normalise(subject);
        String fullText = subjectText + " " + normalise(description);
        List<String> reasons = new ArrayList<>();

        // Category = the group with the most keyword hits. A hit in the subject counts double.
        Category category = Category.GENERAL;
        int bestScore = 0;
        String bestWord = null;
        for (Map.Entry<Category, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
            int score = 0;
            String firstHit = null;
            for (String word : entry.getValue()) {
                if (containsWord(fullText, word)) {
                    score += containsWord(subjectText, word) ? 2 : 1;
                    if (firstHit == null) firstHit = word;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                category = entry.getKey();
                bestWord = firstHit;
            }
        }
        if (bestWord != null) {
            reasons.add("category from \"" + bestWord + "\"");
        }

        // Priority = the most serious signal wins.
        Priority priority = Priority.MEDIUM;
        String urgent = firstMatch(fullText, URGENT_WORDS);
        String high = firstMatch(fullText, HIGH_WORDS);
        String low = firstMatch(fullText, LOW_WORDS);
        if (urgent != null) {
            priority = Priority.URGENT;
            reasons.add("priority from \"" + urgent + "\"");
        } else if (high != null) {
            priority = Priority.HIGH;
            reasons.add("priority from \"" + high + "\"");
        } else if (low != null) {
            priority = Priority.LOW;
            reasons.add("priority from \"" + low + "\"");
        }

        String reason = reasons.isEmpty() ? "No strong keywords found, so defaults were used"
                : "Keyword match: " + String.join(", ", reasons);
        return new TriageResult(category, priority, reason, SOURCE);
    }

    public AiText suggestReply(String customerName, String subject, Category category) {
        String firstName = customerName == null || customerName.isBlank() ? "there" : customerName.trim().split("\\s+")[0];
        String middle = switch (category) {
            case BILLING -> "I'm sorry about the trouble with your billing. I'm checking the payment records for "
                    + "your account now. Could you share the invoice number or the date of the charge so I can "
                    + "trace it quickly? If a refund is due, we will process it right away.";
            case ACCOUNT -> "I understand how frustrating it is to be stuck outside your account. Please try the "
                    + "\"Forgot password\" link on the login page first. If the reset email does not arrive within "
                    + "five minutes, reply here and I will reset access from our side.";
            case TECHNICAL -> "Sorry you ran into this. To get it fixed quickly, could you tell me the exact steps "
                    + "that lead to the problem, the error message you see, and which browser or device you are "
                    + "using? A screenshot would help a lot. I've started looking into it on our side.";
            case FEATURE_REQUEST -> "Thank you for the suggestion, this is useful feedback. I've passed it to our "
                    + "product team for review. I can't promise a timeline yet, but I'll update this ticket as soon "
                    + "as there is a decision.";
            case GENERAL -> "Thanks for reaching out. I'm looking into this and will get back to you shortly. "
                    + "If you have any extra details that might help, please reply to this ticket.";
        };
        String text = "Hi " + firstName + ",\n\nThanks for contacting us about \"" + subject + "\". " + middle
                + "\n\nBest regards,\nSupport Team";
        return new AiText(text, SOURCE);
    }

    public AiText summarize(String subject, String description, List<String> conversation) {
        StringBuilder sb = new StringBuilder();
        sb.append("Issue: ").append(subject).append(".\n");
        sb.append("Customer said: ").append(firstSentences(description, 2)).append("\n");
        if (conversation.isEmpty()) {
            sb.append("No replies yet.");
        } else {
            sb.append(conversation.size()).append(conversation.size() == 1 ? " reply" : " replies")
              .append(" so far. Latest: ").append(shorten(conversation.get(conversation.size() - 1), 220));
        }
        return new AiText(sb.toString(), SOURCE);
    }

    // ---------- helpers ----------

    private static String normalise(String text) {
        return text == null ? "" : " " + text.toLowerCase().replaceAll("\\s+", " ") + " ";
    }

    /** True if the keyword appears as a whole word or phrase, so "bill" does not match "billboard". */
    static boolean containsWord(String text, String keyword) {
        int from = 0;
        while (true) {
            int at = text.indexOf(keyword, from);
            if (at < 0) return false;
            boolean startOk = at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
            int end = at + keyword.length();
            boolean endOk = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end));
            if (startOk && endOk) return true;
            from = at + 1;
        }
    }

    private static String firstMatch(String text, List<String> words) {
        for (String word : words) {
            if (containsWord(text, word)) return word;
        }
        return null;
    }

    private static String firstSentences(String text, int count) {
        String clean = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        String[] parts = clean.split("(?<=[.!?])\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length && i < count; i++) {
            if (i > 0) sb.append(' ');
            sb.append(parts[i]);
        }
        return shorten(sb.toString(), 320);
    }

    private static String shorten(String text, int max) {
        String clean = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        return clean.length() <= max ? clean : clean.substring(0, max - 1) + "…";
    }
}
