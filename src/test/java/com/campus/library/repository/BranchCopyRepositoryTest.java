package com.campus.library.repository;

import com.campus.library.domain.Book;
import com.campus.library.domain.Branch;
import com.campus.library.domain.BranchCopy;
import com.campus.library.domain.enums.CopyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
class BranchCopyRepositoryTest {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BranchCopyRepository branchCopyRepository;

    @Test
    @DisplayName("Verify seed data: CLRS has 0 copies at CENTRAL and 2 copies at CS-DEPT")
    void testInventoryDistribution() {
        Optional<Book> clrsOpt = bookRepository.findByIsbn("9780262033848");
        assertTrue(clrsOpt.isPresent(), "CLRS book must exist in seeded catalog");
        Book clrs = clrsOpt.get();

        Branch central = branchRepository.findByCode("CENTRAL").orElseThrow();
        Branch cs = branchRepository.findByCode("CS-DEPT").orElseThrow();

        long centralCount = branchCopyRepository.countByBookIdAndCurrentBranchIdAndStatus(clrs.getId(), central.getId(), CopyStatus.AVAILABLE);
        long csCount = branchCopyRepository.countByBookIdAndCurrentBranchIdAndStatus(clrs.getId(), cs.getId(), CopyStatus.AVAILABLE);

        assertEquals(0, centralCount, "Central library should have 0 available copies of CLRS");
        assertEquals(2, csCount, "CS library should have 2 available copies of CLRS");
    }

    @Test
    @DisplayName("Verify Pessimistic Lock query executes without syntax or mapping error")
    void testPessimisticLockQuery() {
        BranchCopy copy = branchCopyRepository.findByBarcode("BC-CS-101").orElseThrow();
        Optional<BranchCopy> lockedCopy = branchCopyRepository.findByIdForUpdate(copy.getId());

        assertTrue(lockedCopy.isPresent());
        assertEquals("BC-CS-101", lockedCopy.get().getBarcode());
        assertEquals(CopyStatus.AVAILABLE, lockedCopy.get().getStatus());
    }
}
