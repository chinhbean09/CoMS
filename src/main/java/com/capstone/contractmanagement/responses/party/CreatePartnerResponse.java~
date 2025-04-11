package com.capstone.contractmanagement.responses.party;

import com.capstone.contractmanagement.enums.PartnerType;
import com.capstone.contractmanagement.responses.bank.BankResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreatePartnerResponse {
    private Long partyId;
    private String partnerCode;
    private PartnerType partnerType; // Loại bên (Bên A, Bên B)
    private String partnerName; // Tên công ty hoặc cá nhân
    private String spokesmanName;
    private String address;
    private String taxCode;// Mã số thuế
    private String phone;
    private String email;
    private String note;
    private String position;
    private Boolean isDeleted;
    private String abbreviation;
    private List<BankResponse> banking;
}
