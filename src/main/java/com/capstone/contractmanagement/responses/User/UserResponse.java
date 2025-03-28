package com.capstone.contractmanagement.responses.User;

import com.capstone.contractmanagement.entities.Department;
import com.capstone.contractmanagement.entities.Role;
import com.capstone.contractmanagement.entities.User;
import com.capstone.contractmanagement.enums.DepartmentList;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserResponse {
    private Long id;

    @NotBlank(message = "email is required")
    private String email;

    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("address")
    private String address;

    @JsonProperty("is_active")
    private boolean active;

    @JsonProperty("date_of_birth")
    private LocalDateTime dateOfBirth;

    private Department department;

    @JsonProperty("facebook_account_id")
    private String facebookAccountId;

    @JsonProperty("google_account_id")
    private String googleAccountId;

    private String city;

    @JsonProperty("avatar")
    private String avatar;

    @JsonProperty("staff_code")
    private String staffCode;

    @JsonProperty("gender")
    private String gender;

    @JsonProperty("role")
    private Role role;

    private Boolean isCeo;

    public static UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .active(user.isActive())
                .dateOfBirth(user.getDateOfBirth())
                .department(user.getDepartment())
                .staffCode(user.getStaffCode())
                .facebookAccountId(user.getFacebookAccountId())
                .googleAccountId(user.getGoogleAccountId())
                .role(user.getRole())
                .isCeo(user.getIsCeo())
                .avatar(user.getAvatar())
                .build();
    }
}
