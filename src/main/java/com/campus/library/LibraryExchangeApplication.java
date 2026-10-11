package com.campus.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Centralized Library Inter-Branch Book Exchange System
 * 
 * Provides unified multi-branch inventory discovery, transactional inter-branch
 * transfer pipelines, pessimistic locking concurrency guards, and automated
 * circulation/fine daemons.
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class LibraryExchangeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryExchangeApplication.class, args);
    }
}
