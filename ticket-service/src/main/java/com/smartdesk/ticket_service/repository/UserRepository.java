package com.smartdesk.ticket_service.repository;

import com.smartdesk.ticket_service.entity.ApprovalStatus;
import com.smartdesk.ticket_service.entity.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByRoles_Name(String roleName);
    List<User> findAllByOrderByCreatedAtDesc();
    List<User> findAllByApprovalStatusOrderByCreatedAtAsc(ApprovalStatus approvalStatus);

    @Query("""
        SELECT DISTINCT u FROM User u JOIN u.roles r
        WHERE u.department.id = :departmentId AND u.active = true
          AND u.approvalStatus = com.smartdesk.ticket_service.entity.ApprovalStatus.APPROVED
          AND r.name = 'SUPPORT_AGENT'
        """)
    List<User> findActiveSupportAgentsByDepartment(@Param("departmentId") Long departmentId);

    @Query("""
        SELECT DISTINCT u FROM User u JOIN u.roles r
        WHERE u.department.id = :departmentId
          AND r.name = 'SUPPORT_AGENT'
          AND u.approvalStatus = :status
        ORDER BY u.createdAt ASC
        """)
    List<User> findAgentsByDepartmentAndApprovalStatus(
            @Param("departmentId") Long departmentId,
            @Param("status") ApprovalStatus status
    );
}
