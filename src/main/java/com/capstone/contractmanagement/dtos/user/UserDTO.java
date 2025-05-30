package com.capstone.contractmanagement.dtos.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("full_name")
    private String fullName;

    private String email;

    @NotBlank(message = "Password cannot be blank")
    private String password;

    private String address;

    @JsonProperty("is_ceo")
    private Boolean isCeo;

    @JsonProperty("old_password")
    private String oldPassword;

    @JsonProperty("retype_password")
    private String retypePassword;

    @JsonProperty("role_id")
    private Long roleId;

    private boolean active;

    @JsonProperty("facebook_account_id")
    private String facebookAccountId;

    @JsonProperty("google_account_id")
    private String googleAccountId;

}
