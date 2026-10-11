package com.campus.library.service;

import com.campus.library.domain.Branch;
import com.campus.library.domain.BranchCopy;
import com.campus.library.domain.TransferRequest;
import com.campus.library.domain.User;
import com.campus.library.domain.enums.CopyStatus;
import com.campus.library.domain.enums.TransferStatus;
import com.campus.library.dto.transfer.TransferCreateRequest;
import com.campus.library.dto.transfer.TransferResponse;
import com.campus.library.exception.BadRequestException;
import com.campus.library.exception.ConflictException;
import com.campus.library.exception.ResourceNotFoundException;
import com.campus.library.repository.BranchCopyRepository;
import com.campus.library.repository.BranchRepository;
import com.campus.library.repository.TransferRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final BranchCopyRepository branchCopyRepository;
    private final BranchRepository branchRepository;
    private final TransferRequestRepository transferRequestRepository;
    private final AuditService auditService;
    private final long holdExpiryHours;

    public TransferService(BranchCopyRepository branchCopyRepository,
                           BranchRepository branchRepository,
                           TransferRequestRepository transferRequestRepository,
                           AuditService auditService,
                           @Value("${app.hold.expiry-hours:48}") long holdExpiryHours) {
        this.branchCopyRepository = branchCopyRepository;
        this.branchRepository = branchRepository;
        this.transferRequestRepository = transferRequestRepository;
        this.auditService = auditService;
        this.holdExpiryHours = holdExpiryHours;
    }

    /**
     * NFR-1 Concurrency Guard & Hold Creation:
     * Acquires Spring Data JPA @Lock(PESSIMISTIC_WRITE) on the targeted BranchCopy.
     * Duplicate or concurrent requests result in HTTP 409 Conflict.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransferResponse requestTransfer(TransferCreateRequest request, User student) {
        // 1. Acquire pessimistic write lock (SELECT ... FOR UPDATE)
        BranchCopy copy = branchCopyRepository.findByIdForUpdate(request.copyId())
                .orElseThrow(() -> new ResourceNotFoundException("Book copy not found with ID: " + request.copyId()));

        // 2. Validate current state under exclusive write lock
        if (copy.getStatus() != CopyStatus.AVAILABLE) {
            throw new ConflictException("Book copy [" + copy.getBarcode() + "] is currently " +
                    copy.getStatus() + " and cannot be requested for transfer.");
        }

        // 3. Validate destination branch
        Branch destinationBranch = branchRepository.findById(request.destinationBranchId())
                .orElseThrow(() -> new ResourceNotFoundException("Destination branch not found with ID: " + request.destinationBranchId()));

        if (copy.getCurrentBranch().getId().equals(destinationBranch.getId())) {
            throw new BadRequestException("Requested book copy is already located at " + destinationBranch.getName());
        }

        // 4. Atomic status transition: AVAILABLE -> RESERVED
        copy.setStatus(CopyStatus.RESERVED);
        branchCopyRepository.save(copy);

        // 5. Create transfer pipeline record
        TransferRequest transfer = new TransferRequest(copy, student, copy.getCurrentBranch(), destinationBranch);
        TransferRequest savedTransfer = transferRequestRepository.save(transfer);

        // 6. Append immutable audit trail (NFR-4)
        auditService.recordAction("BranchCopy", copy.getId(), "TRANSFER_RESERVED",
                CopyStatus.AVAILABLE.name(), CopyStatus.RESERVED.name(), student,
                "Hold claimed by student " + student.getUniversityId() + " (" + student.getFullName() + ")");

        auditService.recordAction("TransferRequest", savedTransfer.getId(), "REQUESTED",
                null, TransferStatus.REQUESTED.name(), student,
                "Transfer request initiated from " + copy.getCurrentBranch().getCode() + " to " + destinationBranch.getCode());

        return mapToResponse(savedTransfer);
    }

    /**
     * Source Librarian marks item pulled from shelf and dispatched to transit.
     * State transition: RESERVED -> IN_TRANSIT
     */
    @Transactional
    public TransferResponse dispatchTransfer(Long transferId, User librarian) {
        TransferRequest transfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer request not found with ID: " + transferId));

        if (transfer.getStatus() != TransferStatus.REQUESTED) {
            throw new BadRequestException("Transfer #" + transferId + " cannot be dispatched; current status is " + transfer.getStatus());
        }

        BranchCopy copy = transfer.getCopy();
        if (copy.getStatus() != CopyStatus.RESERVED) {
            throw new BadRequestException("Underlying copy [" + copy.getBarcode() + "] is not in RESERVED status (Current: " + copy.getStatus() + ")");
        }

        // Transition states
        copy.setStatus(CopyStatus.IN_TRANSIT);
        branchCopyRepository.save(copy);

        transfer.setStatus(TransferStatus.DISPATCHED);
        transfer.setDispatchedAt(LocalDateTime.now());
        TransferRequest updatedTransfer = transferRequestRepository.save(transfer);

        // Audit log
        auditService.recordAction("BranchCopy", copy.getId(), "DISPATCHED",
                CopyStatus.RESERVED.name(), CopyStatus.IN_TRANSIT.name(), librarian,
                "Dispatched to campus transit by " + librarian.getFullName());

        auditService.recordAction("TransferRequest", transfer.getId(), "DISPATCHED",
                TransferStatus.REQUESTED.name(), TransferStatus.DISPATCHED.name(), librarian,
                "Parcel in transit to " + transfer.getToBranch().getCode());

        return mapToResponse(updatedTransfer);
    }

    /**
     * Destination Branch acknowledges physical intake.
     * State transition: IN_TRANSIT -> READY_PICKUP
     */
    @Transactional
    public TransferResponse receiveTransfer(Long transferId, User librarian) {
        TransferRequest transfer = transferRequestRepository.findById(transferId)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer request not found with ID: " + transferId));

        if (transfer.getStatus() != TransferStatus.DISPATCHED) {
            throw new BadRequestException("Transfer #" + transferId + " cannot be received; current status is " + transfer.getStatus());
        }

        BranchCopy copy = transfer.getCopy();
        if (copy.getStatus() != CopyStatus.IN_TRANSIT) {
            throw new BadRequestException("Underlying copy [" + copy.getBarcode() + "] is not in IN_TRANSIT status (Current: " + copy.getStatus() + ")");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(holdExpiryHours);

        // Transition states
        copy.setStatus(CopyStatus.READY_PICKUP);
        copy.setCurrentBranch(transfer.getToBranch()); // Update physical presence
        branchCopyRepository.save(copy);

        transfer.setStatus(TransferStatus.READY_FOR_PICKUP);
        transfer.setArrivedAt(now);
        transfer.setExpiresAt(expiresAt);
        TransferRequest updatedTransfer = transferRequestRepository.save(transfer);

        // Audit log
        auditService.recordAction("BranchCopy", copy.getId(), "READY_FOR_PICKUP",
                CopyStatus.IN_TRANSIT.name(), CopyStatus.READY_PICKUP.name(), librarian,
                "Received at " + transfer.getToBranch().getCode() + " hold shelf");

        auditService.recordAction("TransferRequest", transfer.getId(), "READY_FOR_PICKUP",
                TransferStatus.DISPATCHED.name(), TransferStatus.READY_FOR_PICKUP.name(), librarian,
                "Available for student pickup. 48h expiration timer started.");

        return mapToResponse(updatedTransfer);
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getStudentTransfers(Long studentId) {
        return transferRequestRepository.findByStudentIdOrderByCreatedAtDesc(studentId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TransferResponse> getPendingQueue(Long branchId, TransferStatus status) {
        List<TransferRequest> requests;
        if (branchId != null && status != null) {
            requests = transferRequestRepository.findByFromBranchIdAndStatus(branchId, status);
        } else if (status != null) {
            requests = transferRequestRepository.findByStatus(status);
        } else {
            requests = transferRequestRepository.findAll();
        }
        return requests.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TransferResponse getTransferById(Long id) {
        return transferRequestRepository.findById(id)
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Transfer not found with ID: " + id));
    }

    private TransferResponse mapToResponse(TransferRequest transfer) {
        return new TransferResponse(
                transfer.getId(),
                transfer.getCopy().getId(),
                transfer.getCopy().getBarcode(),
                transfer.getCopy().getBook().getTitle(),
                transfer.getCopy().getBook().getIsbn(),
                transfer.getStudent().getId(),
                transfer.getStudent().getFullName(),
                transfer.getStudent().getUniversityId(),
                transfer.getFromBranch().getCode(),
                transfer.getFromBranch().getName(),
                transfer.getToBranch().getCode(),
                transfer.getToBranch().getName(),
                transfer.getStatus(),
                transfer.getCreatedAt(),
                transfer.getDispatchedAt(),
                transfer.getArrivedAt(),
                transfer.getExpiresAt(),
                transfer.getCompletedAt()
        );
    }
}
