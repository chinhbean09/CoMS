package com.capstone.contractmanagement.dtos.party;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyDTO {
    private String partyType;
    private String name;
    private String address;
    private String taxCode;
    private String identityCard;
    private String representative;
    private String contactInfo;

}
