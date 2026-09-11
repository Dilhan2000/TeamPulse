package com.shan.weeklyreport.exception;

import com.shan.weeklyreport.common.AccountStatus;

/**
 * Thrown when an account is not in ACTIVE status during login (403).
 */
public class AccountNotActiveException extends RuntimeException {

    private final AccountStatus status;

    public AccountNotActiveException(AccountStatus status) {
        super(resolveMessage(status));
        this.status = status;
    }

    public AccountStatus getStatus() {
        return status;
    }

    private static String resolveMessage(AccountStatus status) {
        return switch (status) {
            case PENDING_APPROVAL -> "Your account is pending administrator approval.";
            case REJECTED -> "Your registration was rejected. Contact an administrator.";
            case DISABLED -> "Your account has been disabled. Contact an administrator.";
            default -> "Your account is not active.";
        };
    }
}
