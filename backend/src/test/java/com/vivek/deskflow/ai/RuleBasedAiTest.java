package com.vivek.deskflow.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.vivek.deskflow.ai.AiService.TriageResult;
import com.vivek.deskflow.ticket.Category;
import com.vivek.deskflow.ticket.Priority;

class RuleBasedAiTest {
    private final RuleBasedAi ai = new RuleBasedAi();

    @Test
    void billingProblemIsBillingAndHigh() {
        TriageResult result = ai.triage("Charged twice this month", "Please refund the extra payment on my card.");
        assertEquals(Category.BILLING, result.category());
        assertEquals(Priority.HIGH, result.priority());
        assertEquals("RULES", result.source());
    }

    @Test
    void outageIsTechnicalAndUrgent() {
        TriageResult result = ai.triage("Site is down", "Every page shows error 500. Fix it immediately.");
        assertEquals(Category.TECHNICAL, result.category());
        assertEquals(Priority.URGENT, result.priority());
    }

    @Test
    void lockedOutUserIsAccount() {
        TriageResult result = ai.triage("Cannot log in", "My password reset did not work and I am locked out.");
        assertEquals(Category.ACCOUNT, result.category());
        assertEquals(Priority.HIGH, result.priority());
    }

    @Test
    void featureIdeaIsLowPriority() {
        TriageResult result = ai.triage("Please add dark mode", "Just a suggestion, no rush.");
        assertEquals(Category.FEATURE_REQUEST, result.category());
        assertEquals(Priority.LOW, result.priority());
    }

    @Test
    void textWithNoKeywordsFallsBackToDefaults() {
        TriageResult result = ai.triage("Hello", "Good morning to the team.");
        assertEquals(Category.GENERAL, result.category());
        assertEquals(Priority.MEDIUM, result.priority());
    }

    @Test
    void keywordsMatchWholeWordsOnly() {
        assertTrue(RuleBasedAi.containsWord(" my bill is wrong ", "bill"));
        assertFalse(RuleBasedAi.containsWord(" the billboard fell ", "bill"));
    }

    @Test
    void replyUsesFirstNameAndSummaryCountsReplies() {
        assertTrue(ai.suggestReply("John Mathew", "Refund", Category.BILLING).text().startsWith("Hi John,"));
        String summary = ai.summarize("Refund", "I want a refund. Thanks.", List.of("Agent: on it")).text();
        assertTrue(summary.contains("1 reply"));
    }
}
