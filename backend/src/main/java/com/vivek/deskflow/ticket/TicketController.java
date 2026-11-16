package com.vivek.deskflow.ticket;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.vivek.deskflow.ai.AiService.AiText;
import com.vivek.deskflow.common.PageResponse;
import com.vivek.deskflow.security.CurrentUser;
import com.vivek.deskflow.ticket.TicketDtos.AddCommentRequest;
import com.vivek.deskflow.ticket.TicketDtos.CreateTicketRequest;
import com.vivek.deskflow.ticket.TicketDtos.SimilarTicket;
import com.vivek.deskflow.ticket.TicketDtos.TicketDetail;
import com.vivek.deskflow.ticket.TicketDtos.TicketSummary;
import com.vivek.deskflow.ticket.TicketDtos.UpdateTicketRequest;

import jakarta.validation.Valid;

/** Controllers stay thin: read the request, call the service, return the result. */
@RestController
@RequestMapping("/api/tickets")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDetail create(@AuthenticationPrincipal CurrentUser me, @Valid @RequestBody CreateTicketRequest req) {
        return ticketService.create(me, req);
    }

    @GetMapping
    public PageResponse<TicketSummary> list(@AuthenticationPrincipal CurrentUser me,
                                            @RequestParam(required = false) TicketStatus status,
                                            @RequestParam(required = false) Priority priority,
                                            @RequestParam(required = false) String q,
                                            @RequestParam(defaultValue = "false") boolean mine,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "10") int size) {
        return ticketService.search(me, status, priority, q, mine, page, size);
    }

    @GetMapping("/{id}")
    public TicketDetail get(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        return ticketService.get(me, id);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public TicketDetail update(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id,
                               @RequestBody UpdateTicketRequest req) {
        return ticketService.update(me, id, req);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public TicketDetail addComment(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id,
                                   @Valid @RequestBody AddCommentRequest req) {
        return ticketService.addComment(me, id, req);
    }

    @GetMapping("/{id}/similar")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public List<SimilarTicket> similar(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        return ticketService.similar(me, id);
    }

    @PostMapping("/{id}/ai/reply")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public AiText suggestReply(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        return ticketService.suggestReply(me, id);
    }

    @PostMapping("/{id}/ai/summary")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public AiText summarize(@AuthenticationPrincipal CurrentUser me, @PathVariable Long id) {
        return ticketService.summarize(me, id);
    }
}
