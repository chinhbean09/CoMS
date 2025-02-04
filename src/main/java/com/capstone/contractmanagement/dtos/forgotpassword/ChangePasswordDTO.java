package com.capstone.contractmanagement.dtos.forgotpassword;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ChangePasswordDTO {

    @JsonProperty("new_password")
    private String newPassword;
    @JsonProperty("confirm_password")
    private String confirmPassword;
}
