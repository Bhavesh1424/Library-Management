package com.campus.library.domain;

import com.campus.library.domain.enums.CopyStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Physical barcoded unit of a Book assigned to a Branch.
 * Subject to pessimistic write locking during transfer reservation.
 */
@Entity
@Table(
    name = "branch_copies",
    indexes = {
        @Index(name = "idx_copy_branch_status", columnList = "branch_id, status"),
        @Index(name = "idx_copy_barcode", columnList = "barcode", unique = true)
    }
)
public class BranchCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Book reference cannot be null")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @NotNull(message = "Current branch reference cannot be null")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch currentBranch;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "home_branch_id")
    private Branch homeBranch;

    @NotBlank(message = "Barcode cannot be blank")
    @Column(nullable = false, unique = true, length = 64)
    private String barcode;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Column(name = "shelf_location", length = 100)
    private String shelfLocation;

    @Version
    private Long version;

    public BranchCopy() {
    }

    public BranchCopy(Book book, Branch currentBranch, String barcode, String shelfLocation) {
        this.book = book;
        this.currentBranch = currentBranch;
        this.homeBranch = currentBranch;
        this.barcode = barcode;
        this.shelfLocation = shelfLocation;
        this.status = CopyStatus.AVAILABLE;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public Branch getCurrentBranch() {
        return currentBranch;
    }

    public void setCurrentBranch(Branch currentBranch) {
        this.currentBranch = currentBranch;
    }

    public Branch getHomeBranch() {
        return homeBranch;
    }

    public void setHomeBranch(Branch homeBranch) {
        this.homeBranch = homeBranch;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public CopyStatus getStatus() {
        return status;
    }

    public void setStatus(CopyStatus status) {
        this.status = status;
    }

    public String getShelfLocation() {
        return shelfLocation;
    }

    public void setShelfLocation(String shelfLocation) {
        this.shelfLocation = shelfLocation;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BranchCopy that = (BranchCopy) o;
        return Objects.equals(barcode, that.barcode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(barcode);
    }
}
