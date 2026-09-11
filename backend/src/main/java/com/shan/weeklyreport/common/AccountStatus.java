package com.shan.weeklyreport.common;

/**
 * User account status (C1-T03).
 * Must match the CHECK constraint in V2 migration:
 * CHECK (status IN ('PENDING_APPROVAL','ACTIVE','REJECTED','DISABLED'))
 */
public enum AccountStatus {
    PENDING_APPROVAL,
    ACTIVE,
    REJECTED,
    DISABLED
}
