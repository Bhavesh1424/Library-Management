package com.campus.library.controller;

import com.campus.library.domain.User;
import com.campus.library.dto.loan.CheckoutRequest;
import com.campus.library.dto.loan.LoanResponse;
import com.campus.library.dto.loan.ReturnRequest;
import com.campus.library.service.AuthService;
import com.campus.library.service.CirculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/loans")
@Tag(name = "Circulation & Loans", description = "Endpoints for barcode checkout, book returns, and fine inspection")
public class LoanController {

    private final CirculationService circulationService;
    private final AuthService authService;

    public LoanController(CirculationService circulationService, AuthService authService) {
        this.circulationService = circulationService;
        this.authService = authService;
    }

    /**
     * PRD Section 5: Assigns physical barcode copy to student ID (ROLE_LIBRARIAN).
     */
    @PostMapping("/checkout")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    @Operation(summary = "Checkout book copy", description = "Branch librarian issues a book to a student by scanning its physical barcode")
    public ResponseEntity<LoanResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        User librarian = authService.getCurrentAuthenticatedUser();
        LoanResponse response = circulationService.checkoutCopy(request, librarian);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    /**
     * Returns an issued book and calculates any overdue fines.
     */
    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    @Operation(summary = "Return borrowed book", description = "Branch librarian scans returned book, restores stock to AVAILABLE, and assesses any overdue fine")
    public ResponseEntity<LoanResponse> returnCopy(
            @PathVariable Long id,
            @RequestBody(required = false) ReturnRequest request
    ) {
        User librarian = authService.getCurrentAuthenticatedUser();
        Long returnBranchId = (request != null) ? request.returnBranchId() : null;
        LoanResponse response = circulationService.returnCopy(id, returnBranchId, librarian);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-loans")
    @PreAuthorize("hasAuthority('ROLE_STUDENT')")
    @Operation(summary = "Get student active loans", description = "Returns active loans, due dates, and fine balances for the logged-in student")
    public ResponseEntity<List<LoanResponse>> getMyLoans() {
        User student = authService.getCurrentAuthenticatedUser();
        return ResponseEntity.ok(circulationService.getStudentLoans(student.getId()));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    @Operation(summary = "Get all active campus loans", description = "Librarian view of currently unreturned loans")
    public ResponseEntity<List<LoanResponse>> getActiveLoans() {
        return ResponseEntity.ok(circulationService.getActiveLoans());
    }

    @GetMapping("/students")
    @PreAuthorize("hasAnyAuthority('ROLE_LIBRARIAN', 'ROLE_ADMIN')")
    @Operation(summary = "Get registered students", description = "Librarian view of registered student patrons for checkout")
    public ResponseEntity<List<Map<String, Object>>> getStudents() {
        return ResponseEntity.ok(circulationService.getAllStudents());
    }
}
