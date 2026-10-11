package com.campus.library.domain.enums;

/**
 * State lifecycle of a physical barcoded book copy:
 * AVAILABLE -> RESERVED -> IN_TRANSIT -> READY_PICKUP -> ISSUED
 */
public enum CopyStatus {
    AVAILABLE,
    RESERVED,
    IN_TRANSIT,
    READY_PICKUP,
    ISSUED
}
