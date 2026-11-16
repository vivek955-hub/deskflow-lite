package com.vivek.deskflow.ai;

import java.util.List;

import com.vivek.deskflow.ticket.Category;
import com.vivek.deskflow.ticket.Priority;

/**
 * Everything "smart" in DeskFlow goes through this interface.
 * The rest of the app does not know (or care) whether the answer came from an LLM or from rules.
 */
public interface AiService {

    /** source is "LLM" or "RULES" so the UI can show where an answer came from. */
    record TriageResult(Category category, Priority priority, String reason, String source) { }

    record AiText(String text, String source) { }

    /** Reads a new ticket and decides its category and priority. */
    TriageResult triage(String subject, String description);

    /** Drafts a reply for the agent to edit and send. */
    AiText suggestReply(String customerName, String subject, String description, Category category,
                        List<String> conversation);

    /** Sums up a long ticket thread in a few lines. */
    AiText summarize(String subject, String description, List<String> conversation);
}
