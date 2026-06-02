package com.bank.transaction.domain;

public enum TransferStatus {
    INITIATED,
    AWAITING_APPROVAL,   // paused — high-value workflow holds the saga
    APPROVED,            // approver said yes; saga is allowed to continue
    DEBITED,
    CREDITED,
    COMPLETED,
    COMPENSATING,
    FAILED
}
