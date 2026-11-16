package com.vivek.deskflow.dashboard;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.vivek.deskflow.security.CurrentUser;
import com.vivek.deskflow.ticket.Category;
import com.vivek.deskflow.ticket.Priority;
import com.vivek.deskflow.ticket.Ticket;
import com.vivek.deskflow.ticket.TicketRepository;
import com.vivek.deskflow.ticket.TicketStatus;

@Service
public class DashboardService {
    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    public record DayCount(String day, long count) { }

    public record AgentLoad(String agent, long activeTickets) { }

    public record Dashboard(long total, long active, long unassigned, long slaBreached, long aiTriaged,
                            double avgResolutionHours, Map<String, Long> byStatus, Map<String, Long> byPriority,
                            Map<String, Long> byCategory, List<DayCount> last7Days, List<AgentLoad> agentLoad) { }

    private final TicketRepository tickets;

    public DashboardService(TicketRepository tickets) {
        this.tickets = tickets;
    }

    /**
     * Lite version: load the tenant's tickets once and count in Java.
     * At real scale these would become GROUP BY queries (or a cached summary table).
     */
    @Transactional(readOnly = true)
    public Dashboard build(CurrentUser me) {
        List<Ticket> all = tickets.findByTenantId(me.tenantId());

        // Start every bucket at zero so the charts always show every label.
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TicketStatus s : TicketStatus.values()) byStatus.put(s.name(), 0L);
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) byPriority.put(p.name(), 0L);
        Map<String, Long> byCategory = new LinkedHashMap<>();
        for (Category c : Category.values()) byCategory.put(c.name(), 0L);

        LocalDate today = LocalDate.now(ZONE);
        Map<LocalDate, Long> perDay = new TreeMap<>();
        for (int i = 6; i >= 0; i--) perDay.put(today.minusDays(i), 0L);

        Map<String, Long> load = new TreeMap<>();
        long active = 0, unassigned = 0, breached = 0, aiTriaged = 0, resolvedCount = 0;
        double resolutionHours = 0;

        for (Ticket t : all) {
            byStatus.merge(t.getStatus().name(), 1L, Long::sum);
            byPriority.merge(t.getPriority().name(), 1L, Long::sum);
            byCategory.merge(t.getCategory().name(), 1L, Long::sum);
            if (t.isSlaBreached()) breached++;
            if (t.isAiTriaged()) aiTriaged++;

            if (t.getStatus().isActive()) {
                active++;
                if (t.getAssignee() == null) {
                    unassigned++;
                } else {
                    load.merge(t.getAssignee().getName(), 1L, Long::sum);
                }
            }
            if (t.getResolvedAt() != null) {
                resolvedCount++;
                resolutionHours += Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes() / 60.0;
            }
            LocalDate created = t.getCreatedAt().atZone(ZONE).toLocalDate();
            perDay.computeIfPresent(created, (day, count) -> count + 1);
        }

        List<DayCount> last7Days = new ArrayList<>();
        perDay.forEach((day, count) -> last7Days.add(new DayCount(day.toString(), count)));
        List<AgentLoad> agentLoad = new ArrayList<>();
        load.forEach((agent, count) -> agentLoad.add(new AgentLoad(agent, count)));

        double avg = resolvedCount == 0 ? 0 : Math.round(resolutionHours / resolvedCount * 10) / 10.0;
        return new Dashboard(all.size(), active, unassigned, breached, aiTriaged, avg, byStatus, byPriority,
                byCategory, last7Days, agentLoad);
    }
}
