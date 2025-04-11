package com.capstone.contractmanagement.dtos.addendum;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignAddendumRequest {
    private Long addendumId;
    private String fileName;
    private String fileBase64;
    private String signedAt;
}
