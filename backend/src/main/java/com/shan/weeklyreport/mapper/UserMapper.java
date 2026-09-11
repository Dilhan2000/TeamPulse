package com.shan.weeklyreport.mapper;

import com.shan.weeklyreport.domain.User;
import com.shan.weeklyreport.dto.UserProfileResponse;
import com.shan.weeklyreport.dto.UserSummaryResponse;

/**
 * Plain static mapper for User entities (no MapStruct).
 */
public final class UserMapper {

    private UserMapper() {}

    public static UserProfileResponse toProfileResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus()
        );
    }

    public static UserSummaryResponse toSummaryResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getApprovedAt()
        );
    }
}
