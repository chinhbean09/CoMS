package com.capstone.contractmanagement.dtos.user;

import com.capstone.contractmanagement.enums.DepartmentList;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CreateUserDTO {
    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("full_name")
    private String fullName;

    private String email;

    private String address;

    @JsonProperty("is_ceo")
    private Boolean isCeo;

    @JsonProperty("date_of_birth")
    private LocalDateTime dateOfBirth;

    private DepartmentList department;

    @JsonProperty("role_id")
    private Long roleId;
}
