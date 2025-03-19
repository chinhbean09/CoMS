package com.capstone.contractmanagement.responses.addendum;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AddendumResponse {
    private Long addendumId;
    private String title;
    private String content;
    private LocalDateTime effectiveDate;
    //private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
