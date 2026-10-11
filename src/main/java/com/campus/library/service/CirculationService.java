package com.campus.library.service;

import com.campus.library.domain.Branch;
import com.campus.library.domain.BranchCopy;
import com.campus.library.domain.Loan;
import com.campus.library.domain.TransferRequest;
import com.campus.library.domain.User;
import com.campus.library.domain.enums.CopyStatus;
import com.campus.library.domain.enums.TransferStatus;
import com.campus.library.dto.loan.CheckoutRequest;
import com.campus.library.dto.loan.LoanResponse;
import com.campus.library.exception.BadRequestException;
import com.campus.library.exception.ConflictException;
import com.campus.library.exception.ResourceNotFoundException;
import com.campus.library.repository.BranchCopyRepository;
import com.campus.library.repository.BranchRepository;
import com.campus.library.repository.LoanRepository;
import com.campus.library.repository.TransferRequestRepository;
import com.campus.library.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CirculationService {

    private final LoanRepository loanRepository;
    private final BranchCopyRepository branchCopyRepository;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final AuditService auditService;
    private final int loanDurationDays;
    private final double dailyFineRate;

    public CirculationService(LoanRepository loanRepository,
                              BranchCopyRepository branchCopyRepository,
                              UserRepository userRepository,
                              BranchRepository branchRepository,
                              TransferRequestRepository transferRequestRepository,
                              AuditService auditService,
                              @Value("${app.loan.duration-days:14}") int loanDurationDays,
                              @Value("${app.loan.daily-fine-rate:5.00}") double dailyFineRate) {
        this.loanRepository = loanRepository;
        this.branchCopyRepository = branchCopyRepository;
        this.userRepository = userRepository;
        this.branchRepository = branchRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.auditService = auditService;
        this.loanDurationDays = loanDurationDays;
        this.dailyFineRate = dailyFineRate;
    }

    /**
     * PRD Section 5: Assigns physical barcode copy to student ID (POST /api/v1/loans/checkout).
     * State transition: READY_PICKUP or AVAILABLE -> ISSUED
     */
    @Transactional
    public LoanResponse checkoutCopy(CheckoutRequest request, User librarian) {
        BranchCopy copy = branchCopyRepository.findByBarcode(request.barcode())
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found with barcode: " + request.barcode()));

        CopyStatus currentStatus = copy.getStatus();

        if (currentStatus == CopyStatus.ISSUED) {
            throw new ConflictException("Book copy [" + copy.getBarcode() + "] is already checked out.");
        }
        if (currentStatus == CopyStatus.RESERVED || currentStatus == CopyStatus.IN_TRANSIT) {
            throw new ConflictException("Book copy [" + copy.getBarcode() + "] is currently in transit / reserved and cannot be checked out.");
        }

        User student = null;

        // If arrived via transfer, complete the transfer pipeline
        if (currentStatus == CopyStatus.READY_PICKUP) {
            Optional<TransferRequest> transferOpt = transferRequestRepository.findByCopyIdAndStatusIn(
                    copy.getId(), List.of(TransferStatus.READY_FOR_PICKUP));

            if (transferOpt.isPresent()) {
                TransferRequest transfer = transferOpt.get();
                // If request didn't specify studentId or if it matches the hold student, automatically use the hold student!
                if (request.studentId() == null || transfer.getStudent().getId().equals(request.studentId())) {
                    student = transfer.getStudent();
                } else {
                    User reqStudent = userRepository.findById(request.studentId()).orElse(null);
                    String reqIdentifier = reqStudent != null ? reqStudent.getFullName() + " (" + reqStudent.getUniversityId() + ")" : "ID " + request.studentId();
                    throw new ConflictException("Book copy [" + copy.getBarcode() + "] is on hold for student " +
                            transfer.getStudent().getFullName() + " (" + transfer.getStudent().getUniversityId() + "), not " + reqIdentifier);
                }

                transfer.setStatus(TransferStatus.COMPLETED);
                transfer.setCompletedAt(LocalDateTime.now());
                transferRequestRepository.save(transfer);

                auditService.recordAction("TransferRequest", transfer.getId(), "COMPLETED",
                        TransferStatus.READY_FOR_PICKUP.name(), TransferStatus.COMPLETED.name(), librarian,
                        "Transfer fulfilled via checkout to student " + student.getUniversityId());
            }
        }

        if (student == null) {
            if (request.studentId() == null) {
                throw new ConflictException("Student ID is required for checkout of non-hold volumes.");
            }
            student = userRepository.findById(request.studentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + request.studentId()));
        }

        // Transition copy state to ISSUED
        copy.setStatus(CopyStatus.ISSUED);
        branchCopyRepository.save(copy);

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(loanDurationDays);
        Loan loan = new Loan(copy, student, issueDate, dueDate);
        Loan savedLoan = loanRepository.save(loan);

        auditService.recordAction("BranchCopy", copy.getId(), "CHECKOUT",
                currentStatus.name(), CopyStatus.ISSUED.name(), librarian,
                "Checked out to " + student.getUniversityId() + " (" + student.getFullName() + ")");

        auditService.recordAction("Loan", savedLoan.getId(), "CREATED",
                null, "ACTIVE", librarian, "Loan created. Due date: " + dueDate);

        return mapToResponse(savedLoan);
    }

    /**
     * Return book copy to available inventory and calculate overdue fine if applicable.
     */
    @Transactional
    public LoanResponse returnCopy(Long loanId, Long returnBranchId, User librarian) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Loan record not found with ID: " + loanId));

        if (loan.getReturnDate() != null) {
            throw new BadRequestException("Loan #" + loanId + " has already been returned on " + loan.getReturnDate());
        }

        LocalDate returnDate = LocalDate.now();
        loan.setReturnDate(returnDate);

        // Calculate overdue fines via formula: Days Elapsed Past Due * ₹5.00
        if (returnDate.isAfter(loan.getDueDate())) {
            long daysPastDue = ChronoUnit.DAYS.between(loan.getDueDate(), returnDate);
            BigDecimal fine = BigDecimal.valueOf(daysPastDue * dailyFineRate);
            loan.setFineAmount(fine);
        }

        BranchCopy copy = loan.getCopy();
        copy.setStatus(CopyStatus.AVAILABLE);

        if (returnBranchId != null) {
            Branch returnBranch = branchRepository.findById(returnBranchId)
                    .orElseThrow(() -> new ResourceNotFoundException("Return branch not found with ID: " + returnBranchId));
            copy.setCurrentBranch(returnBranch);
        }
        branchCopyRepository.save(copy);
        Loan updatedLoan = loanRepository.save(loan);

        auditService.recordAction("BranchCopy", copy.getId(), "RETURN",
                CopyStatus.ISSUED.name(), CopyStatus.AVAILABLE.name(), librarian,
                "Returned and made available at " + copy.getCurrentBranch().getCode());

        auditService.recordAction("Loan", updatedLoan.getId(), "RETURNED",
                "ACTIVE", "RETURNED", librarian, "Final fine assessed: ₹" + updatedLoan.getFineAmount());

        return mapToResponse(updatedLoan);
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getStudentLoans(Long studentId) {
        return loanRepository.findByStudentIdOrderByIssueDateDesc(studentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<LoanResponse> getActiveLoans() {
        return loanRepository.findAll().stream()
                .filter(l -> l.getReturnDate() == null)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private LoanResponse mapToResponse(Loan loan) {
        boolean isOverdue = loan.getReturnDate() == null && LocalDate.now().isAfter(loan.getDueDate());
        return new LoanResponse(
                loan.getId(),
                loan.getCopy().getId(),
                loan.getCopy().getBarcode(),
                loan.getCopy().getBook().getTitle(),
                loan.getCopy().getBook().getIsbn(),
                loan.getStudent().getId(),
                loan.getStudent().getFullName(),
                loan.getStudent().getUniversityId(),
                loan.getCopy().getCurrentBranch().getCode(),
                loan.getCopy().getCurrentBranch().getName(),
                loan.getIssueDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getFineAmount(),
                isOverdue
        );
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllStudents() {
        return userRepository.findAll().stream()
                .filter(u -> u.getRole() == com.campus.library.domain.enums.UserRole.ROLE_STUDENT)
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "universityId", u.getUniversityId(),
                        "fullName", u.getFullName(),
                        "email", u.getEmail()
                ))
                .collect(Collectors.toList());
    }
}
