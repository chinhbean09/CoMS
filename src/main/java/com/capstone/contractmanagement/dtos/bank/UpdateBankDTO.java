package com.capstone.contractmanagement.dtos.bank;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBankDTO {
    private String bankName;
    private String backAccountNumber;
}
