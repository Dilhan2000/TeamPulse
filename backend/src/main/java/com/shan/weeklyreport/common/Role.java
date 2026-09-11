package com.shan.weeklyreport.common;

/**
 * System roles (C1-T03).
 * Must match the CHECK constraint in V2 migration:
 * CHECK (role IN ('TEAM_MEMBER','MANAGER','ADMIN'))
 */
public enum Role {
    TEAM_MEMBER,
    MANAGER,
    ADMIN
}
