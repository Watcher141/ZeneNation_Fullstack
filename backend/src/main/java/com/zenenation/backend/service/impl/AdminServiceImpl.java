package com.zenenation.backend.service.impl;

import com.zenenation.backend.config.CacheConfig;
import com.zenenation.backend.dto.response.AdminDashboardResponse;
import com.zenenation.backend.dto.response.PagedResponse;
import com.zenenation.backend.dto.response.UserProfileResponse;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.enums.OrderStatus;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.OrderRepository;
import com.zenenation.backend.repository.ProductRepository;
import com.zenenation.backend.repository.CategoryRepository;
import com.zenenation.backend.repository.UserRepository;
import com.zenenation.backend.service.AdminService;
import com.zenenation.backend.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SecurityUtil securityUtil;

    // -------------------------------------------------------------------------
    // DASHBOARD
    // -------------------------------------------------------------------------

    @Override
    @Cacheable(value = CacheConfig.CACHE_DASHBOARD)
    public AdminDashboardResponse getDashboardSummary() {
        log.debug("Building admin dashboard summary");

        // Today's date range
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime todayEnd = LocalDate.now().atTime(23, 59, 59);

        // Month date range
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = LocalDateTime.now();

        // Revenue calculations from DELIVERED orders
        BigDecimal totalRevenue = orderRepository.findAll()
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(o -> o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal todayRevenue = orderRepository
                .findByDateRange(todayStart, todayEnd, PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(o -> o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal thisMonthRevenue = orderRepository
                .findByDateRange(monthStart, monthEnd, PageRequest.of(0, Integer.MAX_VALUE))
                .stream()
                .filter(o -> o.getStatus() == OrderStatus.DELIVERED)
                .map(o -> o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Out of stock products
        long outOfStockProducts = productRepository.findAll()
                .stream()
                .filter(p -> !p.getIsDeleted() && p.getStockQuantity() == 0)
                .count();

        // New users today
        long newUsersToday = userRepository.findAll()
                .stream()
                .filter(u -> u.getCreatedAt() != null
                        && u.getCreatedAt().isAfter(todayStart)
                        && u.getCreatedAt().isBefore(todayEnd))
                .count();

        return AdminDashboardResponse.builder()
                // Orders
                .totalOrders(orderRepository.count())
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PENDING))
                .confirmedOrders(orderRepository.countByStatus(OrderStatus.CONFIRMED))
                .processingOrders(orderRepository.countByStatus(OrderStatus.PROCESSING))
                .shippedOrders(orderRepository.countByStatus(OrderStatus.SHIPPED))
                .deliveredOrders(orderRepository.countByStatus(OrderStatus.DELIVERED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                // Revenue
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .thisMonthRevenue(thisMonthRevenue)
                // Inventory
                .totalProducts(productRepository.count())
                .activeProducts(productRepository.findByIsDeletedFalse(
                        PageRequest.of(0, Integer.MAX_VALUE)).getTotalElements())
                .outOfStockProducts(outOfStockProducts)
                .totalCategories(categoryRepository.findByIsDeletedFalse().size())
                // Users
                .totalUsers(userRepository.count())
                .newUsersToday(newUsersToday)
                .build();
    }

    // -------------------------------------------------------------------------
    // USER MANAGEMENT
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserProfileResponse> getAllUsers(int page, int size) {
        Page<User> users = userRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
        return PagedResponse.of(users.map(this::toUserProfileResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        return toUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Prevent admin from deactivating themselves
        User currentAdmin = securityUtil.getCurrentUser();
        if (user.getId().equals(currentAdmin.getId())) {
            throw new BadRequestException("You cannot deactivate your own account");
        }

        user.setIsActive(false);
        user = userRepository.save(user);
        log.info("User deactivated: userId={}", userId);

        return toUserProfileResponse(user);
    }

    @Override
    @Transactional
    public UserProfileResponse activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setIsActive(true);
        user = userRepository.save(user);
        log.info("User activated: userId={}", userId);

        return toUserProfileResponse(user);
    }

    // -------------------------------------------------------------------------
    // MAPPER
    // -------------------------------------------------------------------------

    private UserProfileResponse toUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .role(user.getRole())
                .provider(user.getProvider())
                .profileImageUrl(user.getProfileImageUrl())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
