package com.campus.library.service;

import com.campus.library.domain.*;
import com.campus.library.domain.enums.CopyStatus;
import com.campus.library.domain.enums.TransferStatus;
import com.campus.library.dto.loan.CheckoutRequest;
import com.campus.library.dto.loan.LoanResponse;
import com.campus.library.repository.BranchCopyRepository;
import com.campus.library.repository.BranchRepository;
import com.campus.library.repository.LoanRepository;
import com.campus.library.repository.TransferRequestRepository;
import com.campus.library.repository.UserRepository;
import com.campus.library.scheduler.HoldExpiryScheduler;
import com.campus.library.scheduler.OverdueFineScheduler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class CirculationAndSchedulerTest {

    @Autowired
    private CirculationService circulationService;

    @Autowired
    private HoldExpiryScheduler holdExpiryScheduler;

    @Autowired
    private OverdueFineScheduler overdueFineScheduler;

    @Autowired
    private BranchCopyRepository branchCopyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRepository loanRepository;

    @Autowired
    private TransferRequestRepository transferRequestRepository;

    @Test
    @DisplayName("Checkout and Return: Status changes AVAILABLE -> ISSUED -> AVAILABLE")
    void testCheckoutAndReturnFlow() {
        User librarian = userRepository.findByEmail("cs.librarian@campus.edu").orElseThrow();
        User student = userRepository.findByEmail("student@campus.edu").orElseThrow();
        BranchCopy copy = branchCopyRepository.findByBarcode("BC-MECH-401").orElseThrow();

        // 1. Checkout
        CheckoutRequest checkoutReq = new CheckoutRequest("BC-MECH-401", student.getId());
        LoanResponse loan = circulationService.checkoutCopy(checkoutReq, librarian);

        assertNotNull(loan.id());
        assertEquals(copy.getId(), loan.copyId());
        assertEquals(student.getId(), loan.studentId());
        assertNull(loan.returnDate());

        BranchCopy issuedCopy = branchCopyRepository.findById(copy.getId()).orElseThrow();
        assertEquals(CopyStatus.ISSUED, issuedCopy.getStatus());

        // 2. Return
        LoanResponse returnedLoan = circulationService.returnCopy(loan.id(), null, librarian);
        assertNotNull(returnedLoan.returnDate());
        assertEquals(LocalDate.now(), returnedLoan.returnDate());

        BranchCopy returnedCopy = branchCopyRepository.findById(copy.getId()).orElseThrow();
        assertEquals(CopyStatus.AVAILABLE, returnedCopy.getStatus());
    }

    @Test
    @DisplayName("Overdue Fine Daemon: Accrues ₹5.00 per day past due")
    void testOverdueFineAccrual() {
        User student = userRepository.findByEmail("student@campus.edu").orElseThrow();
        BranchCopy copy = branchCopyRepository.findByBarcode("BC-MECH-402").orElseThrow();

        // Create an overdue loan: issued 20 days ago, due 6 days ago
        LocalDate issueDate = LocalDate.now().minusDays(20);
        LocalDate dueDate = LocalDate.now().minusDays(6); // 6 days overdue
        Loan overdueLoan = new Loan(copy, student, issueDate, dueDate);
        overdueLoan = loanRepository.save(overdueLoan);

        // Run daemon
        int processedCount = overdueFineScheduler.calculateFinesForDate(LocalDate.now());
        assertTrue(processedCount > 0, "Daemon should process at least 1 overdue loan");

        Loan updated = loanRepository.findById(overdueLoan.getId()).orElseThrow();
        // 6 days * 5.00 = 30.00
        BigDecimal expectedFine = BigDecimal.valueOf(30.00).setScale(2);
        assertEquals(expectedFine, updated.getFineAmount().setScale(2), "Fine should be Days * ₹5.00");
    }

    @Test
    @DisplayName("Hold Expiry Daemon: Auto-cancels unclaimed hold after 48h and restores copy to AVAILABLE")
    void testHoldAutoCancellation() {
        User student = userRepository.findByEmail("student@campus.edu").orElseThrow();
        BranchCopy copy = branchCopyRepository.findByBarcode("BC-CS-501").orElseThrow();
        Branch cs = branchRepository.findByCode("CS-DEPT").orElseThrow();
        Branch central = branchRepository.findByCode("CENTRAL").orElseThrow();

        // Set copy to READY_PICKUP
        copy.setStatus(CopyStatus.READY_PICKUP);
        branchCopyRepository.save(copy);

        // Create expired transfer (expiresAt was 2 hours ago, arrived 50h ago)
        TransferRequest expiredTransfer = new TransferRequest(copy, student, cs, central);
        expiredTransfer.setStatus(TransferStatus.READY_FOR_PICKUP);
        expiredTransfer.setArrivedAt(LocalDateTime.now().minusHours(50));
        expiredTransfer.setExpiresAt(LocalDateTime.now().minusHours(2));
        expiredTransfer = transferRequestRepository.save(expiredTransfer);

        // Run daemon
        holdExpiryScheduler.processExpiredHolds();

        TransferRequest refreshedTransfer = transferRequestRepository.findById(expiredTransfer.getId()).orElseThrow();
        assertEquals(TransferStatus.EXPIRED, refreshedTransfer.getStatus(), "Unclaimed transfer should be marked EXPIRED");

        BranchCopy refreshedCopy = branchCopyRepository.findById(copy.getId()).orElseThrow();
        assertEquals(CopyStatus.AVAILABLE, refreshedCopy.getStatus(), "Copy should revert back to AVAILABLE status");
    }
}
