package com.campus.library.scheduler;

import com.campus.library.domain.BranchCopy;
import com.campus.library.domain.TransferRequest;
import com.campus.library.domain.enums.CopyStatus;
import com.campus.library.domain.enums.TransferStatus;
import com.campus.library.repository.BranchCopyRepository;
import com.campus.library.repository.TransferRequestRepository;
import com.campus.library.service.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * PRD Section 4.1 Auto-Cancellation Window:
 * Arrived books in READY_FOR_PICKUP revert to available branch stock if unclaimed after 48 hours.
 */
@Component
public class HoldExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(HoldExpiryScheduler.class);

    private final TransferRequestRepository transferRequestRepository;
    private final BranchCopyRepository branchCopyRepository;
    private final AuditService auditService;

    public HoldExpiryScheduler(TransferRequestRepository transferRequestRepository,
                               BranchCopyRepository branchCopyRepository,
                               AuditService auditService) {
        this.transferRequestRepository = transferRequestRepository;
        this.branchCopyRepository = branchCopyRepository;
        this.auditService = auditService;
    }

    @Scheduled(fixedDelayString = "${app.hold.expiry-check-ms:60000}")
    @Transactional
    public void processExpiredHolds() {
        LocalDateTime now = LocalDateTime.now();
        List<TransferRequest> expiredTransfers = transferRequestRepository
                .findByStatusAndExpiresAtBefore(TransferStatus.READY_FOR_PICKUP, now);

        if (expiredTransfers.isEmpty()) {
            return;
        }

        log.info("Hold Expiry Daemon: Detected {} expired pickup holds. Reverting to available stock...", expiredTransfers.size());

        for (TransferRequest transfer : expiredTransfers) {
            BranchCopy copy = transfer.getCopy();
            copy.setStatus(CopyStatus.AVAILABLE);
            branchCopyRepository.save(copy);

            transfer.setStatus(TransferStatus.EXPIRED);
            transferRequestRepository.save(transfer);

            auditService.recordAction("BranchCopy", copy.getId(), "HOLD_EXPIRED",
                    CopyStatus.READY_PICKUP.name(), CopyStatus.AVAILABLE.name(), null,
                    "48-hour pickup window expired for student " + transfer.getStudent().getUniversityId() + ". Restored to available.");

            auditService.recordAction("TransferRequest", transfer.getId(), "EXPIRED",
                    TransferStatus.READY_FOR_PICKUP.name(), TransferStatus.EXPIRED.name(), null,
                    "Unclaimed hold cancelled automatically by system daemon.");

            log.info("Expired hold cancelled for copy [{}] at [{}]", copy.getBarcode(), copy.getCurrentBranch().getCode());
        }
    }
}
