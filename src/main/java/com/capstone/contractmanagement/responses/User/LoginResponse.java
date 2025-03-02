package com.capstone.contractmanagement.responses.User;

import com.capstone.contractmanagement.entities.AppConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponse {
    @JsonProperty("message")
    private String message;

    @JsonProperty("token")
    private String token;

    @JsonProperty("refresh_token")
    private String refreshToken;

    private String tokenType = "Bearer";
    //user's detail
    private Long id;

    private String fullName;

    private List<String> roles;

    private String avatar;

    private String phoneNumber;

    private String email;

    private List<AppConfig> configs;

}
