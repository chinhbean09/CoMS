package com.capstone.contractmanagement.dtos.party;

import com.capstone.contractmanagement.dtos.bank.CreateBankDTO;
import com.capstone.contractmanagement.enums.PartnerType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePartnerDTO {
    private PartnerType partnerType; // Loại bên (Bên A, Bên B)
    private String partnerName; // Tên công ty hoặc cá nhân
    private String spokesmanName;
    private String address;
    private String taxCode;// Mã số thuế
    private String phone;
    private String email;
    private String note;
    private String position;
    private String abbreviation;
    private List<CreateBankDTO> banking;
}
