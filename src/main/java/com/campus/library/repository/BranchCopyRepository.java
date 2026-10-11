package com.campus.library.repository;

import com.campus.library.domain.BranchCopy;
import com.campus.library.domain.enums.CopyStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BranchCopyRepository extends JpaRepository<BranchCopy, Long> {

    /**
     * NFR-1 Concurrency Guard: Acquires an exclusive row-level write lock (SELECT ... FOR UPDATE)
     * with a 3000ms timeout to prevent concurrent duplicate transfer holds on the same copy.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "3000")})
    @Query("SELECT c FROM BranchCopy c WHERE c.id = :id")
    Optional<BranchCopy> findByIdForUpdate(@Param("id") Long id);

    Optional<BranchCopy> findByBarcode(String barcode);

    List<BranchCopy> findByBookId(Long bookId);

    List<BranchCopy> findByBookIdAndCurrentBranchId(Long bookId, Long branchId);

    long countByBookIdAndCurrentBranchIdAndStatus(Long bookId, Long branchId, CopyStatus status);

    List<BranchCopy> findByCurrentBranchIdAndStatus(Long branchId, CopyStatus status);
}
