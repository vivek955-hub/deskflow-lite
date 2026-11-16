package com.vivek.deskflow.seed;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vivek.deskflow.ai.AiService.TriageResult;
import com.vivek.deskflow.ai.RuleBasedAi;
import com.vivek.deskflow.tenant.Tenant;
import com.vivek.deskflow.tenant.TenantRepository;
import com.vivek.deskflow.ticket.Comment;
import com.vivek.deskflow.ticket.CommentRepository;
import com.vivek.deskflow.ticket.Ticket;
import com.vivek.deskflow.ticket.TicketEvent;
import com.vivek.deskflow.ticket.TicketEventRepository;
import com.vivek.deskflow.ticket.TicketRepository;
import com.vivek.deskflow.ticket.TicketStatus;
import com.vivek.deskflow.user.Role;
import com.vivek.deskflow.user.User;
import com.vivek.deskflow.user.UserRepository;

/**
 * Fills an empty database with two demo companies so the app is not blank on first run.
 * Two companies on purpose: log in to each and you can see that neither can see the other's tickets.
 * Switch off with SEED_DEMO=false.
 */
@Component
public class DemoDataSeeder implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    public static final String DEMO_PASSWORD = "Password@123";

    private final boolean enabled;
    private final TenantRepository tenants;
    private final UserRepository users;
    private final TicketRepository tickets;
    private final CommentRepository comments;
    private final TicketEventRepository events;
    private final PasswordEncoder passwordEncoder;
    private final RuleBasedAi rules;

    public DemoDataSeeder(@Value("${app.seed-demo:true}") boolean enabled, TenantRepository tenants,
                          UserRepository users, TicketRepository tickets, CommentRepository comments,
                          TicketEventRepository events, PasswordEncoder passwordEncoder, RuleBasedAi rules) {
        this.enabled = enabled;
        this.tenants = tenants;
        this.users = users;
        this.tickets = tickets;
        this.comments = comments;
        this.events = events;
        this.passwordEncoder = passwordEncoder;
        this.rules = rules;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled || tenants.count() > 0) {
            return;
        }
        String hash = passwordEncoder.encode(DEMO_PASSWORD);

        // ---------- Company 1: Acme ----------
        Tenant acme = tenants.save(new Tenant("Acme Corp", "acme-corp"));
        User admin = user(acme, "Asha Rao", "admin@acme.test", hash, Role.ADMIN);
        User ravi = user(acme, "Ravi Kumar", "agent@acme.test", hash, Role.AGENT);
        User meera = user(acme, "Meera Nair", "agent2@acme.test", hash, Role.AGENT);
        User john = user(acme, "John Mathew", "customer@acme.test", hash, Role.CUSTOMER);
        User sara = user(acme, "Sara Khan", "customer2@acme.test", hash, Role.CUSTOMER);

        Ticket t1 = ticket(acme, john, "Charged twice for my monthly subscription",
                "I was charged twice on my card for the Pro plan this month. Please refund the extra payment. "
                        + "Invoice number is INV-2041.", 30, TicketStatus.IN_PROGRESS, ravi, 0);
        comment(t1, ravi, "Hi John, I can see both charges. I have raised the refund with our billing team.", false, 26);
        comment(t1, ravi, "Finance confirmed duplicate charge. Refund ticket FIN-88 opened.", true, 25);

        ticket(acme, sara, "Cannot log in after password reset",
                "I reset my password but the login page still says invalid credentials. I am locked out of my "
                        + "account and cannot access my reports.", 3, TicketStatus.OPEN, null, 0);

        ticket(acme, john, "URGENT: dashboard is down for our whole team",
                "The site is down since morning. Every page shows error 500. This is an outage for our whole "
                        + "team and we need it fixed immediately.", 6, TicketStatus.IN_PROGRESS, meera, 0);

        Ticket t4 = ticket(acme, sara, "Export to Excel fails with timeout",
                "When I export the monthly report to Excel the page keeps loading and then shows a timeout error. "
                        + "It worked fine last week.", 50, TicketStatus.PENDING, ravi, 0);
        comment(t4, ravi, "Hi Sara, could you tell me roughly how many rows the report has and which browser "
                + "you are using?", false, 46);

        ticket(acme, john, "Please add dark mode",
                "It would be nice to have a dark mode option. Many of us work late and the white screen is harsh. "
                        + "Just a suggestion, no rush.", 90, TicketStatus.OPEN, null, 0);

        Ticket t6 = ticket(acme, sara, "Need GST invoice for last payment",
                "Please send the GST invoice for the payment made on the 2nd. Our accounts team needs the receipt.",
                120, TicketStatus.RESOLVED, meera, 5);
        comment(t6, meera, "Hi Sara, the GST invoice is attached to your billing page now. Let me know if "
                + "anything is missing.", false, 116);

        ticket(acme, john, "Export report fails and shows timeout error",
                "The Excel export of the sales report fails with a timeout error after loading for a minute.",
                20, TicketStatus.OPEN, null, 0);

        ticket(acme, sara, "How do I change the email on my account?",
                "I have a question. How do I change the email address on my profile? I could not find the option.",
                140, TicketStatus.CLOSED, ravi, 2);

        ticket(acme, john, "Mobile app crashes when uploading a photo",
                "The app crashes every time I try to upload a photo from my gallery. I am unable to attach "
                        + "anything to my requests.", 10, TicketStatus.OPEN, null, 0);

        ticket(acme, sara, "Refund not received after cancellation",
                "I cancelled my subscription ten days ago but the refund has not reached my card yet.",
                75, TicketStatus.RESOLVED, ravi, 30);

        ticket(acme, john, "API returns 404 for the orders endpoint",
                "Our integration calls the orders API and it fails with a 404 error since yesterday. "
                        + "We are blocked on a client deadline.", 14, TicketStatus.IN_PROGRESS, meera, 0);

        ticket(acme, sara, "Feedback on the new reports page",
                "Just some feedback: the new reports page looks great. One suggestion is to remember the last "
                        + "filter I used.", 160, TicketStatus.RESOLVED, meera, 20);

        // ---------- Company 2: Globex (proves tenant isolation) ----------
        Tenant globex = tenants.save(new Tenant("Globex Ltd", "globex-ltd"));
        User gAdmin = user(globex, "Priya Singh", "admin@globex.test", hash, Role.ADMIN);
        User gCustomer = user(globex, "Tom Varghese", "customer@globex.test", hash, Role.CUSTOMER);
        ticket(globex, gCustomer, "Payment page shows an error",
                "The payment page shows an error when I enter my card details. I cannot complete the purchase.",
                8, TicketStatus.OPEN, null, 0);
        ticket(globex, gCustomer, "Request for annual pricing",
                "I am wondering if you offer annual pricing. Please share the price list.",
                40, TicketStatus.IN_PROGRESS, gAdmin, 0);

        log.info("Demo data created. Log in with admin@acme.test / {} (see README for all demo users)", DEMO_PASSWORD);
        if (admin.getId() == null) {
            throw new IllegalStateException("Demo data was not saved");
        }
    }

    private User user(Tenant tenant, String name, String email, String hash, Role role) {
        return users.save(new User(tenant.getId(), name, email, hash, role));
    }

    /**
     * @param hoursAgo           how long ago the ticket was raised (backdated so the charts look real)
     * @param resolvedAfterHours for RESOLVED/CLOSED tickets, how long the fix took
     */
    private Ticket ticket(Tenant tenant, User requester, String subject, String description, int hoursAgo,
                          TicketStatus status, User assignee, int resolvedAfterHours) {
        Instant createdAt = Instant.now().minus(hoursAgo, ChronoUnit.HOURS);
        TriageResult triage = rules.triage(subject, description);

        Ticket ticket = new Ticket(tenant.getId(), subject, description, triage.priority(), triage.category(),
                requester, createdAt);
        ticket.setAiTriaged(true);
        ticket.setStatus(status);
        ticket.setAssignee(assignee);
        if (status.isActive()) {
            ticket.setSlaBreached(Instant.now().isAfter(ticket.getDueAt()));
        } else {
            Instant resolvedAt = createdAt.plus(resolvedAfterHours, ChronoUnit.HOURS);
            ticket.setResolvedAt(resolvedAt);
            ticket.setUpdatedAt(resolvedAt);
            ticket.setSlaBreached(resolvedAt.isAfter(ticket.getDueAt()));
        }
        tickets.save(ticket);

        events.save(new TicketEvent(tenant.getId(), ticket.getId(), requester, TicketEvent.Type.CREATED,
                "Ticket created", createdAt));
        events.save(new TicketEvent(tenant.getId(), ticket.getId(), null, TicketEvent.Type.AI_TRIAGE,
                "AI set " + triage.category() + " / " + triage.priority() + ". " + triage.reason(), createdAt));
        if (assignee != null) {
            events.save(new TicketEvent(tenant.getId(), ticket.getId(), assignee, TicketEvent.Type.ASSIGNMENT,
                    "Assigned to " + assignee.getName(), createdAt.plus(20, ChronoUnit.MINUTES)));
        }
        return ticket;
    }

    private void comment(Ticket ticket, User author, String body, boolean internal, int hoursAgo) {
        comments.save(new Comment(ticket.getTenantId(), ticket.getId(), author, body, internal,
                Instant.now().minus(hoursAgo, ChronoUnit.HOURS)));
    }
}
