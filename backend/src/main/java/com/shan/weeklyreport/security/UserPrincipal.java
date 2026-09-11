package com.shan.weeklyreport.security;

import com.shan.weeklyreport.common.Role;

import java.security.Principal;

/**
 * Authenticated user principal stored in SecurityContext.
 */
public record UserPrincipal(
        Long id,
        String email,
        Role role
) implements Principal {

    @Override
    public String getName() {
        return email;
    }
}
