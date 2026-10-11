package com.campus.library.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tracks physical loans, borrowing terms, returns, and accrued fines.
 */
@Entity
@Table(
    name = "loans",
    indexes = {
        @Index(name = "idx_loan_student", columnList = "student_id"),
        @Index(name = "idx_loan_copy", columnList = "copy_id"),
        @Index(name = "idx_loan_overdue", columnList = "return_date, due_date")
    }
)
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "copy_id", nullable = false)
    private BranchCopy copy;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @NotNull
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @NotNull
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    @Column(name = "fine_amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal fineAmount = BigDecimal.ZERO;

    public Loan() {
    }

    public Loan(BranchCopy copy, User student, LocalDate issueDate, LocalDate dueDate) {
        this.copy = copy;
        this.student = student;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.fineAmount = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BranchCopy getCopy() {
        return copy;
    }

    public void setCopy(BranchCopy copy) {
        this.copy = copy;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public BigDecimal getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(BigDecimal fineAmount) {
        this.fineAmount = fineAmount;
    }
}
