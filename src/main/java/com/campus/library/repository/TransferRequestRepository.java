package com.campus.library.repository;

import com.campus.library.domain.TransferRequest;
import com.campus.library.domain.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {

    List<TransferRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<TransferRequest> findByStatus(TransferStatus status);

    List<TransferRequest> findByFromBranchIdAndStatus(Long branchId, TransferStatus status);

    List<TransferRequest> findByToBranchIdAndStatus(Long branchId, TransferStatus status);

    Optional<TransferRequest> findByCopyIdAndStatusIn(Long copyId, Collection<TransferStatus> statuses);

    /**
     * Finds transfers in READY_FOR_PICKUP state whose 48-hour window has expired.
     */
    List<TransferRequest> findByStatusAndExpiresAtBefore(TransferStatus status, LocalDateTime cutoffTime);
}
