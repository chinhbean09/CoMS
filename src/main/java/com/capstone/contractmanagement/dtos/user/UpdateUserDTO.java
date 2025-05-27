package com.capstone.contractmanagement.dtos.user;

import com.capstone.contractmanagement.entities.Department;
import com.capstone.contractmanagement.enums.DepartmentList;
import com.capstone.contractmanagement.enums.GenderList;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UpdateUserDTO {
    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("full_name")
    private String fullName;

    private String email;

    private String address;

    @JsonProperty("is_ceo")
    private Boolean isCeo;

    private LocalDateTime dateOfBirth;
    private Long departmentId;

    private GenderList gender;

    @JsonProperty("role_id")
    private Long roleId;

}
