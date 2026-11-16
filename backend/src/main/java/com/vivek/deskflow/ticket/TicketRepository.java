package com.vivek.deskflow.ticket;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    // Never findById alone: a ticket is only ever loaded together with its tenant.
    @EntityGraph(attributePaths = { "requester", "assignee" })
    Optional<Ticket> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * One query for the ticket list. Every filter is optional: passing null switches it off.
     * The EntityGraph loads requester and assignee in the same SQL, avoiding the N+1 problem.
     */
    @EntityGraph(attributePaths = { "requester", "assignee" })
    @Query("""
            select t from Ticket t
            where t.tenantId = :tenantId
              and (:status is null or t.status = :status)
              and (:priority is null or t.priority = :priority)
              and (:requesterId is null or t.requester.id = :requesterId)
              and (:assigneeId is null or t.assignee.id = :assigneeId)
              and (:q is null or lower(t.subject) like lower(concat('%', :q, '%')))
            """)
    Page<Ticket> search(@Param("tenantId") Long tenantId,
                        @Param("status") TicketStatus status,
                        @Param("priority") Priority priority,
                        @Param("requesterId") Long requesterId,
                        @Param("assigneeId") Long assigneeId,
                        @Param("q") String q,
                        Pageable pageable);

    @EntityGraph(attributePaths = { "assignee" })
    List<Ticket> findByTenantId(Long tenantId);

    List<Ticket> findTop200ByTenantIdOrderByCreatedAtDesc(Long tenantId);

    /** Used by the SLA job: open tickets that are past their deadline and not yet flagged. */
    List<Ticket> findBySlaBreachedFalseAndStatusInAndDueAtBefore(Collection<TicketStatus> statuses, Instant now);
}
