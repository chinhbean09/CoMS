package com.capstone.contractmanagement.responses.User;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserContractResponse {
    @JsonProperty("full_name")
    private String fullName;

    @JsonProperty("user_id")
    private Long userId;
}
