package com.smartdesk.ticket_service.repository;

import com.smartdesk.ticket_service.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import com.smartdesk.ticket_service.entity.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import com.smartdesk.ticket_service.entity.TicketStatus;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    Optional<Ticket> findByTicketNumber(String ticketNumber);

    Optional<Ticket> findByRequesterIdAndIdempotencyKey(
            Long requesterId,
            String idempotencyKey
    );
    Page<Ticket> findByDepartmentIdAndStatusOrderByCreatedAtAsc(
            Long departmentId,
            TicketStatus status,
            Pageable pageable
    );

    Page<Ticket> findByDepartmentIdAndAssignedAgentIsNullAndStatusOrderByCreatedAtAsc(
            Long departmentId,
            TicketStatus status,
            Pageable pageable
    );

    Page<Ticket> findByAssignedAgentIdOrderByUpdatedAtDesc(
            Long agentId,
            Pageable pageable
    );

    Page<Ticket> findByRequesterIdOrderByCreatedAtDesc(
            Long requesterId,
            Pageable pageable
    );

    long countByDepartmentId(Long departmentId);

    long countByDepartmentIdAndStatus(
            Long departmentId,
            TicketStatus status
    );

    long countByDepartmentIdAndAssignedAgentIsNullAndStatus(
            Long departmentId,
            TicketStatus status
    );

    Page<Ticket> findByDepartmentIdOrderByCreatedAtDesc(
            Long departmentId,
            Pageable pageable
    );

    boolean existsByTicketNumber(String ticketNumber);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
    UPDATE Ticket t
    SET t.assignedAgent.id = :agentId,
        t.status = :status,
        t.claimedAt = CURRENT_TIMESTAMP
    WHERE t.id = :ticketId
      AND t.department.id = :departmentId
      AND t.assignedAgent IS NULL
      AND t.status = :currentStatus
""")
    int claimTicket(
            @Param("ticketId") Long ticketId,
            @Param("agentId") Long agentId,
            @Param("departmentId") Long departmentId,
            @Param("currentStatus") TicketStatus currentStatus,
            @Param("status") TicketStatus status
    );

    @Query("""
    SELECT t FROM Ticket t
    WHERE t.id = :ticketId
      AND t.requester.email = :email
""")
    Optional<Ticket> findByIdAndRequesterEmail(
            @Param("ticketId") Long ticketId,
            @Param("email") String email
    );
}