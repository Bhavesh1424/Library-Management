package com.campus.library.dto.loan;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LoanResponse(
        Long id,
        Long copyId,
        String barcode,
        String bookTitle,
        String isbn,
        Long studentId,
        String studentName,
        String universityId,
        String branchCode,
        String branchName,
        LocalDate issueDate,
        LocalDate dueDate,
        LocalDate returnDate,
        BigDecimal fineAmount,
        boolean isOverdue
) {}
