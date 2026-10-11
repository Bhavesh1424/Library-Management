package com.campus.library.controller;

import com.campus.library.domain.User;
import com.campus.library.domain.enums.TransferStatus;
import com.campus.library.dto.transfer.TransferCreateRequest;
import com.campus.library.dto.transfer.TransferResponse;
import com.campus.library.service.AuthService;
import com.campus.library.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transfers")
@Tag(name = "Transfer State Machine", description = "Endpoints for inter-branch transfer reservation, dispatch, and receiving intake")
public class TransferController {

    private final TransferService transferService;
    private final AuthService authService;

    public TransferController(TransferService transferService, AuthService authService) {
        this.transferService = transferService;
        this.authService = authService;
    }

    /**
     * PRD Section 5: Locks copy and raises transfer hold request (ROLE_STUDENT).
     */
    @PostMapping("/request")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "Submit inter-branch transfer request", description = "Pessimistically locks copy and reserves it for student pickup at destination branch")
    public ResponseEntity<TransferResponse> requestTransfer(@Valid @RequestBody TransferCreateRequest request) {
        User student = authService.getCurrentAuthenticatedUser();
        TransferResponse response = transferService.requestTransfer(request, student);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * PRD Section 5: Source librarian marks item shipped (ROLE_LIBRARIAN).
     */
    @PutMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    @Operation(summary = "Dispatch transfer parcel", description = "Source branch librarian marks book pulled from shelf and handed to transit team (IN_TRANSIT)")
    public ResponseEntity<TransferResponse> dispatchTransfer(@PathVariable Long id) {
        User librarian = authService.getCurrentAuthenticatedUser();
        TransferResponse response = transferService.dispatchTransfer(id, librarian);
        return ResponseEntity.ok(response);
    }

    /**
     * PRD Section 5: Destination branch acknowledges physical receipt (ROLE_LIBRARIAN).
     */
    @PutMapping("/{id}/receive")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    @Operation(summary = "Receive incoming transfer parcel", description = "Destination librarian scans arrival, marks READY_PICKUP, and starts 48-hour pickup window")
    public ResponseEntity<TransferResponse> receiveTransfer(@PathVariable Long id) {
        User librarian = authService.getCurrentAuthenticatedUser();
        TransferResponse response = transferService.receiveTransfer(id, librarian);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "Get student transfer requests", description = "Returns active and historical transfer requests for the logged-in student")
    public ResponseEntity<List<TransferResponse>> getMyRequests() {
        User student = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(transferService.getStudentTransfers(student.getId()));
    }

    @GetMapping("/queue")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    @Operation(summary = "Get librarian dispatch & receiving queue", description = "Returns active transfer requests filtered optionally by branch ID or status")
    public ResponseEntity<List<TransferResponse>> getQueue(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) TransferStatus status
    ) {
        return ResponseEntity.ok(transferService.getPendingQueue(branchId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get transfer by ID", description = "Retrieves full details of a specific transfer request")
    public ResponseEntity<TransferResponse> getTransferById(@PathVariable Long id) {
        return ResponseEntity.ok(transferService.getTransferById(id));
    }
}
