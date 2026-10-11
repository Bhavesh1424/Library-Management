package com.campus.library.repository;

import com.campus.library.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByStudentIdOrderByIssueDateDesc(Long studentId);

    List<Loan> findByStudentIdAndReturnDateIsNull(Long studentId);

    Optional<Loan> findByCopyIdAndReturnDateIsNull(Long copyId);

    /**
     * Query for midnight fine accrual daemon:
     * Non-returned loans past their due date.
     */
    @Query("SELECT l FROM Loan l WHERE l.returnDate IS NULL AND l.dueDate < :currentDate")
    List<Loan> findActiveOverdueLoans(@Param("currentDate") LocalDate currentDate);
}
