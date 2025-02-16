package com.capstone.contractmanagement.responses.party;

import com.capstone.contractmanagement.dtos.bank.CreateBankDTO;
import com.capstone.contractmanagement.enums.PartyType;
import com.capstone.contractmanagement.responses.bank.BankResponse;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CreatePartyResponse {
    private Long partyId;
    private String partnerCode;
    private PartyType partnerType; // Loại bên (Bên A, Bên B)
    private String partnerName; // Tên công ty hoặc cá nhân
    private String spokesmanName;
    private String address;
    private String taxCode;// Mã số thuế
    private String phone;
    private String email;
    private List<BankResponse> banking;
}
