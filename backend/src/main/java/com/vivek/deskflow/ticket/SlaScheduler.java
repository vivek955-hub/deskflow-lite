package com.vivek.deskflow.ticket;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Background job. Every minute it finds open tickets that have passed their SLA deadline,
 * flags them, and writes a line in the ticket's history. Nobody has to click anything.
 */
@Component
public class SlaScheduler {
    private static final Logger log = LoggerFactory.getLogger(SlaScheduler.class);
    private static final List<TicketStatus> ACTIVE =
            List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.PENDING);

    private final TicketRepository tickets;
    private final TicketEventRepository events;

    public SlaScheduler(TicketRepository tickets, TicketEventRepository events) {
        this.tickets = tickets;
        this.events = events;
    }

    @Scheduled(initialDelay = 15_000, fixedDelayString = "${app.sla.check-ms:60000}")
    @Transactional
    public void flagBreachedTickets() {
        Instant now = Instant.now();
        List<Ticket> overdue = tickets.findBySlaBreachedFalseAndStatusInAndDueAtBefore(ACTIVE, now);
        for (Ticket ticket : overdue) {
            ticket.setSlaBreached(true);
            events.save(new TicketEvent(ticket.getTenantId(), ticket.getId(), null, TicketEvent.Type.SLA_BREACH,
                    "SLA missed: " + ticket.getPriority() + " tickets must be resolved within "
                            + ticket.getPriority().slaHours() + " hours", now));
        }
        if (!overdue.isEmpty()) {
            log.info("Flagged {} ticket(s) as SLA breached", overdue.size());
        }
    }
}
