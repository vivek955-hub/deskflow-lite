package com.vivek.deskflow.ticket;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivek.deskflow.ai.AiService;
import com.vivek.deskflow.ai.AiService.AiText;
import com.vivek.deskflow.ai.AiService.TriageResult;
import com.vivek.deskflow.ai.SimilarityService;
import com.vivek.deskflow.common.ApiException;
import com.vivek.deskflow.common.PageResponse;
import com.vivek.deskflow.security.CurrentUser;
import com.vivek.deskflow.ticket.TicketDtos.AddCommentRequest;
import com.vivek.deskflow.ticket.TicketDtos.CommentResponse;
import com.vivek.deskflow.ticket.TicketDtos.CreateTicketRequest;
import com.vivek.deskflow.ticket.TicketDtos.EventResponse;
import com.vivek.deskflow.ticket.TicketDtos.SimilarTicket;
import com.vivek.deskflow.ticket.TicketDtos.TicketDetail;
import com.vivek.deskflow.ticket.TicketDtos.TicketSummary;
import com.vivek.deskflow.ticket.TicketDtos.UpdateTicketRequest;
import com.vivek.deskflow.user.Role;
import com.vivek.deskflow.user.User;
import com.vivek.deskflow.user.UserRepository;

/**
 * All ticket rules live here. Two rules are applied in every method:
 *  1. Tenant isolation - data is always loaded with the tenantId from the logged-in user's token.
 *  2. Ownership       - a CUSTOMER can only see and touch the tickets they raised themselves.
 */
@Service
public class TicketService {
    private final TicketRepository tickets;
    private final CommentRepository comments;
    private final TicketEventRepository events;
    private final UserRepository users;
    private final AiService ai;
    private final SimilarityService similarity;

    public TicketService(TicketRepository tickets, CommentRepository comments, TicketEventRepository events,
                         UserRepository users, AiService ai, SimilarityService similarity) {
        this.tickets = tickets;
        this.comments = comments;
        this.events = events;
        this.users = users;
        this.ai = ai;
        this.similarity = similarity;
    }

    @Transactional
    public TicketDetail create(CurrentUser me, CreateTicketRequest req) {
        User requester = loadMe(me);
        Priority priority = req.priority();
        Category category = req.category();

        // If the person did not choose a priority or category, the AI decides.
        TriageResult triage = null;
        if (priority == null || category == null) {
            triage = ai.triage(req.subject(), req.description());
            if (priority == null) priority = triage.priority();
            if (category == null) category = triage.category();
        }

        Instant now = Instant.now();
        Ticket ticket = new Ticket(me.tenantId(), req.subject().trim(), req.description().trim(), priority, category,
                requester, now);
        ticket.setAiTriaged(triage != null);
        tickets.save(ticket);

        log(ticket, requester, TicketEvent.Type.CREATED, "Ticket created");
        if (triage != null) {
            log(ticket, null, TicketEvent.Type.AI_TRIAGE,
                    "AI set " + category + " / " + priority + ". " + triage.reason());
        }
        return toDetail(me, ticket);
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketSummary> search(CurrentUser me, TicketStatus status, Priority priority, String q,
                                              boolean mine, int page, int size) {
        // Customers are always limited to their own tickets, whatever filters they send.
        Long requesterId = me.isCustomer() ? me.id() : null;
        Long assigneeId = (mine && me.isStaff()) ? me.id() : null;
        String query = (q == null || q.isBlank()) ? null : q.trim();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.of(
                tickets.search(me.tenantId(), status, priority, requesterId, assigneeId, query, pageable),
                TicketSummary::from);
    }

    @Transactional(readOnly = true)
    public TicketDetail get(CurrentUser me, Long id) {
        return toDetail(me, loadVisible(me, id));
    }

    @Transactional
    public TicketDetail update(CurrentUser me, Long id, UpdateTicketRequest req) {
        Ticket ticket = loadVisible(me, id);
        User actor = loadMe(me);

        if (req.status() != null && req.status() != ticket.getStatus()) {
            changeStatus(ticket, actor, req.status());
        }
        if (req.priority() != null && req.priority() != ticket.getPriority()) {
            log(ticket, actor, TicketEvent.Type.PRIORITY, "Priority " + ticket.getPriority() + " → " + req.priority());
            ticket.setPriority(req.priority());
            ticket.recalculateDueAt(); // a new priority means a new SLA deadline
            if (ticket.getStatus().isActive()) {
                ticket.setSlaBreached(Instant.now().isAfter(ticket.getDueAt()));
            }
        }
        if (req.category() != null && req.category() != ticket.getCategory()) {
            log(ticket, actor, TicketEvent.Type.CATEGORY, "Category " + ticket.getCategory() + " → " + req.category());
            ticket.setCategory(req.category());
        }
        if (Boolean.TRUE.equals(req.unassign()) && ticket.getAssignee() != null) {
            log(ticket, actor, TicketEvent.Type.ASSIGNMENT, "Unassigned from " + ticket.getAssignee().getName());
            ticket.setAssignee(null);
        } else if (req.assigneeId() != null
                && (ticket.getAssignee() == null || !ticket.getAssignee().getId().equals(req.assigneeId()))) {
            // The assignee is looked up inside the same tenant, so you cannot assign to another company's agent.
            User assignee = users.findByIdAndTenantId(req.assigneeId(), me.tenantId())
                    .orElseThrow(() -> ApiException.notFound("Assignee"));
            if (assignee.getRole() == Role.CUSTOMER || !assignee.isActive()) {
                throw ApiException.badRequest("Tickets can only be assigned to active agents or admins");
            }
            ticket.setAssignee(assignee);
            log(ticket, actor, TicketEvent.Type.ASSIGNMENT, "Assigned to " + assignee.getName());
            if (ticket.getStatus() == TicketStatus.OPEN) {
                changeStatus(ticket, actor, TicketStatus.IN_PROGRESS);
            }
        }
        ticket.touch();
        return toDetail(me, ticket);
    }

    @Transactional
    public TicketDetail addComment(CurrentUser me, Long id, AddCommentRequest req) {
        Ticket ticket = loadVisible(me, id);
        User author = loadMe(me);
        boolean internal = Boolean.TRUE.equals(req.internal());
        if (internal && me.isCustomer()) {
            throw ApiException.forbidden("Only staff can write internal notes");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw ApiException.badRequest("This ticket is closed. Please open a new ticket.");
        }
        comments.save(new Comment(me.tenantId(), ticket.getId(), author, req.body().trim(), internal, Instant.now()));

        // A customer replying means the ball is back with the support team.
        if (me.isCustomer()
                && (ticket.getStatus() == TicketStatus.PENDING || ticket.getStatus() == TicketStatus.RESOLVED)) {
            changeStatus(ticket, author, TicketStatus.OPEN);
        }
        ticket.touch();
        return toDetail(me, ticket);
    }

    @Transactional(readOnly = true)
    public List<SimilarTicket> similar(CurrentUser me, Long id) {
        Ticket ticket = loadVisible(me, id);
        return similarity.findSimilar(ticket, tickets.findTop200ByTenantIdOrderByCreatedAtDesc(me.tenantId()));
    }

    @Transactional(readOnly = true)
    public AiText suggestReply(CurrentUser me, Long id) {
        Ticket ticket = loadVisible(me, id);
        return ai.suggestReply(ticket.getRequester().getName(), ticket.getSubject(), ticket.getDescription(),
                ticket.getCategory(), conversation(ticket, false));
    }

    @Transactional(readOnly = true)
    public AiText summarize(CurrentUser me, Long id) {
        Ticket ticket = loadVisible(me, id);
        return ai.summarize(ticket.getSubject(), ticket.getDescription(), conversation(ticket, true));
    }

    // ---------- helpers ----------

    private void changeStatus(Ticket ticket, User actor, TicketStatus next) {
        TicketStatus previous = ticket.getStatus();
        ticket.setStatus(next);
        log(ticket, actor, TicketEvent.Type.STATUS, "Status " + previous + " → " + next);

        Instant now = Instant.now();
        if (next == TicketStatus.RESOLVED || next == TicketStatus.CLOSED) {
            if (ticket.getResolvedAt() == null) {
                ticket.setResolvedAt(now);
                if (now.isAfter(ticket.getDueAt())) {
                    ticket.setSlaBreached(true);
                }
            }
        } else {
            ticket.setResolvedAt(null); // reopened
        }
    }

    /** Loads a ticket the current user is allowed to see, or answers 404 (not 403, so ids cannot be probed). */
    private Ticket loadVisible(CurrentUser me, Long id) {
        Ticket ticket = tickets.findByIdAndTenantId(id, me.tenantId())
                .orElseThrow(() -> ApiException.notFound("Ticket"));
        if (me.isCustomer() && !ticket.getRequester().getId().equals(me.id())) {
            throw ApiException.notFound("Ticket");
        }
        return ticket;
    }

    private User loadMe(CurrentUser me) {
        return users.findByIdAndTenantId(me.id(), me.tenantId()).orElseThrow(() -> ApiException.notFound("User"));
    }

    private void log(Ticket ticket, User actor, TicketEvent.Type type, String detail) {
        events.save(new TicketEvent(ticket.getTenantId(), ticket.getId(), actor, type, detail, Instant.now()));
    }

    private List<String> conversation(Ticket ticket, boolean includeInternal) {
        List<String> lines = new ArrayList<>();
        for (Comment c : comments.findByTicketIdAndTenantIdOrderByCreatedAtAsc(ticket.getId(), ticket.getTenantId())) {
            if (c.isInternal() && !includeInternal) continue;
            String who = c.getAuthor().getRole() == Role.CUSTOMER ? "Customer" : "Agent";
            lines.add(who + (c.isInternal() ? " (internal note)" : "") + ": " + c.getBody());
        }
        return lines;
    }

    private TicketDetail toDetail(CurrentUser me, Ticket t) {
        List<CommentResponse> commentList = comments
                .findByTicketIdAndTenantIdOrderByCreatedAtAsc(t.getId(), t.getTenantId()).stream()
                .filter(c -> !c.isInternal() || me.isStaff()) // customers never receive internal notes
                .map(CommentResponse::from)
                .toList();
        List<EventResponse> eventList = events
                .findByTicketIdAndTenantIdOrderByCreatedAtAsc(t.getId(), t.getTenantId()).stream()
                .map(EventResponse::from)
                .toList();
        User assignee = t.getAssignee();
        return new TicketDetail(t.getId(), t.getSubject(), t.getDescription(), t.getStatus(), t.getPriority(),
                t.getCategory(), t.getRequester().getId(), t.getRequester().getName(), t.getRequester().getEmail(),
                assignee == null ? null : assignee.getId(), assignee == null ? null : assignee.getName(),
                t.isAiTriaged(), t.isSlaBreached(), t.getDueAt(), t.getResolvedAt(), t.getCreatedAt(),
                t.getUpdatedAt(), commentList, eventList);
    }
}
