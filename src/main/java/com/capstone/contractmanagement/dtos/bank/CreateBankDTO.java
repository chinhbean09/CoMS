package com.capstone.contractmanagement.dtos.bank;

import jakarta.persistence.Column;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBankDTO {
    private String bankName;
    private String backAccountNumber;
}
