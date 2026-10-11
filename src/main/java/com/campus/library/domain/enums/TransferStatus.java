package com.campus.library.domain.enums;

/**
 * Status of an inter-branch transfer request:
 * REQUESTED -> DISPATCHED -> READY_FOR_PICKUP -> COMPLETED
 * Can transition to CANCELLED or EXPIRED (after 48 hours).
 */
public enum TransferStatus {
    REQUESTED,
    DISPATCHED,
    READY_FOR_PICKUP,
    COMPLETED,
    CANCELLED,
    EXPIRED
}
