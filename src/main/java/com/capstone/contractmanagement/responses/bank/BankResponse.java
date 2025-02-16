package com.capstone.contractmanagement.responses.bank;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BankResponse {
    private String bankName;
    private String backAccountNumber;
}
