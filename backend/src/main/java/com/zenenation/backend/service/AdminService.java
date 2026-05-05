package com.zenenation.backend.service;

import com.zenenation.backend.dto.response.AdminDashboardResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.UserProfileResponse;

public interface AdminService {

    /** Get dashboard summary — order counts, revenue, inventory, user stats */
    AdminDashboardResponse getDashboardSummary();

    /** Get all users — paginated */
    PagedResponse<UserProfileResponse> getAllUsers(int page, int size);

    /** Get a single user by ID */
    UserProfileResponse getUserById(Long userId);

    /** Deactivate a user account (isActive = false) */
    UserProfileResponse deactivateUser(Long userId);

    /** Reactivate a previously deactivated user account */
    UserProfileResponse activateUser(Long userId);
}
