package com.capstone.contractmanagement.dtos.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdatePasswordDTO {
    @JsonProperty("old_password")
    private String oldPassword;
    @JsonProperty("new_password")
    private String newPassword;
    @JsonProperty("confirm_password")
    private String confirmPassword;
}
