package com.capstone.contractmanagement.responses.party;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreatedByResponse {
    private Long userId;
    private String username;
}
