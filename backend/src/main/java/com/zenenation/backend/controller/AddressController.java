package com.zenenation.backend.controller;

import com.zenenation.backend.dto.request.AddressRequest;
import com.zenenation.backend.dto.response.AddressResponse;
import com.zenenation.backend.dto.response.ApiResponse;
import com.zenenation.backend.entity.Address;
import com.zenenation.backend.entity.User;
import com.zenenation.backend.exception.BadRequestException;
import com.zenenation.backend.exception.ResourceNotFoundException;
import com.zenenation.backend.repository.AddressRepository;
import com.zenenation.backend.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/address")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Address", description = "Manage delivery addresses")
public class AddressController {

    private final AddressRepository addressRepository;
    private final SecurityUtil securityUtil;

    private static final int MAX_ADDRESSES = 5;

    /**
     * GET /api/v1/address
     * Get all saved addresses for current user.
     */
    @GetMapping
    @Operation(summary = "Get all addresses")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAddresses() {
        User user = securityUtil.getCurrentUser();
        List<AddressResponse> addresses = addressRepository.findByUserId(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched", addresses));
    }

    /**
     * GET /api/v1/address/{id}
     * Get a single address by ID.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get address by ID")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddress(@PathVariable Long id) {
        Address address = getAddressBelongingToCurrentUser(id);
        return ResponseEntity.ok(ApiResponse.success("Address fetched", toResponse(address)));
    }

    /**
     * POST /api/v1/address
     * Add a new delivery address (max 5 per user).
     */
    @PostMapping
    @Transactional
    @Operation(summary = "Add a new address")
    public ResponseEntity<ApiResponse<AddressResponse>> addAddress(
            @Valid @RequestBody AddressRequest request) {

        User user = securityUtil.getCurrentUser();

        if (addressRepository.countByUserId(user.getId()) >= MAX_ADDRESSES) {
            throw new BadRequestException(
                    "Maximum " + MAX_ADDRESSES + " addresses allowed per account"
            );
        }

        // If this is set as default, unset all others first
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.unsetDefaultForUser(user.getId());
        }

        // If it's the first address, auto-set as default
        boolean isFirst = addressRepository.countByUserId(user.getId()) == 0;

        Address address = Address.builder()
                .user(user)
                .name(request.getName())
                .phoneNumber(request.getPhoneNumber())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .isDefault(isFirst || Boolean.TRUE.equals(request.getIsDefault()))
                .build();

        address = addressRepository.save(address);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added successfully", toResponse(address)));
    }

    /**
     * PUT /api/v1/address/{id}
     * Update an existing address.
     */
    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update an address")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody AddressRequest request) {

        User user = securityUtil.getCurrentUser();
        Address address = getAddressBelongingToCurrentUser(id);

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            addressRepository.unsetDefaultForUser(user.getId());
        }

        address.setName(request.getName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        address.setCountry(request.getCountry() != null ? request.getCountry() : "India");
        address.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));

        address = addressRepository.save(address);
        return ResponseEntity.ok(ApiResponse.success("Address updated", toResponse(address)));
    }

    /**
     * PATCH /api/v1/address/{id}/default
     * Set an address as the default delivery address.
     */
    @PatchMapping("/{id}/default")
    @Transactional
    @Operation(summary = "Set address as default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(@PathVariable Long id) {
        User user = securityUtil.getCurrentUser();
        Address address = getAddressBelongingToCurrentUser(id);

        addressRepository.unsetDefaultForUser(user.getId());
        address.setIsDefault(true);
        address = addressRepository.save(address);

        return ResponseEntity.ok(ApiResponse.success("Default address updated", toResponse(address)));
    }

    /**
     * DELETE /api/v1/address/{id}
     * Delete an address.
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id) {
        Address address = getAddressBelongingToCurrentUser(id);
        addressRepository.delete(address);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Address getAddressBelongingToCurrentUser(Long addressId) {
        User user = securityUtil.getCurrentUser();
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "id", addressId));
        if (!address.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Address", "id", addressId);
        }
        return address;
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .name(address.getName())
                .phoneNumber(address.getPhoneNumber())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .country(address.getCountry())
                .isDefault(address.getIsDefault())
                .build();
    }
}
