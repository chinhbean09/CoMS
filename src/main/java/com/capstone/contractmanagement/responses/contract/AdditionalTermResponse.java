package com.capstone.contractmanagement.responses.contract;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalTermResponse {
    private Long id;
    private String group; // COMMON, A, B (theo TermGroup)
    private List<AdditionalTermDetailResponse> details;

}
