package com.campus.library.scheduler;

import com.campus.library.domain.Loan;
import com.campus.library.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * PRD Section 4.1 Overdue Calculations:
 * Scheduled daemon executes at midnight (0 0 0 * * ?) accruing fines via formula:
 * Fine Amount = Days Elapsed Past Due * ₹5.00 per non-returned book.
 */
@Component
public class OverdueFineScheduler {

    private static final Logger log = LoggerFactory.getLogger(OverdueFineScheduler.class);

    private final LoanRepository loanRepository;
    private final double dailyFineRate;

    public OverdueFineScheduler(LoanRepository loanRepository,
                                @Value("${app.loan.daily-fine-rate:5.00}") double dailyFineRate) {
        this.loanRepository = loanRepository;
        this.dailyFineRate = dailyFineRate;
    }

    /**
     * Executes at midnight (00:00:00 AM) every day.
     */
    @Scheduled(cron = "${app.loan.fine-cron:0 0 0 * * ?}")
    @Transactional
    public void accrueDailyOverdueFines() {
        calculateFinesForDate(LocalDate.now());
    }

    @Transactional
    public int calculateFinesForDate(LocalDate currentDate) {
        List<Loan> overdueLoans = loanRepository.findActiveOverdueLoans(currentDate);

        if (overdueLoans.isEmpty()) {
            return 0;
        }

        log.info("Overdue Fine Daemon: Accruing fines for {} non-returned books past due...", overdueLoans.size());

        for (Loan loan : overdueLoans) {
            long daysPastDue = ChronoUnit.DAYS.between(loan.getDueDate(), currentDate);
            if (daysPastDue > 0) {
                BigDecimal fine = BigDecimal.valueOf(daysPastDue * dailyFineRate);
                loan.setFineAmount(fine);
                loanRepository.save(loan);
                log.debug("Loan #{} (Student: {}): {} days overdue -> Fine: ₹{}",
                        loan.getId(), loan.getStudent().getUniversityId(), daysPastDue, fine);
            }
        }
        return overdueLoans.size();
    }
}
