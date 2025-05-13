package com.capstone.contractmanagement.dtos.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class SignResponse {
    private String fileName;
    private String fileBase64;
    private String certificateInfo;
    private LocalDateTime signedAt;
}
