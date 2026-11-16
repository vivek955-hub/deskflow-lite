package com.vivek.deskflow.ticket;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = { "author" })
    List<Comment> findByTicketIdAndTenantIdOrderByCreatedAtAsc(Long ticketId, Long tenantId);
}
