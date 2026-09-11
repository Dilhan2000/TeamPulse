package com.shan.weeklyreport.service;

import com.shan.weeklyreport.common.AccountStatus;
import com.shan.weeklyreport.common.Role;
import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.ApproveUserRequest;
import com.shan.weeklyreport.dto.UpdateUserRoleRequest;
import com.shan.weeklyreport.dto.UpdateUserStatusRequest;
import com.shan.weeklyreport.dto.UserSummaryResponse;
import com.shan.weeklyreport.exception.BadRequestException;
import com.shan.weeklyreport.exception.ResourceNotFoundException;
import com.shan.weeklyreport.mapper.UserMapper;
import com.shan.weeklyreport.repository.UserRepository;
import com.shan.weeklyreport.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for administrative user operations (C1-T16, C1-T17).
 */
@Service
public class AdminUserService {

    private final UserRepository userRepository;

    public AdminUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Retrieves paginated list of pending user registrations (C1-T16).
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getPendingUsers(Pageable pageable) {
        return userRepository.findByStatus(AccountStatus.PENDING_APPROVAL, pageable)
                .map(UserMapper::toSummaryResponse);
    }

    /**
     * Approves a pending user with an optional role override (C1-T16).
     */
    @Transactional
    public UserSummaryResponse approveUser(Long id, ApproveUserRequest request, UserPrincipal adminPrincipal) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new BadRequestException("User is not in PENDING_APPROVAL status.");
        }

        User admin = userRepository.findById(adminPrincipal.id())
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found"));

        if (request != null && request.role() != null) {
            if (request.role() == Role.ADMIN) {
                throw new BadRequestException("Cannot assign ADMIN role.");
            }
            user.setRole(request.role());
        }

        user.setStatus(AccountStatus.ACTIVE);
        user.setApprovedBy(admin);
        user.setApprovedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        return UserMapper.toSummaryResponse(saved);
    }

    /**
     * Rejects a pending user (C1-T16).
     */
    @Transactional
    public UserSummaryResponse rejectUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new BadRequestException("User is not in PENDING_APPROVAL status.");
        }

        user.setStatus(AccountStatus.REJECTED);
        User saved = userRepository.save(user);
        return UserMapper.toSummaryResponse(saved);
    }

    /**
     * Lists users with optional status, role, and search query filters (C1-T17).
     */
    @Transactional(readOnly = true)
    public Page<UserSummaryResponse> getUsers(
            AccountStatus status,
            Role role,
            String search,
            Pageable pageable) {

        Specification<User> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("fullName")), pattern);
                Predicate emailLike = cb.like(cb.lower(root.get("email")), pattern);
                predicates.add(cb.or(nameLike, emailLike));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return userRepository.findAll(spec, pageable).map(UserMapper::toSummaryResponse);
    }

    /**
     * Updates status of a user (C1-T17).
     * Only ACTIVE and DISABLED are permitted. Cannot disable an ADMIN or own account.
     */
    @Transactional
    public UserSummaryResponse updateUserStatus(Long id, UpdateUserStatusRequest request, UserPrincipal adminPrincipal) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getId().equals(adminPrincipal.id())) {
            throw new BadRequestException("Cannot modify status of your own account.");
        }
        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot modify the status of an ADMIN account.");
        }
        if (request.status() != AccountStatus.ACTIVE && request.status() != AccountStatus.DISABLED) {
            throw new BadRequestException("Only ACTIVE or DISABLED statuses can be set via this endpoint.");
        }

        user.setStatus(request.status());
        User saved = userRepository.save(user);
        return UserMapper.toSummaryResponse(saved);
    }

    /**
     * Updates role of a user (C1-T17).
     * Only TEAM_MEMBER and MANAGER are permitted. Cannot change role of an ADMIN or set role to ADMIN.
     */
    @Transactional
    public UserSummaryResponse updateUserRole(Long id, UpdateUserRoleRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (user.getRole() == Role.ADMIN) {
            throw new BadRequestException("Cannot modify the role of an ADMIN account.");
        }
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("Cannot promote an account to ADMIN.");
        }

        user.setRole(request.role());
        User saved = userRepository.save(user);
        return UserMapper.toSummaryResponse(saved);
    }
}
