package com.campus.library.service;

import com.campus.library.domain.Branch;
import com.campus.library.domain.BranchCopy;
import com.campus.library.domain.User;
import com.campus.library.domain.enums.CopyStatus;
import com.campus.library.dto.transfer.TransferCreateRequest;
import com.campus.library.exception.ConflictException;
import com.campus.library.repository.BranchCopyRepository;
import com.campus.library.repository.BranchRepository;
import com.campus.library.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("dev")
class TransferConcurrencyTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private BranchCopyRepository branchCopyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("NFR-1 Concurrency Guard: 10 concurrent requests on 1 copy serialize correctly (1 Success, 9 HTTP 409 Conflicts)")
    void testConcurrentTransferRequests() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(threadCount);

        BranchCopy copy = branchCopyRepository.findByBarcode("BC-CS-102").orElseThrow();
        Branch destination = branchRepository.findByCode("CENTRAL").orElseThrow();
        User student = userRepository.findByEmail("student@campus.edu").orElseThrow();

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    startGate.await(); // Synchronize all 10 threads to blast request at identical millisecond
                    transferService.requestTransfer(new TransferCreateRequest(copy.getId(), destination.getId()), student);
                    successCount.incrementAndGet();
                } catch (ConflictException e) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // Any unexpected exception
                    e.printStackTrace();
                } finally {
                    endGate.countDown();
                }
            });
        }

        // Release the gate
        startGate.countDown();
        boolean completed = endGate.await(10, TimeUnit.SECONDS);
        executorService.shutdown();

        assertEquals(true, completed, "All concurrent threads should complete within timeout");
        assertEquals(1, successCount.get(), "Exactly one student transfer request must succeed");
        assertEquals(threadCount - 1, conflictCount.get(), "All other concurrent requests must receive 409 Conflict");

        // Verify final state of the copy
        BranchCopy finalCopy = branchCopyRepository.findById(copy.getId()).orElseThrow();
        assertEquals(CopyStatus.RESERVED, finalCopy.getStatus(), "Book copy must end up in RESERVED status");
    }
}
