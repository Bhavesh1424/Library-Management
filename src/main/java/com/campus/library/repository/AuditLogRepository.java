package com.campus.library.repository;

import com.campus.library.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByEntityNameAndEntityIdOrderByRecordedAtDesc(String entityName, Long entityId);

    List<AuditLog> findTop100ByOrderByRecordedAtDesc();
}
