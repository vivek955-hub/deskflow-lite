package com.vivek.deskflow.ticket;

import java.time.Instant;
import java.util.List;

import com.vivek.deskflow.user.Role;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class TicketDtos {
    private TicketDtos() { }

    /** priority and category are optional: if either is missing, the AI triage fills it in. */
    public record CreateTicketRequest(
            @NotBlank(message = "Subject is required")
            @Size(max = 200, message = "Subject must be 200 characters or fewer") String subject,
            @NotBlank(message = "Description is required")
            @Size(max = 5000, message = "Description must be 5000 characters or fewer") String description,
            Priority priority,
            Category category) { }

    /** A partial update: only the fields that are not null are changed. */
    public record UpdateTicketRequest(TicketStatus status, Priority priority, Category category,
                                      Long assigneeId, Boolean unassign) { }

    public record AddCommentRequest(
            @NotBlank(message = "Write a message first")
            @Size(max = 5000, message = "Message must be 5000 characters or fewer") String body,
            Boolean internal) { }

    public record TicketSummary(Long id, String subject, TicketStatus status, Priority priority, Category category,
                                String requesterName, String assigneeName, boolean aiTriaged, boolean slaBreached,
                                Instant dueAt, Instant createdAt, Instant updatedAt) {
        public static TicketSummary from(Ticket t) {
            return new TicketSummary(t.getId(), t.getSubject(), t.getStatus(), t.getPriority(), t.getCategory(),
                    t.getRequester().getName(), t.getAssignee() == null ? null : t.getAssignee().getName(),
                    t.isAiTriaged(), t.isSlaBreached(), t.getDueAt(), t.getCreatedAt(), t.getUpdatedAt());
        }
    }

    public record CommentResponse(Long id, String authorName, Role authorRole, String body, boolean internal,
                                  Instant createdAt) {
        public static CommentResponse from(Comment c) {
            return new CommentResponse(c.getId(), c.getAuthor().getName(), c.getAuthor().getRole(), c.getBody(),
                    c.isInternal(), c.getCreatedAt());
        }
    }

    public record EventResponse(Long id, TicketEvent.Type type, String detail, String actorName, Instant createdAt) {
        public static EventResponse from(TicketEvent e) {
            return new EventResponse(e.getId(), e.getType(), e.getDetail(),
                    e.getActor() == null ? "System" : e.getActor().getName(), e.getCreatedAt());
        }
    }

    public record TicketDetail(Long id, String subject, String description, TicketStatus status, Priority priority,
                               Category category, Long requesterId, String requesterName, String requesterEmail,
                               Long assigneeId, String assigneeName, boolean aiTriaged, boolean slaBreached,
                               Instant dueAt, Instant resolvedAt, Instant createdAt, Instant updatedAt,
                               List<CommentResponse> comments, List<EventResponse> events) { }

    public record SimilarTicket(Long id, String subject, TicketStatus status, int matchPercent) { }
}
