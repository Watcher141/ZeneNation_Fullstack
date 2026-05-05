package com.zenenation.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AddressRequest {

    @NotBlank(message = "Recipient name is required")
    private String name;

    @NotBlank(message = "Phone number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Please provide a valid Indian mobile number")
    private String phoneNumber;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[1-9][0-9]{5}$", message = "Please provide a valid 6-digit pincode")
    private String pincode;

    private String country = "India";

    private Boolean isDefault = false;
}
