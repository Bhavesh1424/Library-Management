package com.campus.library.service;

import com.campus.library.domain.*;
import com.campus.library.domain.enums.CopyStatus;
import com.campus.library.domain.enums.TransferStatus;
import com.campus.library.dto.transfer.TransferCreateRequest;
import com.campus.library.dto.transfer.TransferResponse;
import com.campus.library.repository.AuditLogRepository;
import com.campus.library.repository.BranchCopyRepository;
import com.campus.library.repository.BranchRepository;
import com.campus.library.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class TransferStateFlowTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private BranchCopyRepository branchCopyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    @DisplayName("Complete linear lifecycle: REQUESTED -> DISPATCHED -> READY_FOR_PICKUP with audit logs")
    void testFullTransferLifecycle() {
        User student = userRepository.findByEmail("student@campus.edu").orElseThrow();
        User csLibrarian = userRepository.findByEmail("cs.librarian@campus.edu").orElseThrow();
        User centralLibrarian = userRepository.findByEmail("central.librarian@campus.edu").orElseThrow();

        // Target copy: BC-CS-101 currently at CS-DEPT
        BranchCopy copy = branchCopyRepository.findByBarcode("BC-CS-101").orElseThrow();
        assertEquals(CopyStatus.AVAILABLE, copy.getStatus());
        assertEquals("CS-DEPT", copy.getCurrentBranch().getCode());

        Branch central = branchRepository.findByCode("CENTRAL").orElseThrow();

        // 1. Student requests transfer to Central
        TransferCreateRequest request = new TransferCreateRequest(copy.getId(), central.getId());
        TransferResponse transfer = transferService.requestTransfer(request, student);

        assertNotNull(transfer.id());
        assertEquals(TransferStatus.REQUESTED, transfer.status());
        assertEquals("CS-DEPT", transfer.fromBranchCode());
        assertEquals("CENTRAL", transfer.toBranchCode());

        BranchCopy reservedCopy = branchCopyRepository.findById(copy.getId()).orElseThrow();
        assertEquals(CopyStatus.RESERVED, reservedCopy.getStatus());

        // 2. CS Librarian dispatches item
        TransferResponse dispatched = transferService.dispatchTransfer(transfer.id(), csLibrarian);
        assertEquals(TransferStatus.DISPATCHED, dispatched.status());
        assertNotNull(dispatched.dispatchedAt());

        BranchCopy inTransitCopy = branchCopyRepository.findById(copy.getId()).orElseThrow();
        assertEquals(CopyStatus.IN_TRANSIT, inTransitCopy.getStatus());

        // 3. Central Librarian receives item
        TransferResponse received = transferService.receiveTransfer(transfer.id(), centralLibrarian);
        assertEquals(TransferStatus.READY_FOR_PICKUP, received.status());
        assertNotNull(received.arrivedAt());
        assertNotNull(received.expiresAt());

        BranchCopy readyCopy = branchCopyRepository.findById(copy.getId()).orElseThrow();
        assertEquals(CopyStatus.READY_PICKUP, readyCopy.getStatus());
        assertEquals("CENTRAL", readyCopy.getCurrentBranch().getCode(), "Current branch should update to Central");

        // 4. Verify immutable audit logs (NFR-4)
        List<AuditLog> copyAudit = auditLogRepository.findByEntityNameAndEntityIdOrderByRecordedAtDesc("BranchCopy", copy.getId());
        assertFalse(copyAudit.isEmpty());
        assertTrue(copyAudit.stream().anyMatch(l -> "TRANSFER_RESERVED".equals(l.getAction())));
        assertTrue(copyAudit.stream().anyMatch(l -> "DISPATCHED".equals(l.getAction())));
        assertTrue(copyAudit.stream().anyMatch(l -> "READY_FOR_PICKUP".equals(l.getAction())));
    }
}
