package com.capstone.contractmanagement.responses.contract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder


public class AdditionalTermDetailResponse {
    private Long id;
    private String content;
    private Long termId;  // Nếu cần tham chiếu đến Term gốc

}
