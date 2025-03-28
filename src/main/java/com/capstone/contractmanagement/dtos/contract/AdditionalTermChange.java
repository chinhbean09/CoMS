package com.capstone.contractmanagement.dtos.contract;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdditionalTermChange {
    private Long typeTermId;
    private String oldValue; // Chuỗi đại diện cho commonTerms, aTerms, bTerms cũ
    private String newValue; // Chuỗi đại diện cho commonTerms, aTerms, bTerms mới
    private String action; // "CREATE", "UPDATE", "DELETE"
}
