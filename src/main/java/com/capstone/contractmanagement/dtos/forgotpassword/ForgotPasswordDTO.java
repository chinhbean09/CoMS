package com.capstone.contractmanagement.dtos.forgotpassword;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ForgotPasswordDTO {
    @JsonProperty("email")
    private String email;

    @JsonProperty("otp")
    private Integer otp;
}
